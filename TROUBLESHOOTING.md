# TROUBLESHOOTING.md

# Troubleshooting: edc-controlplane

---

## Startup Issues

### Pod fails to start — CrashLoopBackOff

**Check logs first**:
```bash
kubectl logs deployment/edc-controlplane -n <namespace> --previous
```

---

#### Vault connection refused at startup

**Symptom**: Log contains `Failed to connect to Vault` or the health check for `vault-hashicorp` returns unhealthy.

**Cause**: The Vault URL is unreachable from the pod or the token is invalid.

**Resolution**:
1. Verify the Vault URL is reachable from inside the pod:
   ```bash
   kubectl exec -it <controlplane-pod> -n <namespace> -- curl http://controlplane-vault:8200/v1/sys/health
   ```
2. Verify the Vault token secret exists:
   ```bash
   kubectl get secret edc-controlplane-vault -n <namespace>
   ```
3. Confirm the token has the required KV read/write capabilities in Vault.

---

#### PostgreSQL connection error at startup

**Symptom**: Log contains `Unable to open JDBC Connection` or `datasource … could not connect`.

**Cause**: PostgreSQL is not ready, credentials are incorrect, or the JDBC URL is wrong.

**Resolution**:
1. Check PostgreSQL pod status:
   ```bash
   kubectl get pods -n <namespace> | grep postgresql
   ```
2. Test TCP connectivity from the control plane pod:
   ```bash
   kubectl exec -it <controlplane-pod> -n <namespace> -- \
     nc -zv controlplane-postgresql 5432 && echo "reachable"
   ```
3. Verify datasource environment variables match `postgresql.auth` in `values.yaml`.
4. If `sqlSchemaAutocreate: true`, the DB user must have `CREATE TABLE` privileges on first boot.

---

#### `WARNING: The list of trusted issuers is empty`

**Symptom**: Startup log shows `The list of trusted issuers is empty`.

**Effect**: The DCP layer will reject all incoming VC presentations. Catalog requests from counterparts will fail policy evaluation.

**Resolution**: Add at least one trusted issuer in `values.yaml`:
```yaml
config:
  iam:
    trustedissuer:
      issuer1:
        id: "did:web:your-issuer.example.com"
        supportedtypes:
          - "VerifiableCredential"
```

---

#### `WARNING: TLS and hostname verification are DISABLED`

**Symptom**: Startup log shows `WARNING: TLS and hostname verification are DISABLED`.

**Cause**: `edc.http.disable.tls=true` is set.

**Resolution**: This is intentional for non-production clusters with self-signed certificates. Remove the flag before any production deployment. See `SECURITY.md`.

---

## Policy Evaluation Failures

Policy failures are logged at `DEBUG` level. Enable debug logging in `values.yaml`:
```yaml
log:
  level: "debug"
```

Then filter the logs:
```bash
kubectl logs deployment/edc-controlplane -n <namespace> \
  | grep -E "ClaimEval|PolicyEval|Constraint|CredentialClaim"
```

---

### Catalog request returns empty — access policy blocking all assets

**Symptom**: Consumer receives an empty catalog even though assets exist.

**Diagnosis**:
1. Check for `ClaimEval[membershipStatus][catalog]` log entries.
2. If absent, the policy scope is not triggering evaluation — the scope extension may not be loaded (check for `Scope Extension initialized` in logs).
3. If present, look at the result.

**Common causes**:

| Log message | Cause | Fix |
|---|---|---|
| `ParticipantAgent did not contain a 'vc' claim` | Consumer IdentityHub returned no VCs | Verify consumer's IdentityHub has the correct credential and scope matches |
| `rightOperand '…' is not a recognized value` | Policy definition uses wrong value or typo | Check `VALID_MEMBERSHIP_STATUSES` in `PolicyConstants` |
| `No ParticipantAgent found on context` | DCP identity processing failed | Check `edc.iam.sts.oauth.token.url` points to a live IdentityHub |
| `VerifiableCredentials … empty` | VC list is present but empty | Verify `edc.policy.catalog.scope` matches what IdentityHub issues |

---

### Contract negotiation fails — moves to `TERMINATED` immediately

**Symptom**: Negotiation reaches `REQUESTED` then immediately moves to `TERMINATED`.

**Diagnosis**:
```bash
kubectl logs deployment/edc-controlplane -n <namespace> \
  | grep -E "negotiation|ClaimEval\[|Constraint"
```

**Common causes**:

| Cause | Fix |
|---|---|
| Consumer VC missing a required claim (e.g. `organizationType`) | Consumer must present a credential with all required `credentialSubject` claims |
| `rightOperand` value not in `VALID_*` set (e.g. `"Hospital"` instead of `"hospital"`) | Correct the policy definition — values are case-sensitive |
| Prohibition constraint triggered (e.g. `euEeaStatus == "third-country"`) | Adjust the policy prohibition or the consumer's credentials |
| Obligation not fulfilled (`dacApproval` or `permitIssued` not `"true"`) | Consumer VC must include the obligation claim set to `"true"` |

---

### Transfer fails — `TERMINATED` after a valid contract agreement

**Symptom**: Transfer moves to `TERMINATING`/`TERMINATED` despite an existing contract agreement.

**Cause**: Transfer scope re-evaluates the same constraints as negotiation. If the consumer's VC changed since negotiation (e.g. `membershipStatus` changed to `suspended`), the transfer policy will fail.

**Diagnosis**: Same steps as negotiation — look for `ClaimEval[…][transfer]` entries.

---

### `organizationRoles` list claim evaluation returns `false` unexpectedly

**Symptom**: A consumer with `organizationRoles: ["health-data-holder", "health-data-user"]` fails a constraint requiring `health-data-user`.

**Cause**: The claim value in the VC has leading/trailing whitespace, or the policy rightOperand has incorrect casing.

**Resolution**:
1. The evaluator trims whitespace from the rightOperand but not from the claim value. Check the raw VC.
2. Valid role values (exact strings): `health-data-holder`, `trusted-health-data-holder`, `health-data-user`, `health-data-applicant`, `health-data-access-body`, `federator`, `connector-operator`.

---

## Scope Issues

### IdentityHub not returning the expected credentials

**Symptom**: Policy evaluation fails because the claim is missing from the VC list.

**Cause**: The scope string sent to IdentityHub does not include the credential type that contains the needed claim.

**Resolution**:
1. Verify the scope in use at startup:
   ```bash
   kubectl logs deployment/edc-controlplane -n <namespace> | grep "Scope Extension initialized"
   ```
2. Verify `edc.policy.catalog.scope`, `edc.policy.contract.scope`, and `edc.policy.transfer.scope` include all necessary credential type scopes.
3. The scope format is `org.eclipse.edc.vc.type:<CredentialType>:read`.

---

## Build Issues

### Checkstyle build failure

```bash
./gradlew checkstyle
# Open build/reports/checkstyle/lint.html for details
```

---

### Extension `initialize()` never called

**Symptom**: A new extension does not activate; no log from its `initialize()`.

**Cause**: The `META-INF/services/org.eclipse.edc.spi.system.ServiceExtension` file is missing or lists the wrong class name.

**Resolution**:
1. Verify the file exists at `src/main/resources/META-INF/services/org.eclipse.edc.spi.system.ServiceExtension`.
2. Verify the fully-qualified class name is correct.
3. After `./gradlew shadowJar`, confirm the service file is merged into the JAR:
   ```bash
   unzip -p build/libs/controlplane.jar META-INF/services/org.eclipse.edc.spi.system.ServiceExtension
   ```
   All registered extension class names must appear.

---

### Test compilation fails — `cannot find symbol`

**Cause**: A required test dependency is missing in the subproject's `build.gradle.kts`.

**Resolution**: Add the missing dependency:
```kotlin
testImplementation(libs.junit.jupiter.api)
testImplementation(libs.mockito.core)
testImplementation(libs.assertj.core)
testImplementation(libs.edc.junit)
```

---

## Runtime Diagnostics

### List all active environment variables on the running pod

```bash
kubectl exec -it <pod> -n <namespace> -- env | grep EDC_ | sort
```

### Enable debug logging at runtime (without restart)

Patch the deployment temporarily:
```bash
kubectl set env deployment/edc-controlplane LOG_LEVEL=debug -n <namespace>
```

### Verify policy function registration

Check the startup log for:
```
Policy Evaluation Extension initializing..
Policy Evaluation Extension initialized
```

If only `initializing..` appears, an exception occurred during registration — look for `ERROR` or stack traces immediately after that line.

### Verify trusted issuer registration

```bash
kubectl logs deployment/edc-controlplane -n <namespace> | grep -i "trustedissuer\|TrustedIssuer"
```

Expected: one log line per issuer alias confirming registration.

# DEPLOYMENT.md

# Deployment Guide: edc-controlplane

## Prerequisites

- Kubernetes cluster (tested with K3s / standard K8s)
- Helm 3.x
- Docker registry accessible from the cluster
- `kubectl` configured for the target cluster
- HashiCorp Vault (bundled as subchart or external)
- PostgreSQL (bundled as subchart or external)

---

## Build and Push the Image

```bash
# From the edc-controlplane directory
docker build -t <registry>/edc-controlplane:<tag> .
docker push <registry>/edc-controlplane:<tag>
```

For a local registry on port 5432:

```bash
docker build -t localhost:5432/edc-controlplane . && docker push localhost:5432/edc-controlplane
```

---

## Helm Deployment

The Helm chart lives at `helm-edc-connector/components/controlplane`.

### Minimal deployment

```bash
helm upgrade --install edc-controlplane ./helm-edc-connector/components/controlplane \
  --namespace edc-user1 \
  --create-namespace \
  -f my-values.yaml
```

### Key values to configure

Edit `values.yaml` or provide an override file:

```yaml
image:
  repository: <your-registry>
  name: edc-controlplane
  tag: "<your-tag>"

config:
  participantId: "did:web:your-participant.example.com"

  dsp:
    callbackAddress: "https://your-participant.example.com/edc/cp/dsp/api"

  sts:
    oauthTokenUrl: "http://edc-identityhub:8185/edc/ih/sts/api"

  vault:
    url: "http://your-vault:8200"
    token: ""               # leave empty; provide via secret
    tokenSecretName: "edc-controlplane-vault"
    tokenSecretKey: "token"
    createSecret: false     # set false if you manage the secret externally

  iam:
    trustedissuer:
      issuer1:
        id: "did:web:issuer.example.com"
        supportedtypes:
          - "VerifiableCredential"
          - "MembershipCredential"

  scope:
    catalog: "org.eclipse.edc.vc.type:MembershipCredential:read"
    contract: "org.eclipse.edc.vc.type:MembershipCredential:read"
    transfer: "org.eclipse.edc.vc.type:MembershipCredential:read"

postgresql:
  enabled: true
  auth:
    username: edc_user
    password: "<secure-password>"
    database: edc_connector_db
    postgresPassword: "<secure-postgres-password>"
```

### Bundled vs external PostgreSQL

**Bundled** (default): Set `postgresql.enabled: true`. PostgreSQL is deployed as a subchart with a `PersistentVolume` at the path defined by `postgresql.primary.persistence.path`.

**External**: Set `postgresql.enabled: false` and configure the JDBC URL via environment variable:

```yaml
extraVars:
  - name: EDC_DATASOURCE_DEFAULT_URL
    value: "jdbc:postgresql://your-postgres:5432/edc_connector_db"
  - name: EDC_DATASOURCE_DEFAULT_USER
    valueFrom:
      secretKeyRef:
        name: my-db-secret
        key: username
  - name: EDC_DATASOURCE_DEFAULT_PASSWORD
    valueFrom:
      secretKeyRef:
        name: my-db-secret
        key: password
```

### Bundled vs external Vault

**External Vault** (recommended for production): Set `vault.enabled: false` and configure:

```yaml
config:
  vault:
    url: "https://vault.example.com"
    createSecret: false
    tokenSecretName: "my-vault-token-secret"
    tokenSecretKey: "token"
```

Create the secret manually:

```bash
kubectl create secret generic my-vault-token-secret \
  --from-literal=token=<your-vault-token> \
  -n edc-user1
```

---

## Environment Variables Reference

All EDC properties map to environment variables by uppercasing and replacing `.` with `_`, prefixed with `EDC_`.

| Property | Env Var | Required | Example |
|---|---|---|---|
| `edc.participant.id` | `EDC_PARTICIPANT_ID` | Yes | `did:web:participant.example.com` |
| `edc.dsp.callback.address` | `EDC_DSP_CALLBACK_ADDRESS` | Yes | `https://participant.example.com/edc/cp/dsp/api` |
| `edc.iam.sts.oauth.token.url` | `EDC_IAM_STS_OAUTH_TOKEN_URL` | Yes | `http://edc-identityhub:8185/edc/ih/sts/api` |
| `edc.vault.hashicorp.url` | `EDC_VAULT_HASHICORP_URL` | Yes | `http://vault:8200` |
| `edc.vault.hashicorp.token` | `EDC_VAULT_HASHICORP_TOKEN` | Yes | `<vault-token>` |
| `edc.datasource.default.url` | `EDC_DATASOURCE_DEFAULT_URL` | Yes | `jdbc:postgresql://pg:5432/edc_db` |
| `edc.datasource.default.user` | `EDC_DATASOURCE_DEFAULT_USER` | Yes | `edc_user` |
| `edc.datasource.default.password` | `EDC_DATASOURCE_DEFAULT_PASSWORD` | Yes | `<password>` |
| `edc.policy.catalog.scope` | `EDC_POLICY_CATALOG_SCOPE` | No | `org.eclipse.edc.vc.type:MembershipCredential:read` |
| `edc.policy.contract.scope` | `EDC_POLICY_CONTRACT_SCOPE` | No | `org.eclipse.edc.vc.type:MembershipCredential:read` |
| `edc.policy.transfer.scope` | `EDC_POLICY_TRANSFER_SCOPE` | No | `org.eclipse.edc.vc.type:MembershipCredential:read` |
| `edc.http.disable.tls` | `EDC_HTTP_DISABLE_TLS` | No | `false` (never `true` in production) |

### Trusted issuer variables

For each issuer, set:

```
EDC_IAM_TRUSTEDISSUER_<ALIAS>_ID=did:web:issuer.example.com
EDC_IAM_TRUSTEDISSUER_<ALIAS>_SUPPORTEDTYPES=["VerifiableCredential"]
```

`<ALIAS>` is any alphanumeric label (e.g., `ISSUER1`). Multiple aliases are supported.

---

## Ingress

The Helm chart creates an Ingress resource when `ingress.enabled: true`. Update `ingress.hosts` to match your domain:

```yaml
ingress:
  enabled: true
  hosts:
    - host: your-participant.example.com
      paths:
        - path: /edc/cp/base/api
          port: 8181
          pathType: Prefix
        - path: /edc/cp/management/api
          port: 9192
          pathType: Prefix
        - path: /edc/cp/dsp/api
          port: 8092
          pathType: Prefix
        - path: /edc/cp/control/api
          port: 8093
          pathType: Prefix
        - path: /edc/cp/catalog/api
          port: 8194
          pathType: Prefix
  tls:
    - secretName: your-participant-tls
      hosts:
        - your-participant.example.com
```

The `dsp.callbackAddress` in `config` must match the externally reachable URL for the DSP path.

---

## Health Check and Readiness

The Helm chart configures a Kubernetes readiness probe against:

```
GET http://<pod-ip>:8181/edc/cp/base/api/check/health
```

The probe uses `readinessProbe.enabled: true` by default. The control plane is marked ready only when all health checks (including Vault connectivity) pass.

---

## Autoscaling

HPA is disabled by default. To enable:

```yaml
autoscaling:
  enabled: true
  minReplicas: 2
  maxReplicas: 3
  targetCPUUtilizationPercentage: 70
  targetMemoryUtilizationPercentage: 70
```

Note: The EDC control plane uses in-memory state machines backed by PostgreSQL. Multiple replicas are safe as long as all instances connect to the same PostgreSQL database.

---

## SQL Schema Auto-Creation

By default, `config.sqlSchemaAutocreate: true` is set. This causes EDC to run SQL DDL statements on startup to create any missing tables. Set to `false` once the schema is established in production and manage migrations explicitly.

---

## Rollback

```bash
# List release history
helm history edc-controlplane -n edc-user1

# Roll back to previous revision
helm rollback edc-controlplane <revision> -n edc-user1
```

After rollback, verify the pod is healthy:

```bash
kubectl rollout status deployment/edc-controlplane -n edc-user1
kubectl logs deployment/edc-controlplane -n edc-user1 --tail=50
```

---

## Post-Deployment Verification

1. **Health check**:
   ```bash
   curl https://your-participant.example.com/edc/cp/base/api/check/health
   ```
   Expected: `"isSystemHealthy": true`

2. **Management API accessible**:
   ```bash
   curl -H "X-Api-Key: <token>" \
     https://your-participant.example.com/edc/cp/management/api/v3/assets/request \
     -d '{}'
   ```
   Expected: `200 OK` with an empty or populated asset list.

3. **DSP endpoint reachable from counterpart**:
   ```bash
   curl https://your-participant.example.com/edc/cp/dsp/api/.well-known/dsp-version
   ```

4. **Trusted issuers registered** (check logs at startup):
   ```bash
   kubectl logs deployment/edc-controlplane -n edc-user1 | grep TrustedIssuer
   ```
   Expected: log line confirming each issuer alias was registered, or a warning if the list is empty.

---

## Upgrading

1. Update the image tag in `values.yaml`
2. Review the `CHANGELOG.md` for breaking changes or migration steps
3. If upgrading EDC beyond `0.15.1`, check whether `extensions/common/decentralized-claims-issuers-configuration` can be removed (see `activeContext.md`)
4. Re-check dependency pins in `build.gradle.kts` — the new EDC BOM may already include patched versions

```bash
helm upgrade edc-controlplane ./helm-edc-connector/components/controlplane \
  --namespace edc-user1 \
  -f my-values.yaml

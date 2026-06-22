# memory-bank/productContext.md

# Product Context: edc-controlplane

## Why This Project Exists

The EDC Control Plane is the **trust and coordination hub** of the Dataspace4Health (DS4H) connector. In the IDSA Dataspace Protocol (DSP), the control plane is responsible for:

- Advertising available data assets through a catalog
- Negotiating contract agreements with consuming participants
- Orchestrating data transfer lifecycle (start, suspend, terminate)
- Enforcing access and usage policies at every stage

DS4H extends the standard EDC control plane because the default EDC policy engine evaluates simple string or numeric constraints, while DS4H requires **Verifiable Credential (VC)-based authorization**. Participants must prove their identity, membership status, organizational attributes, and data governance roles through W3C Verifiable Credentials presented over the DCP protocol.

## Problems It Solves

| Problem | Solution |
|---|---|
| Standard EDC has no VC claim evaluators for domain-specific attributes | Custom `PolicyEvaluationExtension` registers credential claim functions for 10 constraint types |
| Requests to IdentityHub must specify which credential scopes to request | `ScopeExtension` merges configured default scopes into every outgoing request context |
| EDC 0.15.1 fails to parse trusted issuer config with hyphens in env var names | Local `TrustedIssuerConfigurationExtension` reads config reliably via the Config API |
| Runtime needs HTTPS to external services disabled in dev/test clusters | `HttpClientExtension` provides an insecure OkHttpClient when `edc.http.disable.tls=true` |
| S3 DataAddress properties need validation before transfer initiation | `S3DataAddressValidatorExtension` validates required bucket, region, and credential properties |

## How It Should Work

### Catalog Flow
1. A consumer (or federator) sends a catalog request over DSP.
2. EDC resolves the request policy context and invokes the `ScopeExtension` post-validator.
3. The scope post-validator merges the default catalog scope (`edc.policy.catalog.scope`) into the request.
4. EDC's DCP layer uses the scopes to request the correct credential presentation from the counterpart's IdentityHub.
5. The `PolicyEvaluationExtension` evaluates the catalog access policy: the participant's `membershipStatus` claim must equal `active`.
6. Assets satisfying the access policy appear in the returned catalog.

### Contract Negotiation Flow
1. Consumer initiates a contract negotiation.
2. EDC builds a `ContractNegotiationPolicyContext` and runs the contract policy.
3. `ScopeExtension` merges the contract scope into the request; IdentityHub returns the full credential set.
4. `PolicyEvaluationExtension` evaluates all registered negotiation-scope constraints: `membershipStatus`, `euEeaStatus`, `organizationType`, `organizationOwnershipType`, `organizationRoles`, `dataProtectionRole`, `jurisdictionCountry`, `supportedProtocols`, plus obligations (`dacApproval`, `permitIssued`).
5. If all constraints and obligations pass, a contract agreement is finalized.

### Transfer Flow
1. Consumer triggers a data transfer on an existing agreement.
2. EDC re-evaluates the agreement policy in the `TransferProcessPolicyContext`.
3. Same negotiation-scope constraints are re-checked, ensuring conditions are still met at transfer time.
4. Transfer proceeds to the data plane upon success.

## User Experience Goals

- **Operators**: Configure policy constraints purely through ODRL policy definitions — no code changes needed for new attribute combinations.
- **Developers**: Extend with a new policy constraint by adding one constant to `PolicyConstants` and one `bind*` call in `PolicyEvaluationExtension` — the generic `CredentialClaimEvaluationFunction` handles the evaluation logic.
- **Deployers**: All runtime configuration (trusted issuers, scopes, vault URL, DSP callback) is supplied through Helm `values.yaml` environment variables — no manual file editing.
- **Security teams**: The container runs as a non-root user (`UID 10100`), pinned dependencies fix known CVEs, and the TLS bypass is guarded by an explicit config flag with a startup warning.

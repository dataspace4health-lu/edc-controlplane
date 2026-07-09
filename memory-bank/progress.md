# memory-bank/progress.md

# Progress: edc-controlplane

## What Works

### Core Extensions
- **Policy extension** (`extensions/policy`) — fully implemented and tested
  - Permission constraints: `membershipStatus`, `euEeaStatus`, `organizationType`, `organizationOwnershipType`, `organizationRoles`, `dataProtectionRole`, `jurisdictionCountry`, `supportedProtocols`
  - Prohibition constraints: `euEeaStatus`, `organizationOwnershipType`
  - Duty (obligation) constraints: `dacApproval`, `permitIssued`
  - All constraints registered across catalog, negotiation, and transfer scopes
  - Generic `CredentialClaimEvaluator` handles scalar and list claim values with EQ/NEQ operators

- **Scope extension** (`extensions/scope`) — fully implemented and tested
  - Post-validators registered for `RequestCatalogPolicyContext`, `RequestContractNegotiationPolicyContext`, `RequestTransferProcessPolicyContext`
  - Default scopes configurable via `edc.policy.catalog.scope`, `edc.policy.contract.scope`, `edc.policy.transfer.scope`

- **Trusted issuer configuration** (`extensions/common/decentralized-claims-issuers-configuration`) — fully implemented
  - Reads `edc.iam.trustedissuer.*` config at startup and populates `TrustedIssuerRegistry`
  - Supports multiple issuers with `id`, `supportedtypes`, and `properties`

- **HTTP client extension** (`extensions/common/http/client`) — fully implemented and tested
  - Provides injectable `OkHttpClient`
  - TLS bypass via `edc.http.disable.tls=true`

- **S3 validator extension** (`extensions/s3`) — fully implemented and tested
  - Source validator: requires `bucketName`, `region`, `objectName` or `folderName` or `objectPrefix`
  - Destination validator: requires `bucketName`, `region`, `keyName`

### Build & Packaging
- Gradle multi-module build with custom source set aggregation
- Shadow JAR build produces a single fat JAR with `mergeServiceFiles()`
- Multi-stage Docker build: Gradle builder → Eclipse Temurin JRE runtime
- Non-root container user (`UID 10100`)
- Checkstyle enforced at build time
- JaCoCo coverage reports per subproject with HTML + XML output
- Aggregated test report via `allTests` task

### Security Patches Applied
- `jackson-core` pinned to `2.21.1` (GHSA-72hv-8253-57qq)
- `jackson-databind` pinned to `2.21.4` (CVE-2026-54512, CVE-2026-54513 — polymorphic type validator PTV allow-list bypasses, both HIGH)
- Jetty pinned to `12.1.7` (CVE-2026-1605, CVE-2026-2332)
- GnuPG and OpenSSL upgraded in Dockerfile (CVE-2025-68973, CVE-2026-45447)

### Helm Deployment
- Helm chart at `helm-edc-connector/components/controlplane`
- PostgreSQL bundled as subchart
- HashiCorp Vault bundled as subchart
- Ingress configured for all five ports
- HPA support (CPU/memory thresholds)
- Kubernetes SecurityContext: `runAsNonRoot: true`

## What's Left to Build

- **End-to-end integration tests** — unit tests cover extension logic in isolation; no automated tests exercise the full DCP/DSP flow with real counterparts
- **Remove `decentralized-claims-issuers-configuration` local extension** — blocked on EDC upgrade beyond 0.15.1
- **Wildcard support for `jurisdictionCountry`** — currently accepts any string; a format-validation step for ISO 3166-1 alpha-2 codes could be added
- **`organizationRoles` multi-value matching with NEQ** — prohibition variant for roles is not currently registered (only for `euEeaStatus` and `organizationOwnershipType`)

## Current Status

**Production-ready** for the DS4H dataspace with EDC 0.15.1. All extensions are in use in deployed environments. The main maintenance burden is the EDC version pin.

## Known Issues

| Issue | Severity | Workaround |
|---|---|---|
| Trusted issuer env vars with hyphens not parsed by upstream EDC | Medium | Local `TrustedIssuerConfigurationExtension` resolves this; remove when upgrading EDC |
| TLS bypass (`edc.http.disable.tls=true`) disables ALL certificate validation | High (if misused) | Guarded by a startup `monitor.warning()`. Never set in production. |
| `allTests` Gradle task requires all subproject tests to have run first | Low | Run `./gradlew test allTests` sequentially |

## Evolution of Project Decisions

| Decision | Original Approach | Current Approach | Reason |
|---|---|---|---|
| Trusted issuer config | Upstream `edc-dc-issuer-config` module | Local implementation | EDC 0.15.1 bug with hyphenated env var keys |
| Policy evaluation | One class per constraint | Generic `CredentialClaimEvaluator` + constants | Reduces boilerplate; consistent behavior across all scalar/list claims |
| Source set layout | Standard Gradle subprojects with JARs | Flat source set aggregation | Simplifies Shadow JAR assembly; all classes in one compilation unit |
| HTTP client | EDC default OkHttpClient | Custom `HttpClientExtension` with `@Provider` | Required TLS bypass capability for non-production clusters |

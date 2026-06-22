# memory-bank/techContext.md

# Technical Context: edc-controlplane

## Technologies Used

| Layer | Technology | Version |
|---|---|---|
| Runtime | Eclipse Dataspace Connector (EDC) | 0.15.1 |
| Language | Java | 17 |
| Build | Gradle | 8.8 |
| Fat JAR | Shadow (GradleUp) | 8.3.8 |
| HTTP client | OkHttp (via EDC) | via EDC BOM |
| HTTP framework | Jetty | 12.1.7 (pinned) |
| JSON | Jackson | 2.20.1 (databind/annotations), 2.21.1 (core, pinned) |
| Database | PostgreSQL | 16.2 |
| Secrets | HashiCorp Vault | via `edc-vault-hashicorp` 0.15.1 |
| Container base | Eclipse Temurin JRE | 17-jre-jammy |
| Deployment | Helm | 3.x |
| Testing | JUnit 5 | 5.10.1 |
| Testing | Mockito | 5.8.0 |
| Testing | AssertJ | 3.24.2 |
| Coverage | JaCoCo | 0.8.10 |
| Checkstyle | Checkstyle | 12.0.0 |

## Development Setup

### Prerequisites
- JDK 17
- Docker (for image builds)
- Gradle wrapper included — use `./gradlew`

### Build Commands

```bash
# Compile and run all tests
./gradlew test

# Build the shadow (fat) JAR — output: build/libs/controlplane.jar
./gradlew shadowJar

# Build and push Docker image to local registry
docker build -t localhost:5432/edc-controlplane . && docker push localhost:5432/edc-controlplane

# Run Checkstyle
./gradlew checkstyle

# Generate JaCoCo coverage reports (per subproject)
./gradlew test jacocoTestReport

# Aggregate test report across all subprojects
./gradlew allTests
```

### Running Locally
The runtime requires external services: PostgreSQL, HashiCorp Vault, and IdentityHub. Use the `helm-edc-connector/components/controlplane` chart or `integration-edc` for local Kubernetes deployment.

## Runtime Configuration Reference

All configuration is supplied via environment variables following EDC's `EDC_` prefix convention (dots → underscores, uppercased).

### Participant & Identity
| Property | Env Var | Description |
|---|---|---|
| `edc.participant.id` | `EDC_PARTICIPANT_ID` | DID:WEB identifier of this participant |
| `edc.iam.sts.oauth.token.url` | `EDC_IAM_STS_OAUTH_TOKEN_URL` | IdentityHub STS token endpoint |

### API Ports & Paths
| Port | Default | Path | Description |
|---|---|---|---|
| HTTP | `8181` | `/edc/cp/base/api` | General-purpose / health check |
| Management | `9192` | `/edc/cp/management/api` | Management REST API |
| Control | `8093` | `/edc/cp/control/api` | Control API (used by data plane) |
| Protocol (DSP) | `8092` | `/edc/cp/dsp/api` | Dataspace Protocol endpoint |
| Catalog | `8194` | `/edc/cp/catalog/api` | Catalog API |

### Policy & Scope
| Property | Env Var | Default |
|---|---|---|
| `edc.policy.catalog.scope` | `EDC_POLICY_CATALOG_SCOPE` | `org.eclipse.edc.vc.type:MembershipCredential:read` |
| `edc.policy.contract.scope` | `EDC_POLICY_CONTRACT_SCOPE` | `org.eclipse.edc.vc.type:MembershipCredential:read` |
| `edc.policy.transfer.scope` | `EDC_POLICY_TRANSFER_SCOPE` | `org.eclipse.edc.vc.type:MembershipCredential:read` |

### Trusted Issuers
```
edc.iam.trustedissuer.<alias>.id             = did:web:example.com
edc.iam.trustedissuer.<alias>.supportedtypes = ["VerifiableCredential"]
edc.iam.trustedissuer.<alias>.properties     = {}
```

### HashiCorp Vault
| Property | Description |
|---|---|
| `edc.vault.hashicorp.url` | Vault service URL (e.g. `http://controlplane-vault:8200`) |
| `edc.vault.hashicorp.token` | Vault access token |

### PostgreSQL
| Property | Description |
|---|---|
| `edc.datasource.default.url` | JDBC URL |
| `edc.datasource.default.user` | DB username |
| `edc.datasource.default.password` | DB password |

### HTTP Client
| Property | Default | Description |
|---|---|---|
| `edc.http.disable.tls` | `false` | Set `true` to disable TLS verification — **dev/test only** |

### DSP Callback
| Property | Description |
|---|---|
| `edc.dsp.callback.address` | External URL at which this control plane's DSP endpoint is reachable by counterparts |

## Dependencies

### EDC Modules (0.15.1)
- `org.eclipse.edc:boot` — runtime bootstrap
- `org.eclipse.edc:control-plane-core` — CP state machines
- `org.eclipse.edc:control-plane-api` + `management-api` — REST APIs
- `org.eclipse.edc:dsp` — Dataspace Protocol implementation
- `org.eclipse.edc:decentralized-claims-core` — DCP credential processing
- `org.eclipse.edc:edr-store-core` + `edr-store-receiver` — EDR lifecycle
- `org.eclipse.edc:data-plane-selector-*` — data plane registration and selection
- `org.eclipse.edc:vault-hashicorp` — Vault integration
- `org.eclipse.edc:sql-*` bundles — PostgreSQL persistence for all stores

### Pinned Security Patches
| Dependency | Pinned Version | CVE / Advisory |
|---|---|---|
| `com.fasterxml.jackson.core:jackson-core` | `2.21.1` | GHSA-72hv-8253-57qq |
| `org.eclipse.jetty:*` | `12.1.7` | CVE-2026-1605, CVE-2026-2332 |
| `libssl3` / `openssl` (Docker) | latest apt | CVE-2026-45447 |
| `gnupg` family (Docker) | latest apt | CVE-2025-68973 |

## Tool Usage Patterns

### Adding a new policy constraint (no new class needed)
1. Add claim name constant and IRI constraint key to `PolicyConstants`
2. Add valid values set to `PolicyConstants` (or `Set.of()` for free-form)
3. Call `bindToNegotiation(...)` in `PolicyEvaluationExtension.registerNegotiationScope()`
4. Call `bindToTransfer(...)` in `PolicyEvaluationExtension.registerTransferScope()`
5. Write a unit test in `PolicyEvaluationExtensionTest`

### Adding a new extension module
1. Create directory under `extensions/`
2. Add `build.gradle.kts` with module dependencies
3. Add `src/main/resources/META-INF/services/org.eclipse.edc.spi.system.ServiceExtension`
4. Register the subproject in `settings.gradle.kts`
5. Add `implementation(project(":extensions:your-module"))` to root `build.gradle.kts`

### Checkstyle
Config: `config/checkstyle/checkstyle.xml`. Reports: `build/reports/checkstyle/lint.html`. Violations fail the build.

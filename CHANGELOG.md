# CHANGELOG.md

All notable changes to `edc-controlplane` are documented here.

Format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/). Versions follow [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [Unreleased]

### Security
- Pinned `com.fasterxml.jackson.core:jackson-databind` to `2.21.4` — fixes CVE-2026-54512 (HIGH): `PolymorphicTypeValidator` allow-list bypassed via generic type parameters in the type ID (e.g. `ArrayList<EvilClass>` passes PTV check for `ArrayList`; nested type arguments are never validated). Affects 2.10.0–2.21.3; fixed in 2.18.8, **2.21.4**, 3.1.4.
- Pinned `com.fasterxml.jackson.core:jackson-databind` to `2.21.4` — fixes CVE-2026-54513 (HIGH): `BasicPolymorphicTypeValidator.Builder.allowIfSubTypeIsArray()` allowlists any array type based solely on `clazz.isArray()` without validating the component type, allowing `EvilType[]` to bypass an explicit allow-list. Affects 2.10.0–2.21.3; fixed in 2.18.8, **2.21.4**, 3.1.4.

---

## [0.15.1] — 2026-06-15

### Added
- `extensions/s3` — `S3DataAddressValidatorExtension` registers source and destination `DataAddress` validators for the `AmazonS3` type (`bucketName`, `region`, and object/folder/key properties).
- `extensions/common/http/client` — `HttpClientExtension` provides a configurable `OkHttpClient` via `@Provider`; supports TLS verification bypass via `edc.http.disable.tls=true` for non-production environments.
- Obligation (duty) constraint support in `PolicyEvaluationExtension`: `dacApproval` and `permitIssued` registered for negotiation and transfer scopes.
- Prohibition constraint support: `euEeaStatus` and `organizationOwnershipType` registered as prohibition evaluators for negotiation and transfer scopes.
- `VALID_BOOLEAN_VALUES` constant in `PolicyConstants` for boolean obligation claims.
- `VALID_SUPPORTED_PROTOCOLS` constant; `supportedProtocols` constraint registered for negotiation and transfer scopes.

### Changed
- `CredentialClaimEvaluator` now supports both `EQ` and `NEQ` operators for scalar and list claims.
- `PolicyEvaluationExtension` refactored with scope-specific bind helper methods (`bindToCatalog`, `bindToNegotiation`, `bindToTransfer`, `bindProhibitionToNegotiation`, `bindDutyToNegotiation`, etc.) for clarity.

### Security
- Pinned `com.fasterxml.jackson.core:jackson-core` to `2.21.1` — fixes GHSA-72hv-8253-57qq (Number Length Constraint Bypass in Async Parser).
- Pinned `org.eclipse.jetty:*` to `12.1.7` — fixes CVE-2026-1605 (GzipHandler resource leak) and CVE-2026-2332 (HTTP/1.1 request smuggling).
- Upgraded `libssl3`, `openssl` in Docker runtime image — fixes CVE-2026-45447 (use-after-free in PKCS#7 verification).
- Upgraded `gnupg` family packages in Docker runtime image — fixes CVE-2025-68973.

---

## [0.15.0] — Initial release

### Added
- `extensions/policy` — `PolicyEvaluationExtension` with credential claim evaluation for catalog, negotiation, and transfer scopes.
  - Permission constraints: `membershipStatus`, `euEeaStatus`, `organizationType`, `organizationOwnershipType`, `organizationRoles`, `dataProtectionRole`, `jurisdictionCountry`.
  - `AbstractCredentialEvaluationFunction` — base class for VC extraction from `ParticipantAgent`.
  - `CredentialClaimEvaluator` — generic evaluator for `credentialSubject` claims (scalar + list, EQ operator).
- `extensions/scope` — `ScopeExtension` with `ScopeMappingFunction` post-validators for catalog, contract, and transfer request contexts.
  - Configurable via `edc.policy.catalog.scope`, `edc.policy.contract.scope`, `edc.policy.transfer.scope`.
- `extensions/common/decentralized-claims-issuers-configuration` — `TrustedIssuerConfigurationExtension` reads `edc.iam.trustedissuer.*` at startup and registers issuers in `TrustedIssuerRegistry`.
  - Workaround for EDC 0.15.1 config parsing issue with hyphenated environment variable keys.
- Multi-stage Docker build: Gradle 8.8 / JDK 17 builder → Eclipse Temurin 17-jre-jammy runtime.
- Non-root container user (`appuser`, UID 10100).
- Helm chart at `helm-edc-connector/components/controlplane` with PostgreSQL and Vault subcharts.
- Gradle build with Shadow JAR, JaCoCo coverage, Checkstyle, and aggregated test report.
- EDC 0.15.1 runtime with PostgreSQL persistence for all stores (asset, policy, contract, transfer, EDR).

---

[Unreleased]: https://github.com/nttdata/edc-controlplane/compare/v0.15.1...HEAD
[0.15.1]: https://github.com/nttdata/edc-controlplane/compare/v0.15.0...v0.15.1
[0.15.0]: https://github.com/nttdata/edc-controlplane/releases/tag/v0.15.0

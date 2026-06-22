# memory-bank/activeContext.md

# Active Context: edc-controlplane

## Current Work Focus

The `edc-controlplane` repository is a **stable, production-targeted runtime**. The extensions are implemented and tested. The primary ongoing concerns are:

1. **EDC version upgrade tracking** — the local `TrustedIssuerConfigurationExtension` is a compatibility shim for EDC 0.15.1. When upgrading beyond this version, verify the upstream fix is included and remove the local extension.
2. **Policy constraint additions** — new dataspace attribute requirements are added by extending `PolicyConstants` and registering in `PolicyEvaluationExtension`.
3. **CVE patch cadence** — pinned versions for `jackson-core` (GHSA-72hv-8253-57qq) and Jetty (CVE-2026-1605, CVE-2026-2332) must be re-evaluated on each EDC BOM upgrade.

## Recent Changes

- Added `extensions/s3` module with `S3DataAddressValidatorExtension` for AmazonS3 source/destination validation.
- Added `extensions/common/http/client` with `HttpClientExtension` supporting TLS bypass via `edc.http.disable.tls`.
- Pinned `jackson-core` to `2.21.1` and Jetty to `12.1.7` in `build.gradle.kts` resolution strategy.
- Added GnuPG and OpenSSL upgrades in `Dockerfile` runtime stage.
- Added obligation (duty) constraint support: `dacApproval` and `permitIssued` bound to negotiation and transfer scopes.
- Added prohibition constraint support: `euEeaStatus` and `organizationOwnershipType` bound to negotiation and transfer scopes.

## Next Steps

- [ ] Upgrade to EDC > 0.15.1 and remove `decentralized-claims-issuers-configuration` local extension when upstream fix is confirmed.
- [ ] Add integration tests that exercise the full policy evaluation path end-to-end with a real IdentityHub.
- [ ] Evaluate adding `jurisdictionCountry` NEQ prohibition support if needed by new policy requirements.
- [ ] Review Jetty and jackson-core pins on next EDC BOM upgrade.

## Active Decisions and Considerations

### No credential type filtering in claim evaluation
`CredentialClaimEvaluator` searches all VCs in the presentation for the named claim, regardless of credential type. This is intentional — it avoids tight coupling between policy constraints and specific credential type names, which may vary across dataspaces.

### OR semantics for obligations expressed via single duty with `odrl:or`
The `dacApproval` and `permitIssued` constraints are registered as separate atomic duty evaluators. To express "dacApproval OR permitIssued", use a single duty with an `odrl:or` constraint in the policy definition. Two separate duties in the same policy evaluate as AND.

### `jurisdictionCountry` is free-form
Unlike all other constraints, `jurisdictionCountry` accepts any ISO 3166-1 alpha-2 code and has no fixed `allowedValues` set. Validation of the value format is not performed at policy evaluation time.

### Scope defaults shared across scopes
All three scope properties (`catalog`, `contract`, `transfer`) currently default to the same value (`MembershipCredential:read`). This is intentional — operators may override each independently via configuration.

## Important Patterns and Preferences

- All policy constraints use the `https://w3id.org/edc/v0.0.1/ns/` namespace prefix. Policy definitions must use `@vocab` in their `@context` to trigger JSON-LD expansion.
- `CredentialClaimEvaluator.evaluate()` returns `Result<Boolean>` — callers (permission/prohibition/duty wrappers) interpret the semantics differently.
- Extension initialization order: `TrustedIssuerConfigurationExtension` → `ScopeExtension` → `PolicyEvaluationExtension` (EDC manages ordering via the dependency injection graph).
- The Gradle source set aggregation (`setSrcDirs`) means all extension classes compile into the root project's output. Subproject `build.gradle.kts` files exist for dependency scoping but do not produce separate JARs.

## Learnings and Insights

- EDC 0.15.1 does not reliably parse environment variables containing hyphens when the property key maps to a config subtree. The local `TrustedIssuerConfigurationExtension` was the pragmatic fix; newer EDC versions resolve this.
- The `ScopeMappingFunction` must always return `true` — it is a post-validator, not a policy evaluator. Returning `false` would incorrectly block the request.
- The `@Provider` annotation on `HttpClientExtension.createHttpClient()` is what makes the client injectable by other EDC components. Without it, the custom OkHttpClient would not be used.

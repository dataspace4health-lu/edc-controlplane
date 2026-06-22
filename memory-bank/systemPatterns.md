# memory-bank/systemPatterns.md

# System Patterns: edc-controlplane

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                     edc-controlplane runtime                    │
│  (org.eclipse.edc.boot.system.runtime.BaseRuntime)              │
│                                                                 │
│  ┌──────────────┐  ┌──────────────┐  ┌────────────────────┐    │
│  │   Policy     │  │   Scope      │  │ TrustedIssuer      │    │
│  │  Extension   │  │  Extension   │  │ Configuration      │    │
│  │ (extensions/ │  │ (extensions/ │  │ Extension          │    │
│  │   policy)    │  │   scope)     │  │ (extensions/common/│    │
│  └──────┬───────┘  └──────┬───────┘  │  issuers-config)   │    │
│         │                 │          └────────────────────┘    │
│         ▼                 ▼                                     │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │          EDC PolicyEngine + RuleBindingRegistry          │  │
│  └──────────────────────────────────────────────────────────┘  │
│                                                                 │
│  ┌──────────────┐  ┌──────────────┐                            │
│  │  S3 Validator│  │  HTTP Client │                            │
│  │  Extension   │  │  Extension   │                            │
│  │ (extensions/ │  │ (extensions/ │                            │
│  │    s3)       │  │ common/http) │                            │
│  └──────────────┘  └──────────────┘                            │
└─────────────────────────────────────────────────────────────────┘
        │ DSP                │ Management API         │ Control API
        ▼                    ▼                        ▼
   Counterpart CP      Dashboard / Operator     Data Plane (edc-dataplane)
   (consumer DSP)       (edc-dashboard)
```

## Key Technical Decisions

### 1. EDC Service Extension Pattern
Every custom behavior is packaged as a `ServiceExtension` discovered via `META-INF/services/org.eclipse.edc.spi.system.ServiceExtension`. This is the canonical EDC extension mechanism — no reflection or custom loaders are needed.

### 2. Generic Credential Claim Evaluator
Rather than writing one class per policy constraint, a single `CredentialClaimEvaluator` handles any `credentialSubject` top-level claim. New constraints are added by:
1. Adding a constant pair (claim name + constraint key) to `PolicyConstants`
2. Calling `bindToNegotiation(...)` / `bindToTransfer(...)` in `PolicyEvaluationExtension`

The evaluator supports both scalar (`String`) and list (`List<Object>`) claim values and both `EQ` and `NEQ` operators.

### 3. Permission / Prohibition / Duty Segregation
Three separate function wrapper classes correspond to the three ODRL rule types:
- `CredentialClaimEvaluationFunction` — wraps `CredentialClaimEvaluator` for `Permission`
- `ProhibitionCredentialClaimEvaluationFunction` — wraps for `Prohibition` (inverted semantics)
- `DutyCredentialClaimEvaluationFunction` — wraps for `Duty` (obligation/obligation-OR)

All three delegate to `CredentialClaimEvaluator.evaluate()` and differ only in how they interpret `success(true)` vs `success(false)`.

### 4. Scope Injection via Post-Validators
`ScopeMappingFunction` is registered as a `PolicyValidatorRule<RequestPolicyContext>` post-validator. This hook runs after the request policy context is created but before policy evaluation, giving the extension the opportunity to add default scopes without changing the caller's request payload.

### 5. Flat Source Set Aggregation
`build.gradle.kts` uses a custom `sourceSets` block that walks `extensions/**/src/main` directories and registers them all as source roots of the root project. This allows Gradle to compile all extension classes as one unit while keeping the logical module separation in the filesystem.

### 6. Shadow JAR Deployment
The `com.gradleup.shadow` plugin merges all runtime dependencies and extension service files (`mergeServiceFiles()`) into a single fat JAR. The Dockerfile's build stage produces `/workspace/build/libs/*.jar`, which is copied to `/app/controlplane.jar` in the runtime image.

## Component Relationships

```
PolicyEvaluationExtension
    └─ creates ─▶ CredentialClaimEvaluationFunction<CatalogPolicyContext>
    └─ creates ─▶ CredentialClaimEvaluationFunction<ContractNegotiationPolicyContext>
    └─ creates ─▶ CredentialClaimEvaluationFunction<TransferProcessPolicyContext>
    └─ creates ─▶ ProhibitionCredentialClaimEvaluationFunction (NEG & NEQ variants)
    └─ creates ─▶ DutyCredentialClaimEvaluationFunction (dacApproval, permitIssued)
    └─ injects ─▶ PolicyEngine (registers functions)
    └─ injects ─▶ RuleBindingRegistry (binds constraint IRIs to scopes)

CredentialClaimEvaluationFunction
    └─ delegates ─▶ CredentialClaimEvaluator.evaluate()

CredentialClaimEvaluator extends AbstractCredentialEvaluationFunction
    └─ calls ─▶ getCredentialList(ParticipantAgent)
    └─ reads ─▶ VerifiableCredential.credentialSubject[n].claims[claimName]

ScopeExtension
    └─ creates ─▶ ScopeMappingFunction (one per scope type)
    └─ registers ─▶ PolicyEngine.registerPostValidator(RequestCatalogPolicyContext)
    └─ registers ─▶ PolicyEngine.registerPostValidator(RequestContractNegotiationPolicyContext)
    └─ registers ─▶ PolicyEngine.registerPostValidator(RequestTransferProcessPolicyContext)

TrustedIssuerConfigurationExtension
    └─ reads ─▶ Config("edc.iam.trustedissuer.*")
    └─ registers ─▶ TrustedIssuerRegistry.register(Issuer, credentialType)

HttpClientExtension
    └─ provides ─▶ OkHttpClient (via @Provider)
    └─ reads ─▶ Config("edc.http.disable.tls")
```

## Policy Scope Mapping

| EDC Scope | Extension scope class | What is evaluated |
|---|---|---|
| Catalog access policy | `CatalogPolicyContext` | `membershipStatus` (permission only) |
| Contract negotiation policy | `ContractNegotiationPolicyContext` | All 8 permission + 2 prohibition + 2 duty constraints |
| Transfer process policy | `TransferProcessPolicyContext` | Same as negotiation — re-evaluated on every transfer |
| Catalog request scope | `RequestCatalogPolicyContext` | Scope injection post-validator |
| Contract request scope | `RequestContractNegotiationPolicyContext` | Scope injection post-validator |
| Transfer request scope | `RequestTransferProcessPolicyContext` | Scope injection post-validator |

## Critical Implementation Paths

### Credential lookup path
```
IncomingDSPRequest
  → EDC DCP layer parses presentation token
  → Writes List<VerifiableCredential> into ParticipantAgent.claims["vc"]
  → PolicyEngine invokes registered AtomicConstraintRuleFunction
  → CredentialClaimEvaluator.getCredentialList(agent)
  → Searches all VC credentialSubjects for the named claim
  → Returns success(true/false) or failure
```

### Trusted issuer config path
```
Runtime startup
  → TrustedIssuerConfigurationExtension.initialize()
  → context.getConfig("edc.iam.trustedissuer").partition()
  → For each issuer alias: read id, supportedtypes, properties
  → trustedIssuerRegistry.register(new Issuer(id, props), credentialType)
  → DCP layer checks registry when validating incoming VC issuers
```

### Scope injection path
```
Outgoing catalog/negotiation/transfer request
  → EDC creates RequestPolicyContext
  → PolicyEngine runs post-validators
  → ScopeMappingFunction.apply() merges defaultScopes ∪ existingScopes
  → Updated RequestScope sent to IdentityHub STS
  → IdentityHub returns presentation with matching credential types

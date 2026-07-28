# ARCHITECTURE.md

# Architecture: edc-controlplane

## Overview

The `edc-controlplane` is a custom packaging of the Eclipse Dataspace Connector (EDC) control plane runtime for the **Dataspace4Health (DS4H)** project. It bundles the standard EDC control plane with five local extensions that adapt it to the DS4H dataspace's trust, identity, and policy requirements.

The runtime is a single JVM process. All extensions are loaded at startup through the standard EDC service extension mechanism and participate in the runtime's lifecycle via `initialize()`.

---

## High-Level Architecture

```
┌────────────────────────────────────────────────────────────────────┐
│                     edc-controlplane (JVM)                         │
│                                                                    │
│  ┌──────────────────────────────────────────────────────────────┐  │
│  │                  EDC BaseRuntime                             │  │
│  │  ┌─────────────────────────────────────────────────────┐    │  │
│  │  │  Standard EDC Control Plane Modules                 │    │  │
│  │  │  • control-plane-core  • management-api             │    │  │
│  │  │  • dsp                 • edr-store                  │    │  │
│  │  │  • sql-* stores        • vault-hashicorp            │    │  │
│  │  │  • data-plane-selector • decentralized-claims-core  │    │  │
│  │  └─────────────────────────────────────────────────────┘    │  │
│  │                                                              │  │
│  │  ┌─────────────────────────────────────────────────────┐    │  │
│  │  │  DS4H Custom Extensions                             │    │  │
│  │  │  • PolicyEvaluationExtension (extensions/policy)   │    │  │
│  │  │  • ScopeExtension (extensions/scope)               │    │  │
│  │  │  • TrustedIssuerConfigurationExtension             │    │  │
│  │  │  • HttpClientExtension (extensions/common/http)    │    │  │
│  │  │  • S3DataAddressValidatorExtension (extensions/s3) │    │  │
│  │  │  • LegalContractingExtension (extensions/          │    │  │
│  │  │      legal-contracting)                            │    │  │
│  │  └─────────────────────────────────────────────────────┘    │  │
│  └──────────────────────────────────────────────────────────────┘  │
└────────────────────────────────────────────────────────────────────┘
         │ DSP :8092        │ Management :9192     │ Control :8093
         ▼                  ▼                      ▼
   Counterpart          edc-dashboard         edc-dataplane
   Control Plane        (operator UI)         (data transfer)

         │ HTTP :8181       │ Catalog :8194
         ▼                  ▼
   Health checks        Catalog API
```

---

## Extension Architecture

### Policy Extension (`extensions/policy`)

**Purpose**: Evaluates ODRL policy constraints against Verifiable Credentials (VCs) presented by counterpart participants.

**Design**:
```
PolicyEvaluationExtension
  ├── registerCatalogScope()       → 1 permission binding
  ├── registerNegotiationScope()   → 8 permission + 2 prohibition + 2 duty bindings
  └── registerTransferScope()      → 8 permission + 2 prohibition + 2 duty bindings

Each binding creates:
  AtomicConstraintRuleFunction<RuleType, ContextType>
    └── delegates to CredentialClaimEvaluator

CredentialClaimEvaluator
  extends AbstractCredentialEvaluationFunction
    └── getCredentialList(ParticipantAgent)
          └── reads agent.claims["vc"] → List<VerifiableCredential>
```

**Evaluation flow**:
1. EDC's DCP layer parses the counterpart's presentation token and stores `List<VerifiableCredential>` in `ParticipantAgent.claims["vc"]`.
2. `PolicyEngine` invokes the registered `AtomicConstraintRuleFunction` for each constraint in the policy.
3. `CredentialClaimEvaluator` iterates all VCs, searching `credentialSubject` for the named claim.
4. For `EQ`: returns `success(true)` if any subject contains a matching claim value.
5. For `NEQ`: returns `success(true)` if no subject contains the excluded value.
6. Permission wrapper: `success(true)` = allow. Prohibition wrapper: `success(true)` = block. Duty wrapper: `success(true)` = obligation met.

**Registered constraints**:

| Constraint key (IRI) | Claim | Scopes | Rule type |
|---|---|---|---|
| `…/membershipStatus` | `membershipStatus` | catalog, negotiation, transfer | Permission |
| `…/euEeaStatus` | `euEeaStatus` | negotiation, transfer | Permission + Prohibition |
| `…/organizationType` | `organizationType` | negotiation, transfer | Permission |
| `…/organizationOwnershipType` | `organizationOwnershipType` | negotiation, transfer | Permission + Prohibition |
| `…/organizationRoles` | `organizationRoles` | negotiation, transfer | Permission |
| `…/dataProtectionRole` | `dataProtectionRole` | negotiation, transfer | Permission |
| `…/jurisdictionCountry` | `jurisdictionCountry` | negotiation, transfer | Permission |
| `…/supportedProtocols` | `supportedProtocols` | negotiation, transfer | Permission |
| `…/dacApproval` | `dacApproval` | negotiation, transfer | Duty |
| `…/permitIssued` | `permitIssued` | negotiation, transfer | Duty |

All IRIs use the prefix `https://w3id.org/edc/v0.0.1/ns/`.

---

### Scope Extension (`extensions/scope`)

**Purpose**: Ensures outgoing requests to the counterpart's IdentityHub include the correct credential scopes so the IdentityHub returns the necessary VCs.

**Design**:
```
ScopeExtension.initialize()
  ├── reads edc.policy.catalog.scope  → ScopeMappingFunction(catalogScopes)
  ├── reads edc.policy.contract.scope → ScopeMappingFunction(contractScopes)
  └── reads edc.policy.transfer.scope → ScopeMappingFunction(transferScopes)

ScopeMappingFunction implements PolicyValidatorRule<RequestPolicyContext>
  └── apply(policy, context)
        └── requestScopeBuilder.scopes(defaultScopes ∪ existingScopes)
        └── returns true (always — this is a scope enricher, not a gatekeeper)
```

The post-validator fires after the request policy context is created, before policy evaluation. Returning `true` unconditionally ensures the scope merge never blocks the request.

---

### Trusted Issuer Configuration Extension (`extensions/common/decentralized-claims-issuers-configuration`)

**Purpose**: Populates `TrustedIssuerRegistry` at startup from `edc.iam.trustedissuer.*` configuration properties.

**Why local**: EDC 0.15.1 does not reliably parse configuration keys with hyphens when sourced from environment variables. The local implementation reads from the standard `Config` API, which handles all key formats correctly.

**Configuration structure**:
```
edc.iam.trustedissuer.<alias>.id             = <DID or URL of issuer>
edc.iam.trustedissuer.<alias>.supportedtypes = ["VerifiableCredential", "MembershipCredential"]
edc.iam.trustedissuer.<alias>.properties     = {}
```

Each alias is an arbitrary label used to partition the config. Multiple issuers are supported.

---

### HTTP Client Extension (`extensions/common/http/client`)

**Purpose**: Provides the shared `OkHttpClient` instance used by EDC runtime components for all outbound HTTP calls.

**Key decision**: Marked with `@Provider` so EDC's dependency injection system selects this instance over any default. The optional TLS bypass (`edc.http.disable.tls=true`) creates a client that trusts all certificates and skips hostname verification — intended exclusively for test clusters where internal services use self-signed certificates.

---

### Legal Contracting Extension (`extensions/legal-contracting`)

**Purpose**: Automatically generates a signed PDF legal contract whenever a contract negotiation reaches the FINALIZED state, and exposes the stored PDF via a Management API endpoint.

**Design**:
```
LegalContractingExtension.initialize()
  ├── reads edc.legal.contract.storage.dir
  │     → FilesystemContractDocumentStore(storageDir)
  ├── ContractDocumentGenerator(AssetService, ObjectMapper, Monitor, store)
  ├── ContractNegotiationObservable.registerListener(ContractNegotiationListenerImpl)
  └── WebService.registerResource("management", LegalContractApiController(store))

ContractNegotiationListenerImpl implements ContractNegotiationListener
  └── finalized(negotiation)
        └── ContractDocumentGenerator.generateAndStore(negotiation)
              ├── loads Asset via AssetService
              ├── ContractContentBuilder.build(...)   → List<ContentLine>
              ├── PdfPageWriter.renderLines(lines)    → PDDocument (pass 1)
              ├── sha256(pass-1 bytes)                → docHash
              ├── PdfPageWriter.renderLines(lines)    → PDDocument (pass 2, real hash)
              └── FilesystemContractDocumentStore.store(agreementId, pdfBytes)

LegalContractApiController  (JAX-RS, /v3/legal-contract)
  ├── GET /{agreementId}        → 200 application/pdf | 404
  └── GET /{agreementId}/status → 200 application/json {"agreementId":…,"pdfAvailable":…}
```

**PDF generation pipeline**:
1. `ContractNegotiationListenerImpl.finalized()` fires on **both** PROVIDER and CONSUMER sides when a negotiation reaches FINALIZED state. Both sides independently generate and persist their own copy of the PDF.
2. `ContractDocumentGenerator` loads the associated `Asset` via `AssetService`, serializes the ODRL policy to pretty-printed JSON, and computes a SHA-256 hash of the policy bytes.
3. `ContractContentBuilder` assembles an ordered list of `ContentLine` objects representing the 8-section document model. No I/O occurs here; this class is a pure content mapper.
4. `PdfPageWriter` paginates the `ContentLine` list into an A4 `PDDocument` using Apache PDFBox. Handles automatic page breaks, bold/regular field rendering, horizontal separator rules, and monospaced code blocks for the ODRL JSON annex.
5. **Two-pass hash embedding**: the document is rendered twice. The first-pass PDF bytes are SHA-256 hashed to obtain the `docHash`, which is then embedded into the second (final) pass. This makes the document self-certifying without requiring an external signature infrastructure.
6. The final PDF bytes are persisted by `FilesystemContractDocumentStore` as `<storageDir>/<sanitizedAgreementId>.pdf`.

**PDF document structure**:

| Section | Key content |
|---|---|
| 1. Agreement Identification | Agreement ID, signing date, negotiation ID/timestamp, type, DSP protocol, provider/consumer DIDs |
| 2. Parties | Provider and consumer DIDs; counterparty DSP address; DCP identity verification statement |
| 3. Dataset Schedule | Asset ID; HealthDCAT-AP properties (title, publisher, description, format, classification, version, checksum); additional custom properties |
| 4. Permissions | ODRL permissions: actions, atomic/multiplicity constraints |
| 5. Prohibitions | ODRL prohibitions |
| 6. Obligations | ODRL duties (including pre-condition duties and consequences) |
| 7. Legal Clauses | Placeholder — reserved for binding legal terms (pending legal working group) |
| 8. Technical Annex | Summary fields; document generation timestamp; SHA-256 document hash (self-referential); SHA-256 policy hash; full ODRL policy as JSON-LD |

**Error handling**: PDF generation failures are caught by `ContractNegotiationListenerImpl`, logged as SEVERE, but **not** re-thrown. This guarantees that a PDF failure never propagates into the EDC state machine or blocks the data-sharing flow.

**Storage**: `FilesystemContractDocumentStore` sanitizes agreement IDs (replacing non-alphanumeric/dash/dot/underscore characters with `_`) to prevent path traversal attacks. The storage directory is created lazily on first write.

**Configuration**:

| Property | Default | Description |
|---|---|---|
| `edc.legal.contract.storage.dir` | `/tmp/legal-contracts` | Directory where generated PDF files are stored |

**Injected services**:
- `Monitor` — EDC logging
- `ContractNegotiationObservable` — EDC contract negotiation event bus
- `AssetService` — resolves asset metadata for the Dataset Schedule section
- `WebService` — registers the JAX-RS controller on the management context
- `TypeManager` — provides the `ObjectMapper` used to serialize the ODRL policy

---

### S3 DataAddress Validator Extension (`extensions/s3`)

**Purpose**: Validates `AmazonS3` `DataAddress` objects at the control plane layer before a transfer is accepted.

**Validators**:
- **Source validator**: checks `bucketName`, `region`, and at least one of `objectName`, `folderName`, or `objectPrefix`.
- **Destination validator**: checks `bucketName`, `region`, and `keyName`.

---

## Data Flow

### Catalog Request (Provider side)
```
Consumer DSP request
  → EDC DSP module receives request
  → Creates RequestCatalogPolicyContext
  → ScopeExtension post-validator: merges default catalog scope into request
  → EDC DCP: requests credential presentation from consumer's IdentityHub
  → Consumer IdentityHub returns VCs for requested scopes
  → EDC DCP: parses VCs into ParticipantAgent
  → PolicyEngine evaluates catalog access policy
  → PolicyEvaluationExtension: checks membershipStatus == "active"
  → If pass: asset included in catalog response
  → If fail: asset excluded
```

### Contract Negotiation (Provider side)
```
Consumer negotiation request
  → EDC DSP module receives DSPACE:ContractRequestMessage
  → Creates ContractNegotiationPolicyContext
  → ScopeExtension post-validator: merges default contract scope
  → DCP + IdentityHub credential exchange
  → PolicyEngine evaluates contract policy
  → PolicyEvaluationExtension: checks all negotiation-scope constraints
  → All pass → contract agreement created
  → Any fail → negotiation terminated
```

---

## Build Architecture

```
edc-controlplane/
├── build.gradle.kts        ← root project: aggregates sources, defines dependencies
├── settings.gradle.kts     ← declares subprojects
├── extensions/
│   ├── policy/             ← subproject (dependencies only, no separate JAR)
│   ├── scope/              ← subproject
│   ├── s3/                 ← subproject
│   └── common/
│       ├── http/client/    ← subproject
│       └── decentralized-claims-issuers-configuration/ ← subproject
└── gradle/
    └── libs.versions.toml  ← version catalog
```

The root `build.gradle.kts` uses a custom `sourceSets` block that walks all `extensions/**/src/main` directories and registers them as source roots. This means all extension classes are compiled as part of the root project, and the Shadow plugin produces a single fat JAR containing everything.

---

## Deployment Architecture

```
Kubernetes Cluster
├── edc-controlplane Pod
│   ├── Container: edc-controlplane (UID 10100, non-root)
│   │   Ports: 8181, 9192, 8093, 8092, 8194
│   └── Env: EDC_* config vars from ConfigMap/Secret
├── PostgreSQL Pod (subchart)
│   └── Persistent Volume: 5Gi
├── HashiCorp Vault Pod (subchart)
│   └── Secrets: Vault KV
└── Ingress
    └── Routes all five ports to the control plane pod
```

The Helm chart at `helm-edc-connector/components/controlplane` manages the full deployment including PostgreSQL and Vault subcharts.

---

## Design Decisions

| Decision | Rationale |
|---|---|
| Generic `CredentialClaimEvaluator` instead of one class per constraint | Eliminates repetition; all claim types share the same scalar/list + EQ/NEQ logic |
| No credential type filter in claim lookup | Claim names (e.g. `euEeaStatus`) may appear in any VC type; type coupling would be fragile |
| `ScopeMappingFunction` always returns `true` | It enriches scope; policy enforcement is the evaluator's job, not the scope injector's |
| Flat source set aggregation | Simplifies Shadow JAR; avoids multi-artifact dependency management for a single deployable |
| Local trusted issuer extension | Pragmatic fix for EDC 0.15.1 config parsing bug; documented as removable on upgrade |

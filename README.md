# EDC Control Plane

Custom Eclipse Dataspace Connector (EDC) control plane runtime for the **Dataspace4Health (DS4H)** project, developed and maintained by NTT Data.

This repo packages the standard EDC control plane with a set of local extensions that adapt it to DS4H's trust, identity, and policy requirements — enabling credential-based access control, scope enrichment, and S3 data address validation.

## Documentation

| Document | Description |
|---|---|
| [ARCHITECTURE.md](ARCHITECTURE.md) | System architecture, extension design, and data flows |
| [DEPLOYMENT.md](DEPLOYMENT.md) | Deployment instructions, environment variables, and Helm configuration |
| [CONTRIBUTING.md](CONTRIBUTING.md) | Development setup, coding standards, and contribution guidelines |
| [CHANGELOG.md](CHANGELOG.md) | Version history and release notes |
| [SECURITY.md](SECURITY.md) | Security policies and vulnerability reporting |
| [TROUBLESHOOTING.md](TROUBLESHOOTING.md) | Common issues and debugging guidance |
| [INFRASTRUCTURE.md](INFRASTRUCTURE.md) | Helm and Kubernetes infrastructure reference |

## Overview

The control plane is the coordination layer of each DS4H participant connector. It handles:

- **Policy evaluation** — evaluates ODRL policy constraints against Verifiable Credentials presented by counterpart connectors
- **Scope enrichment** — injects default credential scopes into catalog, negotiation, and transfer requests
- **Contract negotiation** — drives the DSPACE contract negotiation protocol with counterparties
- **Transfer coordination** — manages transfer process state and delegates execution to the data plane
- **Asset and catalog management** — stores participant assets, policies, and contract definitions

## Custom Extensions

| Extension | Module | Description |
|---|---|---|
| `PolicyEvaluationExtension` | `extensions/policy` | Registers ODRL constraint functions that evaluate VC claims for catalog, negotiation, and transfer scopes |
| `ScopeExtension` | `extensions/scope` | Injects configured default scopes into outgoing requests |
| `TrustedIssuerConfigurationExtension` | `extensions/common/decentralized-claims-issuers-configuration` | Registers trusted VC issuers from configuration at startup |
| `HttpClientExtension` | `extensions/common/http/client` | Provides shared OkHttpClient with optional TLS bypass for non-production environments |
| `S3DataAddressValidatorExtension` | `extensions/s3` | Validates AmazonS3 DataAddress fields before a transfer is accepted |
| `LegalContractingExtension` | `extensions/legal-contracting` | (Unreleased) Generates a PDF technical agreement summary when contract negotiation is finalized and exposes it via the Management API. The PDF is not electronically signed and does not contain legal clauses |

## Quick Start

### Prerequisites

- Java 17
- Gradle 8.x (or use the included `./gradlew` wrapper)
- Docker

When it is used:

- during catalog access policy evaluation
- during contract negotiation policy evaluation
- during transfer process policy evaluation

### Scope extension

Module: `extensions/scope`

What it does:

- registers post-validators for request policy contexts
- injects configured default scopes into incoming:
    - catalog requests
    - contract negotiation requests
    - transfer requests

When it is used:

- after a request policy context is created and before policy evaluation continues
- to ensure the request contains the expected scope values even if the caller did not explicitly provide all of them

### Trusted issuer configuration extension

Module: `extensions/common/decentralized-claims-issuers-configuration`

What it does:

- reads trusted issuer entries from configuration at startup
- registers issuers and supported credential types into `TrustedIssuerRegistry`

Why this exists here:

- this extension is kept locally as a compatibility workaround for the EDC version currently used in this repo
- in this version line, trusted issuer settings backed by environment variables with hyphens were not being read reliably by the upstream code path
- this local extension makes trusted issuer registration predictable for the current runtime

When it is used:

- during runtime initialization
- later by decentralized claims validation when issuer trust must be checked against incoming Verifiable Credentials

Maintenance note:

- newer EDC versions already contain the fix for this configuration issue
- when this repo upgrades to an EDC version that includes that fix, this local extension should be removable and the upstream behavior should be sufficient

### HTTP client extension

Module: `extensions/common/http/client`

What it does:

- provides the shared `OkHttpClient` used by the runtime
- supports `edc.http.disable.tls=true` to create an insecure client for development and test environments

When it is used:

- whenever runtime components request the provided HTTP client through dependency injection
- especially for outbound HTTP calls to external services used by the control plane

### Legal Contracting Extension

Module: `extensions/legal-contracting`

What it does:

- generates human-readable PDF evidence documents for finalized EDC contract agreements
- renders ODRL policies in human-friendly format (e.g., "Membership Status = active" instead of technical constraint objects)
- embeds DATASPACE4HEALTH branding and logos
- stores PDFs in filesystem cache for fast retrieval
- provides REST API endpoints to download contract PDFs or check PDF availability

Key features:

- **On-demand PDF generation** — PDFs generated only when requested, not on every contract finalization
- **ODRL rendering** — Converts complex nested constraints (AndConstraint, OrConstraint) to readable format
- **Logo branding** — Embeds organization logo in top-right corner of page 1
- **7-section structure** — Includes agreement ID, parties, dataset schedule, permissions, prohibitions, obligations, and technical annex with hashes

Test coverage: **89%** (1110/1242 lines) with comprehensive unit tests for all core components.

When it is used:

- automatically triggered when a contract negotiation is finalized
- on-demand PDF generation when `/legal-contract/{agreementId}` endpoint is called
- PDF availability checks via `/legal-contract/{agreementId}/status` endpoint

For detailed architecture and design information, see [`extensions/legal-contracting/ARCHITECTURE.md`](extensions/legal-contracting/ARCHITECTURE.md) and [`extensions/legal-contracting/README.md`](extensions/legal-contracting/README.md).

### S3 validator extension

Module: `extensions/s3`

What it does:

- registers `DataAddress` validators for the `AmazonS3` type at the control-plane level
- validates that source `DataAddress` entries contain `bucketName`, `region`, and at least one of `objectName`, `folderName`, or `objectPrefix`
- validates that destination `DataAddress` entries contain `bucketName`, `region`, and `keyName`

When it is used:

- at contract definition and asset registration time, when the control plane validates incoming `DataAddress` fields before accepting the asset
- ensures malformed or incomplete S3 addresses are rejected early, before any transfer is attempted

## How The Repo Is Used At Runtime

1. The application starts the EDC base runtime.
2. Local service extensions are discovered from the packaged modules.
3. Startup extensions register policy functions, scope validators, trusted issuers, and providers.
4. At request time, the scope and policy extensions participate in authorization decisions for catalog, negotiation, and transfer flows.

## Build

Build and push the image from the repo root:

```bash
# Build fat JAR
./gradlew shadowJar

# Build and push Docker image
docker build -t localhost:5432/edc-controlplane . && docker push localhost:5432/edc-controlplane

# Run tests
./gradlew test

# Run Checkstyle
./gradlew checkstyleMain
```

### Minimum required environment variables

```bash
EDC_PARTICIPANT_ID=did:web:your-participant.dataspace4health.local
EDC_DSP_CALLBACK_ADDRESS=https://your-participant.dataspace4health.local/edc/cp/dsp/api
EDC_IAM_STS_OAUTH_TOKEN_URL=http://edc-identityhub:8185/edc/ih/sts/api/token
EDC_VAULT_HASHICORP_URL=http://vault:8200
EDC_VAULT_HASHICORP_TOKEN=<vault-token>
EDC_DATASOURCE_DEFAULT_URL=jdbc:postgresql://localhost:5432/edc_db
EDC_DATASOURCE_DEFAULT_USER=edc_user
EDC_DATASOURCE_DEFAULT_PASSWORD=<password>
```

See [DEPLOYMENT.md](DEPLOYMENT.md) for the full environment variable reference.

## API Ports

| Port | Purpose |
|---|---|
| 8181 | Health check (`/edc/cp/base/api/check/health`) |
| 9192 | Management API (asset, policy, contract, negotiation, transfer CRUD) |
| 8092 | DSP API (counterpart protocol endpoint) |
| 8093 | Control API (internal data plane coordination) |
| 8194 | Catalog API |

## Kubernetes Deployment

Use the Helm chart at `helm-edc-connector/components/controlplane`. See [DEPLOYMENT.md](DEPLOYMENT.md) and [INFRASTRUCTURE.md](INFRASTRUCTURE.md) for details.

## Contributing

Add new extensions under the existing `extensions/**` structure and register them through the appropriate `META-INF/services/org.eclipse.edc.spi.system.ServiceExtension` entry in that module.

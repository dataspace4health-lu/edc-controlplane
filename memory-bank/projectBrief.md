# memory-bank/projectBrief.md

# Project Brief: edc-controlplane

## Overview

Custom Eclipse Dataspace Connector (EDC) Control Plane implementation for the **Dataspace4Health (DS4H)** project, developed and maintained by NTT Data.

The repository extends the standard EDC control plane runtime with a set of custom EDC service extensions that enable credential-based policy enforcement, request-scope injection, trusted issuer registration, and configurable HTTP client behavior.

## Repository

- **Group**: `com.nttdata.dataspace`
- **Package root**: `com.nttdata.dataspace.cp`
- **Build tool**: Gradle 8.8 with Shadow plugin

## Core Goals

1. Enforce ODRL policies against Verifiable Credentials (VCs) presented by counterpart participants via the DCP token at catalog, negotiation, and transfer time.
2. Inject configured default scopes into catalog, contract negotiation, and data transfer requests so the IdentityHub returns the correct credentials.
3. Register trusted decentralized credential issuers from runtime configuration, working around a configuration-parsing limitation in EDC 0.15.1.
4. Provide a shared, configurable OkHttpClient with optional TLS bypass for non-production environments.
5. Validate Amazon S3 `DataAddress` properties on both source and destination sides at the control plane layer.
6. Package the runtime and all extensions as a single deployable fat JAR inside a secure, non-root Docker container.

## Modules

| Module | Path | Description |
|---|---|---|
| `policy` | `extensions/policy` | Credential claim evaluators registered with the EDC PolicyEngine for catalog, negotiation, and transfer scopes |
| `scope` | `extensions/scope` | Post-validators that merge default scopes into incoming request policy contexts |
| `s3` | `extensions/s3` | Source and destination `DataAddress` validators for the `AmazonS3` type |
| `client` | `extensions/common/http/client` | Configurable OkHttpClient provider — supports TLS bypass via `edc.http.disable.tls=true` |
| `decentralized-claims-issuers-configuration` | `extensions/common/decentralized-claims-issuers-configuration` | Startup extension reading trusted issuers from `edc.iam.trustedissuer.*` config |

## Scope Boundaries

- This repository is **control plane only**. Data plane functionality lives in `edc-dataplane`.
- No custom catalog storage or asset management — delegated to the standard EDC modules.
- No custom negotiation or transfer orchestration — standard EDC control plane core is used.
- Credential issuance and DID resolution are delegated to the IdentityHub (`edc-identityhub`).

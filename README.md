# EDC Control Plane

This repo packages a custom EDC control plane runtime plus a small set of extensions that adapt request-scope handling, credential-based policy evaluation, trusted issuer registration, and HTTP client behavior.

## What This Repo Provides

The application boots the standard EDC runtime through `org.eclipse.edc.boot.system.runtime.BaseRuntime` and adds local extensions from `extensions/**`.

The main technical functions are:

- credential-based policy evaluation for catalog, contract negotiation, and transfer processing
- request-scope enrichment for catalog, contract negotiation, and transfer requests
- startup registration of trusted decentralized credential issuers from configuration
- a configurable shared OkHttp client with optional TLS verification bypass for non-production environments

## Runtime Extensions

### Policy extension

Module: `extensions/policy`

What it does:

- registers policy functions in the EDC `PolicyEngine`
- evaluates permission, prohibition, and duty constraints against claims in the participant's Verifiable Credentials
- binds different checks to the right policy scope:
    - catalog request time
    - contract negotiation time
    - transfer start time

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

## How The Repo Is Used At Runtime

1. The application starts the EDC base runtime.
2. Local service extensions are discovered from the packaged modules.
3. Startup extensions register policy functions, scope validators, trusted issuers, and providers.
4. At request time, the scope and policy extensions participate in authorization decisions for catalog, negotiation, and transfer flows.

## Build

Build and push the image from the repo root:

```bash
docker build -t localhost:5432/edc-controlplane . && docker push localhost:5432/edc-controlplane
```

## Contributing

Add new extensions under the existing `extensions/**` structure and register them through the appropriate `META-INF/services/org.eclipse.edc.spi.system.ServiceExtension` entry in that module.
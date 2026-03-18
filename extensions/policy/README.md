# Policy Evaluation Extension

Plugs credential-based policy evaluation into the EDC control plane by registering constraint evaluation functions with the EDC `PolicyEngine`.

---

## What is evaluated

Policies are enforced against the **Verifiable Credentials** presented by the counterpart participant via the DCP token. The constraint `leftOperand` in a policy definition (e.g. `membershipStatus`) is expanded by the EDC JSON-LD processor to a fully-qualified IRI (e.g. `https://w3id.org/edc/v0.0.1/ns/membershipStatus`) and matched against the registered functions.

All constraints check a named claim inside `credentialSubject` of the presented VC. Scalar claims are checked with direct equality; list claims (e.g. `organizationRoles`) pass if the required value is present anywhere in the list.

---

## Constraints per scope

**Catalog scope** — lightweight visibility gate (access policy):

| Constraint | Claim | Valid values |
|---|---|---|
| `membershipStatus` | `credentialSubject.membershipStatus` | `active`, `suspended`, `revoked`, `pending` |

**Negotiation & Transfer scopes** — full attribute verification (contract policy):

| Constraint | Claim | Valid values |
|---|---|---|
| `membershipStatus` | `credentialSubject.membershipStatus` | `active`, `suspended`, `revoked`, `pending` |
| `euEeaStatus` | `credentialSubject.euEeaStatus` | `eu-member-state`, `eea-member`, `third-country` |
| `organizationType` | `credentialSubject.organizationType` | see `PolicyConstants.VALID_ORGANIZATION_TYPES` |
| `organizationOwnershipType` | `credentialSubject.organizationOwnershipType` | `public`, `private` |
| `organizationRoles` | `credentialSubject.organizationRoles` | see `PolicyConstants.VALID_ORGANIZATION_ROLES` |
| `dataProtectionRole` | `credentialSubject.dataProtectionRole` | `controller`, `joint-controller`, `processor` |
| `jurisdictionCountry` | `credentialSubject.jurisdictionCountry` | any ISO 3166-1 alpha-2 code |
| `supportedProtocols` | `credentialSubject.supportedProtocols` | `dataspace-protocol-http:2025-1` |

---

## Adding a new policy constraint

`CredentialClaimEvaluationFunction` is generic and handles any `credentialSubject` attribute — no new class is needed for simple scalar or list claims. Add the constant to `PolicyConstants`, then register it in the appropriate scope method in `PolicyEvaluationExtension`.

If the new policy requires logic that goes beyond a `credentialSubject` claim check (e.g. cross-credential validation, time-based rules, or a different credential type structure), a new evaluation function implementing `AtomicConstraintRuleFunction` must be written and registered.

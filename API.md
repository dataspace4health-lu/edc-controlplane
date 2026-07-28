# API Reference: edc-controlplane

This document describes the REST API endpoints exposed by the `edc-controlplane` runtime, including the custom DS4H extensions. For standard EDC Management API endpoints (assets, policies, contract definitions, negotiations, transfers) refer to the upstream [EDC Management API documentation](https://eclipse-edc.github.io/documentation/).

---

## Ports

| Port | Context | Base path |
|---|---|---|
| 8181 | Health | `/edc/cp/base/api` |
| 9192 | Management | `/management` |
| 8092 | DSP | `/edc/cp/dsp/api` |
| 8093 | Control | `/edc/cp/control/api` |
| 8194 | Catalog | `/edc/cp/catalog/api` |

---

## Health API — port 8181

### `GET /edc/cp/base/api/check/health`

Returns runtime liveness status.

**Response `200 application/json`**

```json
{
  "componentResults": [
    { "component": "Liveness", "isHealthy": true }
  ],
  "isSystemHealthy": true
}
```

---

## Management API — port 9192

All endpoints under `/management` require authentication credentials configured in the EDC runtime (API key or token, depending on deployment).

### Legal Contracting — `extensions/legal-contracting`

These endpoints are added by `LegalContractingExtension` and are registered on the same Management API context (port 9192) as the standard EDC Management API endpoints.

---

#### `GET /management/v3/legal-contract/{agreementId}`

Downloads the legal contract PDF for the given contract agreement.

The PDF is generated automatically when the contract negotiation reaches the FINALIZED state. Both the **provider** and **consumer** sides independently generate and store their own copy.

**Path parameters**

| Parameter | Type | Description |
|---|---|---|
| `agreementId` | string | The contract agreement ID. URL-encode if the ID contains special characters. |

**Responses**

| Status | Content-Type | Description |
|---|---|---|
| `200 OK` | `application/pdf` | Binary PDF bytes. Response includes `Content-Disposition: attachment; filename="contract-<agreementId>.pdf"` and `Content-Length` headers. |
| `404 Not Found` | `text/plain` | No PDF has been generated yet for this agreement ID. This occurs if the negotiation has not yet reached FINALIZED state, or if PDF generation failed. |

**Example**

```bash
curl -H "X-Api-Key: $MGMT_API_KEY" \
     "https://your-participant.dataspace4health.local/management/v3/legal-contract/urn:uuid:abc123" \
     --output contract-abc123.pdf
```

---

#### `GET /management/v3/legal-contract/{agreementId}/status`

Returns a JSON object indicating whether a PDF has been generated and stored for the given agreement ID, without downloading the document.

**Path parameters**

| Parameter | Type | Description |
|---|---|---|
| `agreementId` | string | The contract agreement ID. |

**Responses**

| Status | Content-Type | Description |
|---|---|---|
| `200 OK` | `application/json` | JSON object with `agreementId` and `pdfAvailable` fields. `pdfAvailable` is `true` if a PDF is stored and ready for download. |

**Response body**

```json
{
  "agreementId": "urn:uuid:abc123",
  "pdfAvailable": true
}
```

**Example**

```bash
curl -H "X-Api-Key: $MGMT_API_KEY" \
     "https://your-participant.dataspace4health.local/management/v3/legal-contract/urn:uuid:abc123/status"
```

---

### PDF Document Structure

The generated PDF is a paginated A4 document organised into eight sections:

| Section | Content |
|---|---|
| **1. Agreement Identification** | Contract agreement ID, signing date, negotiation ID, finalization timestamp, negotiation type, DSP protocol, provider DID, consumer DID |
| **2. Parties** | Provider and consumer DIDs and counterparty addresses; DCP identity verification statement |
| **3. Dataset Schedule** | EDC asset ID and all HealthDCAT-AP metadata properties (title, publisher, description, format, classification, version, checksum) plus any additional custom properties |
| **4. Permissions** | All ODRL permissions from the contract policy, with actions and constraints rendered in human-readable form |
| **5. Prohibitions** | All ODRL prohibitions from the contract policy |
| **6. Obligations** | All ODRL obligations (duties) from the contract policy |
| **7. Legal Clauses** | Placeholder section reserved for binding legal terms (pending legal working group review) |
| **8. Technical Annex** | Provider/consumer DIDs, DSP protocol, correlation ID; document generation timestamp; SHA-256 hash of the full document (self-referential, computed in a two-pass render); SHA-256 hash of the ODRL policy JSON; full ODRL policy as pretty-printed JSON-LD |

**Document integrity**: The PDF is produced in two passes. The first pass renders the document with a placeholder hash; the SHA-256 digest of that first-pass PDF is then embedded into the final (second-pass) document. This allows downstream consumers to verify the PDF has not been tampered with by recomputing the hash of the document with the `Document Hash (SHA-256)` field blanked out.

---

## Standard EDC Management API

The following standard EDC endpoints are available at port 9192. Refer to the upstream EDC documentation for full schemas.

| Resource | Endpoint prefix |
|---|---|
| Assets | `POST /management/v3/assets` |
| Policy definitions | `POST /management/v3/policydefinitions` |
| Contract definitions | `POST /management/v3/contractdefinitions` |
| Contract negotiations | `POST /management/v3/contractnegotiations` |
| Transfer processes | `POST /management/v3/transferprocesses` |
| EDR (Endpoint Data References) | `GET /management/v3/edrs` |
| Catalog requests | `POST /management/v3/catalog/request` |

---

## DSP API — port 8092

The DSP (Dataspace Protocol) API is used exclusively for machine-to-machine communication between connector control planes. It is not intended for direct human or dashboard interaction.

| Operation | Endpoint |
|---|---|
| Catalog request | `POST /edc/cp/dsp/api/catalog/request` |
| Contract negotiation | `POST /edc/cp/dsp/api/negotiations/{id}/request` |
| Transfer request | `POST /edc/cp/dsp/api/transfers/{id}/request` |

---

## Control API — port 8093

Internal API used by the data plane to coordinate transfer state transitions. Not exposed outside the cluster.

---

## Error Handling

All Management API endpoints follow standard EDC error response conventions:

```json
{
  "type": "about:blank",
  "title": "Not Found",
  "status": 404,
  "detail": "No legal contract PDF found for agreementId: urn:uuid:abc123"
}
```

| HTTP Status | Meaning |
|---|---|
| `200` | Success |
| `400` | Bad request (missing or invalid parameters) |
| `401` | Unauthorized (missing or invalid API key) |
| `404` | Resource not found |
| `500` | Internal server error |

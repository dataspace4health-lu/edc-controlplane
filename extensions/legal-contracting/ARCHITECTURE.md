# Legal Contracting Extension - Architecture & Design

## High-Level Overview

The Legal Contracting extension generates human-readable PDF evidence documents for finalized EDC contract agreements. It operates as an asynchronous, event-driven service triggered when contracts are finalized.

### Core Workflow

```
Contract Finalized
       ↓
ContractNegotiationListenerImpl
       ↓
ContractDocumentGenerator (Orchestrator)
       ├─ Fetch Agreement + Asset metadata
       ├─ Build ContentLine list (ContractContentBuilder)
       ├─ Render PDF (PdfPageWriter)
       └─ Store PDF (FilesystemContractDocumentStore)
       ↓
PDF Available via REST API
```

## Architecture Components

### 1. ContractDocumentGenerator
**Purpose:** Orchestrates PDF generation pipeline

**Key Responsibilities:**
- Coordinates 2-pass PDF rendering (hash generation + final PDF)
- Fetches agreement, asset, and policy data
- Calculates SHA-256 hashes for document integrity
- Stores PDF in filesystem cache
- Graceful error handling (missing assets, null agreements)

**Algorithm:**
```
Pass 1: Generate minimal PDF
  → Extract content hash (SHA-256)

Pass 2: Generate final PDF
  → Embed content hash reference
  → Include logo branding
  → Apply formatting

Store in FilesystemContractDocumentStore
```

### 2. ContractContentBuilder
**Purpose:** Transforms agreement/policy data into human-readable PDF sections

**7-Section Structure:**
1. **Header** - Title, subtitle, generation timestamp
2. **Section 1** - Agreement Identification (IDs, DIDs, timestamps)
3. **Section 2** - Parties (Provider/Consumer, VC verification)
4. **Section 3** - Dataset Schedule (HealthDCAT-AP metadata)
5. **Section 4** - Permissions (ODRL policy rendering)
6. **Section 5** - Prohibitions (ODRL policy rendering)
7. **Section 6** - Obligations (ODRL policy rendering)
8. **Section 7** - Technical Annex (Hashes, ODRL JSON-LD, metadata)

**ODRL Constraint Rendering:**

The extension converts complex ODRL constraints into human-readable format:

```
Constraint Type: AtomicConstraint
├─ Left Operand: membershipStatus
├─ Operator: EQ
└─ Right Operand: active

Output: "Membership Status = active"
```

```
Constraint Type: AndConstraint (wraps 2 AtomicConstraints)
├─ Inner 1: membershipStatus EQ active
└─ Inner 2: organizationType EQ hospital

Output:
ALL of:
  Membership Status = active
  Organization Type = hospital
```

**Operator Mapping:**
- `EQ` → `=`
- `NEQ` → `!=`
- `GT` → `>`
- `GEQ` → `>=`
- `LT` → `<`
- `LEQ` → `<=`
- `IN` → `in`
- `HAS_PART` → `has part`
- `IS_PART_OF` → `is part of`
- `IS_ANY_OF` → `is any of`
- `IS_ALL_OF` → `is all of`
- `IS_NONE_OF` → `is none of`

**CamelCase to Title Case Conversion:**
- `membershipStatus` → `Membership Status`
- `euEeaStatus` → `EU EEA Status`
- `organizationType` → `Organization Type`
- `dacApproval` → `DAC Approval`

### 3. PdfPageWriter
**Purpose:** PDFBox-based PDF rendering engine

**Features:**
- A4 page size (612 × 792 points) with margins
- Automatic pagination on content overflow
- Branded title ("DATASPACE4HEALTH" with color gradients)
- Logo embedding (top-right, page 1 only)
- Text wrapping and line spacing

**Logo Rendering:**
```java
if (pageNumber == 1 && logoBytes != null) {
  // Load PNG image from byte array
  PDImageXObject image = PDImageXObject.createFromByteArray(doc, logoBytes, "logo");
  
  // Scale to 36pt height (preserve aspect ratio)
  float scaledWidth = (image.getWidth() / image.getHeight()) * 36;
  
  // Position: top-right corner
  float x = PAGE_WIDTH - scaledWidth - RIGHT_MARGIN;
  float y = TOP_MARGIN + 72;  // Below header
  
  // Render with transparency
  drawImage(image, x, y, scaledWidth, 36);
}
```

**Content Rendering:**
- `ContentLine.title()` → Font size 24, bold, centered
- `ContentLine.heading()` → Font size 14, bold, left-aligned
- `ContentLine.field(label, value)` → Label bold, value regular
- `ContentLine.body()` → Font size 11, wrapped text
- `ContentLine.mono()` → Monospace (for JSON snippets)

### 4. FilesystemContractDocumentStore
**Purpose:** File-based PDF cache with security measures

**Storage Path:**
```
/tmp/legal-contracts/
  ├─ d4afee2f-76af-4ad8-98da-6ffcb2fb5930.pdf
  ├─ a1b2c3d4-e5f6-7890-abcd-ef1234567890.pdf
  └─ ...
```

**Path Traversal Prevention:**
```java
String sanitizeFilename(String agreementId) {
  // Replace path separators and special chars
  return agreementId.replaceAll("[/\\\\:.]+", "_") + ".pdf";
}
```

Examples:
- `../../etc/passwd` → `.._.._etc_passwd.pdf`
- `agreement:id:v2` → `agreement_id_v2.pdf`

**Interface:**
- `store(agreementId, pdfBytes)` - Write to disk
- `retrieve(agreementId)` - Load from cache
- `exists(agreementId)` - Check file existence

### 5. ContentLine Model
**Purpose:** Immutable abstraction for PDF text elements

**Line Types:**
```
TITLE      - Large, centered heading
SUBTITLE   - Subheading below title
HEADING    - Section heading (bold, large)
SUBHEADING - Subsection heading
FIELD      - Key-value pair (label: value)
BODY       - Paragraph text
SMALL      - Small text (metadata)
SEPARATOR  - Horizontal line
SPACER     - Blank line
MONO       - Monospace (code/JSON)
```

**Factory Methods:**
```java
ContentLine.title("Agreement Evidence")
ContentLine.field("Agreement ID", "agr-001")
ContentLine.body("This is a paragraph...")
ContentLine.mono("{\"policy\": {...}}")
```

### 6. REST API Layer (LegalContractApiController)

**Endpoints:**

#### 1. Download Contract PDF
```
GET /edc/cp/management/api/v3/legal-contract/{agreementId}
```

**Behavior:**
1. Check cache: `store.retrieve(agreementId)`
2. If hit → Return PDF bytes (HTTP 200)
3. If miss → Find negotiation via service
4. Generate PDF on-demand
5. Return bytes (HTTP 200) or error (404/500)

**Response Headers:**
```
Content-Type: application/pdf
Content-Disposition: attachment; filename="contract-{agreementId}.pdf"
```

**Error Cases:**
- Agreement not found → HTTP 404
- PDF generation fails → HTTP 500
- Special chars in filename → Sanitized

#### 2. Check Contract Status
```
GET /edc/cp/management/api/v3/legal-contract/{agreementId}/status
```

**Response (HTTP 200):**
```json
{
  "agreementId": "d4afee2f-76af-4ad8-98da-6ffcb2fb5930",
  "pdfAvailable": true
}
```

### 7. Lifecycle Hook

**ContractNegotiationListenerImpl**
- Implements `ContractNegotiationStateChangedListener`
- Triggers on negotiation state → `FINALIZED`
- Calls `generator.generateAndStore(negotiation)`
- Errors logged but never propagated (non-blocking)

## Test Coverage

**Overall: 89% (1110/1242 lines)**

| Class | Coverage | Focus Areas |
|-------|----------|------------|
| ContentLine | 100% | All 11 factory methods |
| LegalContractingException | 100% | Both constructors |
| ContractNegotiationListenerImpl | 100% | Callback logic, error handling |
| ContractContentBuilder | 98% | Section builders, ODRL rendering |
| LegalContractApiController | 92% | HTTP endpoints, edge cases |
| PdfPageWriter | 87% | PDF rendering, logo embedding |
| FilesystemContractDocumentStore | 85% | Store/retrieve, path traversal |
| ContractDocumentGenerator | 72% | 2-pass generation, hashing |

## Error Handling Strategy

### Silent Failures (Never Disrupt EDC)
- Missing asset → Log warning, render "N/A"
- Logo loading fails → Continue without logo
- PDF generation timeout → Catch, log, return gracefully
- I/O error on store → Wrap in `LegalContractingException`

### Explicit Failures (Early Return)
- Null agreement → Skip generation, return null
- Invalid negotiation ID → Return empty Optional

### API Error Responses
- Agreement not found → HTTP 404
- Generation fails → HTTP 500
- Success → HTTP 200

## Performance Considerations

- **Caching:** PDFs cached to filesystem, reused across requests
- **2-Pass Rendering:** First pass small (hash only), second pass full PDF
- **Async Generation:** Generated on-demand (not on every contract finalization)
- **Storage:** `/tmp/legal-contracts/` allows fast SSD access

## Security & Compliance

- **Path Traversal:** Sanitize agreement IDs before filename
- **Content Integrity:** SHA-256 hashes for audit trail
- **Document Authenticity:** ODRL policy embedded in annex
- **Data Minimization:** Only essential metadata in PDF
- **Graceful Degradation:** Missing assets don't break document

## Dependencies

```gradle
implementation 'org.apache.pdfbox:pdfbox:3.x'
implementation 'org.eclipse.edc:contract-spi'
implementation 'org.eclipse.edc:asset-spi'
implementation 'org.eclipse.edc:web-spi'
implementation 'org.eclipse.edc:control-plane-spi'
implementation 'com.fasterxml.jackson.core:jackson-databind'
```

## Extension Registration

The `LegalContractingExtension` implements `ServiceExtension` and:
1. Registers HTTP endpoints via `WebService`
2. Registers contract negotiation listener
3. Initializes file cache directory (`/tmp/legal-contracts/`)
4. Starts background cleanup (optional)

```
META-INF/services/org.eclipse.edc.spi.system.ServiceExtension
→ com.nttdata.dataspace.cp.legalcontracting.LegalContractingExtension
```

## Future Enhancements

1. **Digital Signatures** - Sign PDFs with issuer certificate
2. **Encryption** - Encrypt PDFs with party public keys
3. **Multi-language** - Localized labels (DE, FR, IT, etc.)
4. **Audit Log** - Track all PDF generations for compliance
5. **Email Integration** - Auto-send PDFs to parties
6. **Archival** - Long-term storage with retention policies
7. **Customizable Templates** - Per-dataspace PDF layouts

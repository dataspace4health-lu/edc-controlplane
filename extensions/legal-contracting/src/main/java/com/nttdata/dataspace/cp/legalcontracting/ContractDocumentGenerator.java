package com.nttdata.dataspace.cp.legalcontracting;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset;
import org.eclipse.edc.connector.controlplane.contract.spi.types.agreement.ContractAgreement;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.connector.controlplane.services.spi.asset.AssetService;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.monitor.Monitor;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Orchestrates legal contract PDF generation from a finalised
 * {@link ContractNegotiation}.
 *
 * <p>Workflow:
 * <ol>
 *   <li>Listen for {@code finalized()} via {@link ContractNegotiationListenerImpl}</li>
 *   <li>Load asset metadata from {@link AssetService}</li>
 *   <li>Delegate 8-section content assembly to {@link ContractContentBuilder}</li>
 *   <li>Render paginated PDF via {@link PdfPageWriter}</li>
 *   <li>Two-pass SHA-256 hash embedding for document integrity</li>
 *   <li>Persist PDF bytes via {@link ContractDocumentStore}</li>
 * </ol>
 */
public class ContractDocumentGenerator {

    private static final DateTimeFormatter ISO_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'").withZone(ZoneOffset.UTC);

    private final AssetService assetService;
    private final ObjectMapper mapper;
    private final Monitor monitor;
    private final ContractDocumentStore store;

    /** Logo PNG bytes loaded from classpath, or null if not found. */
    private final byte[] logoBytes;

    public ContractDocumentGenerator(AssetService assetService,
                                     ObjectMapper objectMapper,
                                     Monitor monitor,
                                     ContractDocumentStore store) {
        this.assetService = assetService;
        this.mapper = objectMapper.copy()
                .enable(SerializationFeature.INDENT_OUTPUT)
                .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        this.monitor = monitor;
        this.store = store;
        this.logoBytes = loadLogo();
    }

    private byte[] loadLogo() {
        try (InputStream is = getClass().getResourceAsStream("/logo.png")) {
            if (is == null) {
                return null;
            }
            return is.readAllBytes();
        } catch (Exception e) {
            monitor.warning("[LegalContracting] Could not load logo.png from classpath: " + e.getMessage());
            return null;
        }
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Generates a legal contract PDF and persists it for the given finalised negotiation.
     *
     * @param negotiation the finalised contract negotiation (must have a non-null agreement)
     */
    public void generateAndStore(ContractNegotiation negotiation) {
        ContractAgreement agreement = negotiation.getContractAgreement();
        if (agreement == null) {
            monitor.warning("[LegalContracting] No ContractAgreement on negotiation "
                    + negotiation.getId() + " — skipping PDF generation.");
            return;
        }
        String agreementId = agreement.getId();
        monitor.debug("[LegalContracting] Generating PDF for agreementId=" + agreementId);
        try {
            byte[] pdf = buildPdf(negotiation, agreement);
            store.store(agreementId, pdf);
            monitor.info("[LegalContracting] Legal contract PDF stored for agreementId=" + agreementId);
        } catch (Exception e) {
            monitor.severe("[LegalContracting] PDF generation failed for agreementId="
                    + agreementId + ": " + e.getMessage());
            throw new LegalContractingException(
                    "PDF generation failed for agreementId=" + agreementId, e);
        }
    }

    // ── PDF construction (two-pass for self-referential hash) ────────────────

    private byte[] buildPdf(ContractNegotiation negotiation, ContractAgreement agreement)
            throws IOException {
        String timestamp = ISO_FMT.format(Instant.now());
        Asset asset = loadAsset(agreement.getAssetId());
        String odrlJson = serializePolicy(agreement.getPolicy());
        String metaHash = sha256Hex(odrlJson.getBytes(StandardCharsets.UTF_8));

        // First pass: placeholder hash to compute final document hash
        byte[] firstPass = renderPdf(negotiation, agreement, asset, odrlJson, metaHash,
                timestamp, "COMPUTING...");
        String docHash = sha256Hex(firstPass);

        // Second pass: embed the real document hash
        return renderPdf(negotiation, agreement, asset, odrlJson, metaHash, timestamp, docHash);
    }

    private byte[] renderPdf(ContractNegotiation negotiation,
                              ContractAgreement agreement,
                              Asset asset,
                              String odrlJson,
                              String metaHash,
                              String timestamp,
                              String docHash) throws IOException {

        List<ContentLine> lines = ContractContentBuilder.build(
                negotiation, agreement, asset, odrlJson, metaHash, timestamp, docHash);

        try (PDDocument doc = new PDDocument();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfPageWriter writer = new PdfPageWriter(doc, logoBytes);
            writer.renderLines(lines);
            doc.save(baos);
            return baos.toByteArray();
        }
    }

    // ── Utility methods ───────────────────────────────────────────────────────

    private Asset loadAsset(String assetId) {
        if (assetId == null || assetId.isBlank()) {
            return null;
        }
        try {
            return assetService.findById(assetId);
        } catch (Exception e) {
            monitor.warning("[LegalContracting] Could not load asset '" + assetId
                    + "' for contract PDF: " + e.getMessage());
            return null;
        }
    }

    private String serializePolicy(Policy policy) {
        if (policy == null) {
            return "{}";
        }
        try {
            return mapper.writeValueAsString(policy);
        } catch (Exception e) {
            monitor.warning("[LegalContracting] Failed to serialize policy to JSON: " + e.getMessage());
            return "{\"error\": \"serialization failed\"}";
        }
    }

    static String sha256Hex(byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(data);
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new LegalContractingException("SHA-256 not available", e);
        }
    }
}

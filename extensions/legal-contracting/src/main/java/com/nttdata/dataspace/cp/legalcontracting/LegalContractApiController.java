package com.nttdata.dataspace.cp.legalcontracting;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.connector.controlplane.services.spi.contractnegotiation.ContractNegotiationService;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;

import java.util.List;
import java.util.Optional;

/**
 * JAX-RS controller that exposes legal contract PDFs over the EDC management API.
 *
 * <p>Endpoint: {@code GET /management/v3/legal-contract/{agreementId}}
 *
 * <p>Returns the PDF binary for the given contract agreement ID with
 * {@code Content-Type: application/pdf} and a {@code Content-Disposition}
 * header that prompts browser download.
 *
 * <p>If no PDF has been pre-generated (e.g. for agreements finalised before
 * the extension was deployed), the PDF is generated on-demand by looking up
 * the associated {@link ContractNegotiation} and delegating to
 * {@link ContractDocumentGenerator}. Returns {@code 404} only when no matching
 * negotiation can be found.
 *
 * <p>This controller is registered on the {@code "management"} web service context
 * by {@link LegalContractingExtension} and therefore shares the same port and
 * authentication as the standard EDC Management API (default: 9192).
 */
@Path("/v3/legal-contract")
public class LegalContractApiController {

    private static final String MEDIA_TYPE_PDF = "application/pdf";

    private final ContractDocumentStore store;
    private final ContractDocumentGenerator generator;
    private final ContractNegotiationService negotiationService;
    private final Monitor monitor;

    public LegalContractApiController(ContractDocumentStore store,
                                      ContractDocumentGenerator generator,
                                      ContractNegotiationService negotiationService,
                                      Monitor monitor) {
        this.store = store;
        this.generator = generator;
        this.negotiationService = negotiationService;
        this.monitor = monitor;
    }

    /**
     * Downloads the legal contract PDF for the given contract agreement ID.
     * If no pre-generated PDF exists the PDF is produced on-demand.
     *
     * @param agreementId the contract agreement ID (URL-encoded if necessary)
     * @return {@code 200 application/pdf} with the PDF bytes, or {@code 404} if no
     *         matching negotiation can be found
     */
    @GET
    @Path("/{agreementId}")
    @Produces(MEDIA_TYPE_PDF)
    public Response downloadContractPdf(@PathParam("agreementId") String agreementId) {
        // 1. Try to return a cached PDF
        Optional<byte[]> cached = store.retrieve(agreementId);
        if (cached.isPresent()) {
            return pdfResponse(agreementId, cached.get());
        }

        // 2. No cached PDF — generate on-demand
        monitor.debug("[LegalContracting] No cached PDF for agreementId=" + agreementId
                + " — attempting on-demand generation.");

        Optional<ContractNegotiation> negotiation = findNegotiationByAgreementId(agreementId);
        if (negotiation.isEmpty()) {
            monitor.warning("[LegalContracting] No ContractNegotiation found for agreementId=" + agreementId);
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("No legal contract PDF found for agreementId: " + agreementId)
                    .build();
        }

        try {
            generator.generateAndStore(negotiation.get());
        } catch (LegalContractingException e) {
            monitor.severe("[LegalContracting] On-demand PDF generation failed for agreementId="
                    + agreementId + ": " + e.getMessage());
            return Response.serverError()
                    .entity("PDF generation failed for agreementId: " + agreementId)
                    .build();
        }

        return store.retrieve(agreementId)
                .map(pdfBytes -> pdfResponse(agreementId, pdfBytes))
                .orElseGet(() -> Response.serverError()
                        .entity("PDF was generated but could not be retrieved for agreementId: " + agreementId)
                        .build());
    }

    /**
     * Returns {@code 200} with a JSON body indicating whether a PDF exists
     * for the given agreement ID, without downloading the full document.
     *
     * @param agreementId the contract agreement ID
     * @return {@code 200} with {@code {"agreementId":"…","pdfAvailable":true|false}}
     */
    @GET
    @Path("/{agreementId}/status")
    @Produces("application/json")
    public Response getContractStatus(@PathParam("agreementId") String agreementId) {
        boolean exists = store.exists(agreementId);
        String body = "{\"agreementId\": \"" + agreementId + "\", \"pdfAvailable\": " + exists + "}";
        return Response.ok(body).build();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Response pdfResponse(String agreementId, byte[] pdfBytes) {
        return Response.ok(pdfBytes, MEDIA_TYPE_PDF)
                .header("Content-Disposition",
                        "attachment; filename=\"contract-" + sanitizeFilename(agreementId) + ".pdf\"")
                .header("Content-Length", pdfBytes.length)
                .build();
    }

    private Optional<ContractNegotiation> findNegotiationByAgreementId(String agreementId) {
        try {
            var query = QuerySpec.Builder.newInstance()
                    .filter(List.of(new Criterion("contractAgreement.id", "=", agreementId)))
                    .limit(1)
                    .build();
            var result = negotiationService.search(query);
            if (result.failed()) {
                monitor.warning("[LegalContracting] Negotiation search failed: " + result.getFailureDetail());
                return Optional.empty();
            }
            var list = result.getContent();
            return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
        } catch (Exception e) {
            monitor.warning("[LegalContracting] Exception querying negotiation for agreementId="
                    + agreementId + ": " + e.getMessage());
            return Optional.empty();
        }
    }

    private static String sanitizeFilename(String id) {
        return id.replaceAll("[^a-zA-Z0-9_\\-.]", "_");
    }
}

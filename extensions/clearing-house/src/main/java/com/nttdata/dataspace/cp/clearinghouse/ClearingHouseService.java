package com.nttdata.dataspace.cp.clearinghouse;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.edc.spi.monitor.Monitor;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Posts clearing house entries to the Digital Clearing House REST API.
 * All HTTP calls are fire-and-forget (non-blocking) and never throw.
 */
public class ClearingHouseService {

    private static final String ENTRIES_PATH = "/api/entries";
    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    private final String clearingHouseUrl;
    private final String participantId;
    private final Monitor monitor;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final ExecutorService executor;

    public ClearingHouseService(String clearingHouseUrl, String participantId, Monitor monitor) {
        this.clearingHouseUrl = clearingHouseUrl.replaceAll("/$", "");
        this.participantId = participantId;
        this.monitor = monitor.withPrefix("ClearingHouseService");
        this.objectMapper = new ObjectMapper();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(TIMEOUT)
                .build();
        this.executor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "clearing-house-poster");
            t.setDaemon(true);
            return t;
        });
    }

    /**
     * Asynchronously posts a negotiation event to the clearing house.
     *
     * @param action        one of NEGOTIATION_REQUESTED, NEGOTIATION_FINALIZED, NEGOTIATION_TERMINATED
     * @param transactionId the EDC contract negotiation ID
     * @param dataUser      DID/URL of the consumer participant
     * @param dataHolder    DID/URL of the provider participant
     * @param contractId    contract agreement ID (may be null for early lifecycle events)
     * @param assetId       the asset / data product ID (may be null)
     * @param status        the EDC state string
     */
    public void logNegotiationEvent(String action, String transactionId,
                                    String dataUser, String dataHolder,
                                    String contractId, String assetId, String status) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("action", action);
        payload.put("transactionId", transactionId);
        payload.put("dataUser", dataUser != null ? dataUser : participantId);
        payload.put("dataHolder", dataHolder != null ? dataHolder : "unknown");
        payload.put("status", status);
        payload.put("timestamp", Instant.now().toString());
        if (contractId != null) payload.put("contractId", contractId);
        if (assetId != null) payload.put("assetId", assetId);

        postAsync(payload);
    }

    /**
     * Asynchronously posts a transfer process event to the clearing house.
     *
     * @param action        one of TRANSFER_REQUESTED, TRANSFER_STARTED, TRANSFER_COMPLETED,
     *                      TRANSFER_SUSPENDED, TRANSFER_TERMINATED
     * @param transactionId the EDC transfer process ID
     * @param dataUser      DID/URL of the consumer participant
     * @param dataHolder    DID/URL of the provider participant
     * @param contractId    the associated contract agreement ID
     * @param assetId       the asset / data product ID
     * @param status        the EDC state string
     */
    public void logTransferEvent(String action, String transactionId,
                                 String dataUser, String dataHolder,
                                 String contractId, String assetId, String status) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("action", action);
        payload.put("transactionId", transactionId);
        payload.put("dataUser", dataUser != null ? dataUser : participantId);
        payload.put("dataHolder", dataHolder != null ? dataHolder : "unknown");
        payload.put("status", status);
        payload.put("timestamp", Instant.now().toString());
        if (contractId != null) payload.put("contractId", contractId);
        if (assetId != null) payload.put("assetId", assetId);

        postAsync(payload);
    }

    // ── Private helpers ────────────────────────────────────────────────────────

    private void postAsync(Map<String, Object> payload) {
        executor.submit(() -> {
            try {
                String json = objectMapper.writeValueAsString(payload);
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(clearingHouseUrl + ENTRIES_PATH))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(json))
                        .timeout(TIMEOUT)
                        .build();

                HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
                if (response.statusCode() >= 300) {
                    monitor.warning("Clearing house returned HTTP " + response.statusCode()
                            + " for action " + payload.get("action"));
                }
            } catch (IOException | InterruptedException e) {
                monitor.warning("Failed to post to clearing house: " + e.getMessage());
                if (e instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
            }
        });
    }

    /** Shuts down the async posting thread gracefully. */
    public void shutdown() {
        executor.shutdown();
    }
}

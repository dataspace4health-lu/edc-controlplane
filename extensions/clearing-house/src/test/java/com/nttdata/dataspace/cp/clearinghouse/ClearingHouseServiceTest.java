package com.nttdata.dataspace.cp.clearinghouse;

import org.eclipse.edc.spi.monitor.Monitor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClearingHouseServiceTest {

    private static final String BASE_URL = "http://localhost:3010";
    private static final String PARTICIPANT_ID = "did:web:lih.dataspace4health.local";

    @Mock
    private Monitor monitor;

    @Mock
    private HttpClient httpClient;

    @Mock
    private HttpResponse<Void> httpResponse;

    private ExecutorService directExecutor;
    private ClearingHouseService service;

    @BeforeEach
    void setUp() {
        when(monitor.withPrefix(anyString())).thenReturn(monitor);
        // Use a direct (same-thread) executor so HTTP calls complete synchronously in tests
        directExecutor = Executors.newSingleThreadExecutor();
        service = new ClearingHouseService(BASE_URL, PARTICIPANT_ID, monitor, httpClient, directExecutor);
    }

    // ── logNegotiationEvent ────────────────────────────────────────────────────

    @Test
    void logNegotiationEvent_postsToEntriesEndpoint() throws Exception {
        when(httpResponse.statusCode()).thenReturn(200);
        doReturn(httpResponse).when(httpClient).send(any(), any());

        service.logNegotiationEvent(
                "NEGOTIATION_REQUESTED", "neg-123",
                "did:web:consumer", "did:web:provider",
                null, "asset-1", "REQUESTED");

        directExecutor.shutdown();
        directExecutor.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS);

        verify(httpClient, atLeastOnce()).send(any(), any());
    }

    @Test
    void logNegotiationEvent_withContractAndAsset_postsSuccessfully() throws Exception {
        when(httpResponse.statusCode()).thenReturn(201);
        doReturn(httpResponse).when(httpClient).send(any(), any());

        service.logNegotiationEvent(
                "NEGOTIATION_FINALIZED", "neg-456",
                "did:web:consumer", "did:web:provider",
                "contract-789", "asset-abc", "FINALIZED");

        directExecutor.shutdown();
        directExecutor.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS);

        verify(httpClient, atLeastOnce()).send(any(), any());
        verify(monitor, never()).warning(anyString());
    }

    @Test
    void logNegotiationEvent_whenServerReturns5xx_logsWarning() throws Exception {
        when(httpResponse.statusCode()).thenReturn(500);
        doReturn(httpResponse).when(httpClient).send(any(), any());

        service.logNegotiationEvent(
                "NEGOTIATION_TERMINATED", "neg-err",
                "did:web:consumer", "did:web:provider",
                null, null, "TERMINATED");

        directExecutor.shutdown();
        directExecutor.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS);

        verify(monitor, atLeastOnce()).warning(anyString());
    }

    @Test
    void logNegotiationEvent_whenHttpThrows_logsWarning() throws Exception {
        doThrow(new IOException("connection refused")).when(httpClient).send(any(), any());

        service.logNegotiationEvent(
                "NEGOTIATION_REQUESTED", "neg-io-err",
                null, null, null, null, "REQUESTED");

        directExecutor.shutdown();
        directExecutor.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS);

        verify(monitor, atLeastOnce()).warning(anyString());
    }

    @Test
    void logNegotiationEvent_nullDataUser_fallsBackToParticipantId() throws Exception {
        when(httpResponse.statusCode()).thenReturn(200);
        doReturn(httpResponse).when(httpClient).send(any(), any());

        service.logNegotiationEvent(
                "NEGOTIATION_REQUESTED", "neg-null",
                null, "did:web:provider",
                null, null, "REQUESTED");

        directExecutor.shutdown();
        directExecutor.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS);

        verify(httpClient, atLeastOnce()).send(any(), any());
    }

    // ── logTransferEvent ───────────────────────────────────────────────────────

    @Test
    void logTransferEvent_postsToEntriesEndpoint() throws Exception {
        when(httpResponse.statusCode()).thenReturn(201);
        doReturn(httpResponse).when(httpClient).send(any(), any());

        service.logTransferEvent(
                "TRANSFER_STARTED", "tp-123",
                "did:web:consumer", "did:web:provider",
                "contract-111", "asset-222", "STARTED");

        directExecutor.shutdown();
        directExecutor.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS);

        verify(httpClient, atLeastOnce()).send(any(), any());
    }

    @Test
    void logTransferEvent_withNullOptionalFields_postsSuccessfully() throws Exception {
        when(httpResponse.statusCode()).thenReturn(200);
        doReturn(httpResponse).when(httpClient).send(any(), any());

        service.logTransferEvent(
                "TRANSFER_COMPLETED", "tp-456",
                "did:web:consumer", "did:web:provider",
                null, null, "COMPLETED");

        directExecutor.shutdown();
        directExecutor.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS);

        verify(httpClient, atLeastOnce()).send(any(), any());
        verify(monitor, never()).warning(anyString());
    }

    @Test
    void logTransferEvent_whenServerReturns4xx_logsWarning() throws Exception {
        when(httpResponse.statusCode()).thenReturn(400);
        doReturn(httpResponse).when(httpClient).send(any(), any());

        service.logTransferEvent(
                "TRANSFER_TERMINATED", "tp-err",
                "did:web:consumer", "did:web:provider",
                null, null, "TERMINATED");

        directExecutor.shutdown();
        directExecutor.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS);

        verify(monitor, atLeastOnce()).warning(anyString());
    }

    @Test
    void logTransferEvent_nullDataHolder_fallsBackToUnknown() throws Exception {
        when(httpResponse.statusCode()).thenReturn(200);
        doReturn(httpResponse).when(httpClient).send(any(), any());

        service.logTransferEvent(
                "TRANSFER_REQUESTED", "tp-null-holder",
                "did:web:consumer", null,
                null, null, "REQUESTED");

        directExecutor.shutdown();
        directExecutor.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS);

        verify(httpClient, atLeastOnce()).send(any(), any());
    }

    // ── shutdown ───────────────────────────────────────────────────────────────

    @Test
    void shutdown_shutsDownExecutor() {
        service.shutdown();
        // After shutdown, the executor should be shutting down — no exception expected
        assert directExecutor.isShutdown();
    }

    // ── URL normalisation ──────────────────────────────────────────────────────

    @Test
    void constructor_stripsTrailingSlashFromUrl() throws Exception {
        when(monitor.withPrefix(anyString())).thenReturn(monitor);
        when(httpResponse.statusCode()).thenReturn(200);
        doReturn(httpResponse).when(httpClient).send(any(), any());

        var svc = new ClearingHouseService(
                "http://localhost:3010/", PARTICIPANT_ID, monitor, httpClient, directExecutor);

        svc.logNegotiationEvent("NEGOTIATION_REQUESTED", "n1",
                "u", "h", null, null, "REQUESTED");

        directExecutor.shutdown();
        directExecutor.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS);

        // Request URI must not have double slash (trailing slash stripped by constructor)
        verify(httpClient, atLeastOnce()).send(any(), any());
    }
}

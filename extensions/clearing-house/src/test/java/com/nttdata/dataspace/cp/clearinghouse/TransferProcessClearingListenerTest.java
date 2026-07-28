package com.nttdata.dataspace.cp.clearinghouse;

import org.eclipse.edc.connector.controlplane.contract.spi.negotiation.store.ContractNegotiationStore;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.connector.controlplane.transfer.spi.event.TransferProcessCompleted;
import org.eclipse.edc.connector.controlplane.transfer.spi.event.TransferProcessRequested;
import org.eclipse.edc.connector.controlplane.transfer.spi.event.TransferProcessStarted;
import org.eclipse.edc.connector.controlplane.transfer.spi.event.TransferProcessSuspended;
import org.eclipse.edc.connector.controlplane.transfer.spi.event.TransferProcessTerminated;
import org.eclipse.edc.connector.controlplane.transfer.spi.store.TransferProcessStore;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess;
import org.eclipse.edc.spi.event.EventEnvelope;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.query.QuerySpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Collections;
import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TransferProcessClearingListenerTest {

    private static final String PARTICIPANT_ID = "did:web:hrs.dataspace4health.local";
    private static final String COUNTER_PARTY_DID = "did:web:lih.dataspace4health.local";
    private static final String COUNTER_PARTY_DSP = "http://edc-controlplane.lih.svc:8092/dsp";
    private static final String TRANSFER_ID = "tp-001";
    private static final String CONTRACT_ID = "contract-001";
    private static final String ASSET_ID = "asset-001";

    @Mock
    private ClearingHouseService clearingHouseService;

    @Mock
    private TransferProcessStore transferProcessStore;

    @Mock
    private ContractNegotiationStore negotiationStore;

    @Mock
    private Monitor monitor;

    private TransferProcessClearingListener listener;

    @BeforeEach
    void setUp() {
        when(monitor.withPrefix(anyString())).thenReturn(monitor);
        listener = new TransferProcessClearingListener(
                clearingHouseService, transferProcessStore, negotiationStore, PARTICIPANT_ID, monitor);
    }

    // ── TRANSFER_REQUESTED ─────────────────────────────────────────────────────

    @Test
    void onTransferRequested_callsLogTransferEventWithCorrectAction() {
        var transfer = consumerTransfer(COUNTER_PARTY_DSP, CONTRACT_ID, ASSET_ID);
        when(transferProcessStore.findById(TRANSFER_ID)).thenReturn(transfer);
        when(negotiationStore.queryNegotiations(any(QuerySpec.class)))
                .thenReturn(Stream.empty());

        listener.on(envelope(requested(TRANSFER_ID)));

        verify(clearingHouseService).logTransferEvent(
                eq("TRANSFER_REQUESTED"), eq(TRANSFER_ID),
                any(), any(), any(), any(), any());
    }

    // ── TRANSFER_STARTED ───────────────────────────────────────────────────────

    @Test
    void onTransferStarted_callsLogTransferEventWithCorrectAction() {
        var transfer = consumerTransfer(COUNTER_PARTY_DSP, CONTRACT_ID, ASSET_ID);
        when(transferProcessStore.findById(TRANSFER_ID)).thenReturn(transfer);
        when(negotiationStore.queryNegotiations(any(QuerySpec.class)))
                .thenReturn(Stream.empty());

        listener.on(envelope(started(TRANSFER_ID)));

        verify(clearingHouseService).logTransferEvent(
                eq("TRANSFER_STARTED"), eq(TRANSFER_ID),
                any(), any(), any(), any(), any());
    }

    // ── TRANSFER_COMPLETED ─────────────────────────────────────────────────────

    @Test
    void onTransferCompleted_callsLogTransferEventWithCorrectAction() {
        var transfer = consumerTransfer(COUNTER_PARTY_DSP, CONTRACT_ID, ASSET_ID);
        when(transferProcessStore.findById(TRANSFER_ID)).thenReturn(transfer);
        when(negotiationStore.queryNegotiations(any(QuerySpec.class)))
                .thenReturn(Stream.empty());

        listener.on(envelope(completed(TRANSFER_ID)));

        verify(clearingHouseService).logTransferEvent(
                eq("TRANSFER_COMPLETED"), eq(TRANSFER_ID),
                any(), any(), any(), any(), any());
    }

    // ── TRANSFER_SUSPENDED ─────────────────────────────────────────────────────

    @Test
    void onTransferSuspended_callsLogTransferEventWithCorrectAction() {
        var transfer = consumerTransfer(COUNTER_PARTY_DSP, CONTRACT_ID, ASSET_ID);
        when(transferProcessStore.findById(TRANSFER_ID)).thenReturn(transfer);
        when(negotiationStore.queryNegotiations(any(QuerySpec.class)))
                .thenReturn(Stream.empty());

        listener.on(envelope(suspended(TRANSFER_ID)));

        verify(clearingHouseService).logTransferEvent(
                eq("TRANSFER_SUSPENDED"), eq(TRANSFER_ID),
                any(), any(), any(), any(), any());
    }

    // ── TRANSFER_TERMINATED ────────────────────────────────────────────────────

    @Test
    void onTransferTerminated_callsLogTransferEventWithCorrectAction() {
        var transfer = consumerTransfer(COUNTER_PARTY_DSP, CONTRACT_ID, ASSET_ID);
        when(transferProcessStore.findById(TRANSFER_ID)).thenReturn(transfer);
        when(negotiationStore.queryNegotiations(any(QuerySpec.class)))
                .thenReturn(Stream.empty());

        listener.on(envelope(terminated(TRANSFER_ID)));

        verify(clearingHouseService).logTransferEvent(
                eq("TRANSFER_TERMINATED"), eq(TRANSFER_ID),
                any(), any(), any(), any(), any());
    }

    // ── provider / consumer role distinction ──────────────────────────────────

    @Test
    void onTransferEvent_asConsumer_dataUserIsParticipant() {
        var transfer = consumerTransfer(COUNTER_PARTY_DSP, CONTRACT_ID, ASSET_ID);
        when(transferProcessStore.findById(TRANSFER_ID)).thenReturn(transfer);
        when(negotiationStore.queryNegotiations(any(QuerySpec.class)))
                .thenReturn(Stream.empty());

        listener.on(envelope(started(TRANSFER_ID)));

        verify(clearingHouseService).logTransferEvent(
                any(), any(),
                eq(PARTICIPANT_ID),   // consumer → dataUser = self
                eq(COUNTER_PARTY_DSP), // fallback to DSP address
                any(), any(), any());
    }

    @Test
    void onTransferEvent_asProvider_dataUserIsCounterParty() {
        var transfer = providerTransfer(COUNTER_PARTY_DSP, CONTRACT_ID, ASSET_ID);
        when(transferProcessStore.findById(TRANSFER_ID)).thenReturn(transfer);
        when(negotiationStore.queryNegotiations(any(QuerySpec.class)))
                .thenReturn(Stream.empty());

        listener.on(envelope(started(TRANSFER_ID)));

        verify(clearingHouseService).logTransferEvent(
                any(), any(),
                eq(COUNTER_PARTY_DSP), // provider → dataUser = consumer (resolved to DSP fallback)
                eq(PARTICIPANT_ID),    // provider → dataHolder = self
                any(), any(), any());
    }

    // ── DID resolution from negotiation store ─────────────────────────────────

    @Test
    void onTransferEvent_whenNegotiationHasCounterPartyId_usesDid() {
        var transfer = consumerTransfer(COUNTER_PARTY_DSP, CONTRACT_ID, ASSET_ID);
        when(transferProcessStore.findById(TRANSFER_ID)).thenReturn(transfer);

        var negotiation = mockNegotiation(COUNTER_PARTY_DID);
        when(negotiationStore.queryNegotiations(any(QuerySpec.class)))
                .thenReturn(Stream.of(negotiation));

        listener.on(envelope(started(TRANSFER_ID)));

        verify(clearingHouseService).logTransferEvent(
                any(), any(),
                eq(PARTICIPANT_ID),    // consumer → dataUser = self
                eq(COUNTER_PARTY_DID), // resolved DID from negotiation
                any(), any(), any());
    }

    @Test
    void onTransferEvent_whenNegotiationCounterPartyIdIsBlank_fallsBackToDspAddress() {
        var transfer = consumerTransfer(COUNTER_PARTY_DSP, CONTRACT_ID, ASSET_ID);
        when(transferProcessStore.findById(TRANSFER_ID)).thenReturn(transfer);

        var negotiation = mockNegotiation("");
        when(negotiationStore.queryNegotiations(any(QuerySpec.class)))
                .thenReturn(Stream.of(negotiation));

        listener.on(envelope(started(TRANSFER_ID)));

        verify(clearingHouseService).logTransferEvent(
                any(), any(),
                eq(PARTICIPANT_ID),
                eq(COUNTER_PARTY_DSP), // blank counterPartyId → fallback to DSP address
                any(), any(), any());
    }

    // ── no transfer found ─────────────────────────────────────────────────────

    @Test
    void onTransferEvent_whenNoTransferFound_stillCallsService() {
        when(transferProcessStore.findById(TRANSFER_ID)).thenReturn(null);

        listener.on(envelope(started(TRANSFER_ID)));

        verify(clearingHouseService).logTransferEvent(
                eq("TRANSFER_STARTED"), eq(TRANSFER_ID),
                eq(PARTICIPANT_ID), eq("unknown"),
                any(), any(), any());
    }

    // ── asset and contract IDs forwarded ──────────────────────────────────────

    @Test
    void onTransferEvent_forwardsAssetIdAndContractId() {
        var transfer = consumerTransfer(COUNTER_PARTY_DSP, CONTRACT_ID, ASSET_ID);
        when(transferProcessStore.findById(TRANSFER_ID)).thenReturn(transfer);
        when(negotiationStore.queryNegotiations(any(QuerySpec.class)))
                .thenReturn(Stream.empty());

        listener.on(envelope(completed(TRANSFER_ID)));

        verify(clearingHouseService).logTransferEvent(
                any(), any(), any(), any(),
                eq(CONTRACT_ID), eq(ASSET_ID), any());
    }

    // ── unhandled event types are silently ignored ────────────────────────────

    @Test
    void onUnknownEventType_doesNotCallClearingHouseService() {
        var unknownEvent = org.mockito.Mockito.mock(org.eclipse.edc.spi.event.Event.class);
        @SuppressWarnings("unchecked")
        EventEnvelope<org.eclipse.edc.spi.event.Event> envelope =
                org.mockito.Mockito.mock(EventEnvelope.class);
        when(envelope.getPayload()).thenReturn(unknownEvent);

        listener.on(envelope);

        verify(clearingHouseService, never()).logTransferEvent(
                any(), any(), any(), any(), any(), any(), any());
    }

    // ── Helper factories ───────────────────────────────────────────────────────

    private <E extends org.eclipse.edc.spi.event.Event> EventEnvelope<E> envelope(E event) {
        @SuppressWarnings("unchecked")
        EventEnvelope<E> env = org.mockito.Mockito.mock(EventEnvelope.class);
        when(env.getPayload()).thenReturn(event);
        return env;
    }

    private TransferProcessRequested requested(String id) {
        var e = org.mockito.Mockito.mock(TransferProcessRequested.class);
        when(e.getTransferProcessId()).thenReturn(id);
        return e;
    }

    private TransferProcessStarted started(String id) {
        var e = org.mockito.Mockito.mock(TransferProcessStarted.class);
        when(e.getTransferProcessId()).thenReturn(id);
        return e;
    }

    private TransferProcessCompleted completed(String id) {
        var e = org.mockito.Mockito.mock(TransferProcessCompleted.class);
        when(e.getTransferProcessId()).thenReturn(id);
        return e;
    }

    private TransferProcessSuspended suspended(String id) {
        var e = org.mockito.Mockito.mock(TransferProcessSuspended.class);
        when(e.getTransferProcessId()).thenReturn(id);
        return e;
    }

    private TransferProcessTerminated terminated(String id) {
        var e = org.mockito.Mockito.mock(TransferProcessTerminated.class);
        when(e.getTransferProcessId()).thenReturn(id);
        return e;
    }

    private TransferProcess consumerTransfer(String counterPartyAddress, String contractId, String assetId) {
        var tp = org.mockito.Mockito.mock(TransferProcess.class);
        when(tp.getType()).thenReturn(TransferProcess.Type.CONSUMER);
        when(tp.getCounterPartyAddress()).thenReturn(counterPartyAddress);
        when(tp.getContractId()).thenReturn(contractId);
        when(tp.getAssetId()).thenReturn(assetId);
        return tp;
    }

    private TransferProcess providerTransfer(String counterPartyAddress, String contractId, String assetId) {
        var tp = org.mockito.Mockito.mock(TransferProcess.class);
        when(tp.getType()).thenReturn(TransferProcess.Type.PROVIDER);
        when(tp.getCounterPartyAddress()).thenReturn(counterPartyAddress);
        when(tp.getContractId()).thenReturn(contractId);
        when(tp.getAssetId()).thenReturn(assetId);
        return tp;
    }

    private ContractNegotiation mockNegotiation(String counterPartyId) {
        var neg = org.mockito.Mockito.mock(ContractNegotiation.class);
        when(neg.getCounterPartyId()).thenReturn(counterPartyId);
        return neg;
    }
}

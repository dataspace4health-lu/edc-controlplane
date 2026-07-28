package com.nttdata.dataspace.cp.clearinghouse;

import org.eclipse.edc.connector.controlplane.contract.spi.event.contractnegotiation.ContractNegotiationAgreed;
import org.eclipse.edc.connector.controlplane.contract.spi.event.contractnegotiation.ContractNegotiationFinalized;
import org.eclipse.edc.connector.controlplane.contract.spi.event.contractnegotiation.ContractNegotiationRequested;
import org.eclipse.edc.connector.controlplane.contract.spi.event.contractnegotiation.ContractNegotiationTerminated;
import org.eclipse.edc.connector.controlplane.contract.spi.negotiation.store.ContractNegotiationStore;
import org.eclipse.edc.connector.controlplane.contract.spi.types.agreement.ContractAgreement;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.spi.event.EventEnvelope;
import org.eclipse.edc.spi.monitor.Monitor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ContractNegotiationClearingListenerTest {

    private static final String PARTICIPANT_ID = "did:web:hrs.dataspace4health.local";
    private static final String COUNTER_PARTY_ID = "did:web:lih.dataspace4health.local";
    private static final String NEGOTIATION_ID = "neg-001";
    private static final String CONTRACT_ID = "contract-001";
    private static final String ASSET_ID = "asset-001";

    @Mock
    private ClearingHouseService clearingHouseService;

    @Mock
    private ContractNegotiationStore negotiationStore;

    @Mock
    private Monitor monitor;

    private ContractNegotiationClearingListener listener;

    @BeforeEach
    void setUp() {
        when(monitor.withPrefix(anyString())).thenReturn(monitor);
        listener = new ContractNegotiationClearingListener(
                clearingHouseService, negotiationStore, PARTICIPANT_ID, monitor);
    }

    // ── NEGOTIATION_REQUESTED ──────────────────────────────────────────────────

    @Test
    void onNegotiationRequested_callsLogNegotiationEvent() {
        var negotiation = consumerNegotiation(null, null);
        when(negotiationStore.findById(NEGOTIATION_ID)).thenReturn(negotiation);

        listener.on(envelope(requested(NEGOTIATION_ID)));

        verify(clearingHouseService).logNegotiationEvent(
                eq("NEGOTIATION_REQUESTED"), eq(NEGOTIATION_ID),
                any(), any(), any(), any(), any());
    }

    @Test
    void onNegotiationRequested_whenNoNegotiationFound_usesUnknownCounterParty() {
        when(negotiationStore.findById(NEGOTIATION_ID)).thenReturn(null);

        listener.on(envelope(requested(NEGOTIATION_ID)));

        verify(clearingHouseService).logNegotiationEvent(
                eq("NEGOTIATION_REQUESTED"), eq(NEGOTIATION_ID),
                eq(PARTICIPANT_ID), eq("unknown"),
                any(), any(), any());
    }

    // ── NEGOTIATION_AGREED ─────────────────────────────────────────────────────

    @Test
    void onNegotiationAgreed_callsLogNegotiationEventWithCorrectAction() {
        var negotiation = consumerNegotiation(null, null);
        when(negotiationStore.findById(NEGOTIATION_ID)).thenReturn(negotiation);

        listener.on(envelope(agreed(NEGOTIATION_ID)));

        verify(clearingHouseService).logNegotiationEvent(
                eq("NEGOTIATION_AGREED"), eq(NEGOTIATION_ID),
                any(), any(), any(), any(), any());
    }

    // ── NEGOTIATION_FINALIZED ──────────────────────────────────────────────────

    @Test
    void onNegotiationFinalized_callsLogNegotiationEventWithContractInfo() {
        var agreement = mockAgreement(CONTRACT_ID, ASSET_ID);
        var negotiation = consumerNegotiationWithAgreement(agreement);
        when(negotiationStore.findById(NEGOTIATION_ID)).thenReturn(negotiation);

        listener.on(envelope(finalized(NEGOTIATION_ID)));

        verify(clearingHouseService).logNegotiationEvent(
                eq("NEGOTIATION_FINALIZED"), eq(NEGOTIATION_ID),
                eq(PARTICIPANT_ID), eq(COUNTER_PARTY_ID),
                eq(CONTRACT_ID), eq(ASSET_ID), any());
    }

    @Test
    void onNegotiationFinalized_asConsumer_dataUserIsParticipant() {
        var negotiation = consumerNegotiation(COUNTER_PARTY_ID, ASSET_ID);
        when(negotiationStore.findById(NEGOTIATION_ID)).thenReturn(negotiation);

        listener.on(envelope(finalized(NEGOTIATION_ID)));

        verify(clearingHouseService).logNegotiationEvent(
                eq("NEGOTIATION_FINALIZED"), eq(NEGOTIATION_ID),
                eq(PARTICIPANT_ID),    // consumer → dataUser = self
                eq(COUNTER_PARTY_ID), // provider → dataHolder = counterParty
                any(), any(), any());
    }

    @Test
    void onNegotiationFinalized_asProvider_dataUserIsCounterParty() {
        var negotiation = providerNegotiation(COUNTER_PARTY_ID, ASSET_ID);
        when(negotiationStore.findById(NEGOTIATION_ID)).thenReturn(negotiation);

        listener.on(envelope(finalized(NEGOTIATION_ID)));

        verify(clearingHouseService).logNegotiationEvent(
                eq("NEGOTIATION_FINALIZED"), eq(NEGOTIATION_ID),
                eq(COUNTER_PARTY_ID), // provider receives from consumer → dataUser = consumer
                eq(PARTICIPANT_ID),   // provider → dataHolder = self
                any(), any(), any());
    }

    // ── NEGOTIATION_TERMINATED ─────────────────────────────────────────────────

    @Test
    void onNegotiationTerminated_callsLogNegotiationEventWithCorrectAction() {
        var negotiation = consumerNegotiation(COUNTER_PARTY_ID, null);
        when(negotiationStore.findById(NEGOTIATION_ID)).thenReturn(negotiation);

        listener.on(envelope(terminated(NEGOTIATION_ID)));

        verify(clearingHouseService).logNegotiationEvent(
                eq("NEGOTIATION_TERMINATED"), eq(NEGOTIATION_ID),
                any(), any(), any(), any(), any());
    }

    // ── counterPartyId resolution ──────────────────────────────────────────────

    @Test
    void onEvent_whenCounterPartyIdIsBlank_fallsBackToCounterPartyAddress() {
        var negotiation = consumerNegotiationWithAddress("", "http://edc-controlplane.lih.svc:8092/dsp");
        when(negotiationStore.findById(NEGOTIATION_ID)).thenReturn(negotiation);

        listener.on(envelope(requested(NEGOTIATION_ID)));

        verify(clearingHouseService).logNegotiationEvent(
                any(), any(),
                eq(PARTICIPANT_ID),
                eq("http://edc-controlplane.lih.svc:8092/dsp"),
                any(), any(), any());
    }

    @Test
    void onEvent_whenCounterPartyIdIsSet_usesCounterPartyId() {
        var negotiation = consumerNegotiation(COUNTER_PARTY_ID, null);
        when(negotiationStore.findById(NEGOTIATION_ID)).thenReturn(negotiation);

        listener.on(envelope(requested(NEGOTIATION_ID)));

        verify(clearingHouseService).logNegotiationEvent(
                any(), any(),
                eq(PARTICIPANT_ID),
                eq(COUNTER_PARTY_ID),
                any(), any(), any());
    }

    // ── unhandled event types are ignored ─────────────────────────────────────

    @Test
    void onUnknownEventType_doesNotCallClearingHouseService() {
        // Wrap an unknown event type — should be silently ignored
        var unknownEvent = org.mockito.Mockito.mock(org.eclipse.edc.spi.event.Event.class);
        @SuppressWarnings("unchecked")
        EventEnvelope<org.eclipse.edc.spi.event.Event> envelope =
                org.mockito.Mockito.mock(EventEnvelope.class);
        when(envelope.getPayload()).thenReturn(unknownEvent);

        listener.on(envelope);

        verify(clearingHouseService, never()).logNegotiationEvent(
                any(), any(), any(), any(), any(), any(), any());
    }

    // ── Helper factories ───────────────────────────────────────────────────────

    private <E extends org.eclipse.edc.spi.event.Event> EventEnvelope<E> envelope(E event) {
        @SuppressWarnings("unchecked")
        EventEnvelope<E> env = org.mockito.Mockito.mock(EventEnvelope.class);
        when(env.getPayload()).thenReturn(event);
        return env;
    }

    private ContractNegotiationRequested requested(String id) {
        var e = org.mockito.Mockito.mock(ContractNegotiationRequested.class);
        when(e.getContractNegotiationId()).thenReturn(id);
        return e;
    }

    private ContractNegotiationAgreed agreed(String id) {
        var e = org.mockito.Mockito.mock(ContractNegotiationAgreed.class);
        when(e.getContractNegotiationId()).thenReturn(id);
        return e;
    }

    private ContractNegotiationFinalized finalized(String id) {
        var e = org.mockito.Mockito.mock(ContractNegotiationFinalized.class);
        when(e.getContractNegotiationId()).thenReturn(id);
        return e;
    }

    private ContractNegotiationTerminated terminated(String id) {
        var e = org.mockito.Mockito.mock(ContractNegotiationTerminated.class);
        when(e.getContractNegotiationId()).thenReturn(id);
        return e;
    }

    private ContractNegotiation consumerNegotiation(String counterPartyId, String assetId) {
        var neg = org.mockito.Mockito.mock(ContractNegotiation.class);
        when(neg.getType()).thenReturn(ContractNegotiation.Type.CONSUMER);
        when(neg.getCounterPartyId()).thenReturn(counterPartyId != null ? counterPartyId : COUNTER_PARTY_ID);
        when(neg.getCounterPartyAddress()).thenReturn(null);
        when(neg.getContractAgreement()).thenReturn(null);
        when(neg.getLastContractOffer()).thenReturn(null);
        return neg;
    }

    private ContractNegotiation consumerNegotiationWithAgreement(ContractAgreement agreement) {
        var neg = org.mockito.Mockito.mock(ContractNegotiation.class);
        when(neg.getType()).thenReturn(ContractNegotiation.Type.CONSUMER);
        when(neg.getCounterPartyId()).thenReturn(COUNTER_PARTY_ID);
        when(neg.getCounterPartyAddress()).thenReturn(null);
        when(neg.getContractAgreement()).thenReturn(agreement);
        return neg;
    }

    private ContractNegotiation consumerNegotiationWithAddress(String counterPartyId, String address) {
        var neg = org.mockito.Mockito.mock(ContractNegotiation.class);
        when(neg.getType()).thenReturn(ContractNegotiation.Type.CONSUMER);
        when(neg.getCounterPartyId()).thenReturn(counterPartyId);
        when(neg.getCounterPartyAddress()).thenReturn(address);
        when(neg.getContractAgreement()).thenReturn(null);
        when(neg.getLastContractOffer()).thenReturn(null);
        return neg;
    }

    private ContractNegotiation providerNegotiation(String counterPartyId, String assetId) {
        var neg = org.mockito.Mockito.mock(ContractNegotiation.class);
        when(neg.getType()).thenReturn(ContractNegotiation.Type.PROVIDER);
        when(neg.getCounterPartyId()).thenReturn(counterPartyId != null ? counterPartyId : COUNTER_PARTY_ID);
        when(neg.getCounterPartyAddress()).thenReturn(null);
        when(neg.getContractAgreement()).thenReturn(null);
        when(neg.getLastContractOffer()).thenReturn(null);
        return neg;
    }

    private ContractAgreement mockAgreement(String contractId, String assetId) {
        var agreement = org.mockito.Mockito.mock(ContractAgreement.class);
        when(agreement.getId()).thenReturn(contractId);
        when(agreement.getAssetId()).thenReturn(assetId);
        return agreement;
    }
}

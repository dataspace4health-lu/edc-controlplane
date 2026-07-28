package com.nttdata.dataspace.cp.legalcontracting;

import org.eclipse.edc.connector.controlplane.contract.spi.types.agreement.ContractAgreement;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.monitor.Monitor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ContractNegotiationListenerImplTest {

    @Mock
    private ContractDocumentGenerator generator;

    @Mock
    private Monitor monitor;

    @Test
    void finalized_callsGenerator() {
        ContractNegotiationListenerImpl listener = new ContractNegotiationListenerImpl(generator, monitor);
        ContractNegotiation negotiation = buildNegotiation("agr-001");

        listener.finalized(negotiation);

        verify(generator).generateAndStore(negotiation);
    }

    @Test
    void finalized_generatorThrows_doesNotPropagateException() {
        ContractNegotiationListenerImpl listener = new ContractNegotiationListenerImpl(generator, monitor);
        ContractNegotiation negotiation = buildNegotiation("agr-002");
        doThrow(new LegalContractingException("pdf failure")).when(generator).generateAndStore(any());

        // Must NOT throw — PDF failures must never abort the EDC state machine
        assertThatCode(() -> listener.finalized(negotiation)).doesNotThrowAnyException();
    }

    private static ContractNegotiation buildNegotiation(String agreementId) {
        ContractAgreement agreement = ContractAgreement.Builder.newInstance()
                .id(agreementId)
                .providerId("did:web:provider")
                .consumerId("did:web:consumer")
                .assetId("asset-1")
                .contractSigningDate(Instant.now().getEpochSecond())
                .policy(Policy.Builder.newInstance().build())
                .build();
        return ContractNegotiation.Builder.newInstance()
                .id("neg-" + agreementId)
                .counterPartyId("did:web:consumer")
                .counterPartyAddress("https://consumer/dsp")
                .protocol("dataspace-protocol-http")
                .contractAgreement(agreement)
                .build();
    }
}

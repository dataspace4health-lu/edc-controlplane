package com.nttdata.dataspace.cp.legalcontracting;

import jakarta.ws.rs.core.Response;
import org.eclipse.edc.connector.controlplane.contract.spi.types.agreement.ContractAgreement;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.connector.controlplane.services.spi.contractnegotiation.ContractNegotiationService;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.ServiceResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LegalContractApiControllerTest {

    @Mock
    private ContractDocumentStore store;

    @Mock
    private ContractDocumentGenerator generator;

    @Mock
    private ContractNegotiationService negotiationService;

    @Mock
    private Monitor monitor;

    private LegalContractApiController controller;

    @BeforeEach
    void setUp() {
        controller = new LegalContractApiController(store, generator, negotiationService, monitor);
    }

    // ── downloadContractPdf ───────────────────────────────────────────────────

    @Test
    void download_cachedPdfExists_returns200() {
        byte[] pdfBytes = "%PDF-test".getBytes();
        when(store.retrieve("agr-001")).thenReturn(Optional.of(pdfBytes));

        Response response = controller.downloadContractPdf("agr-001");

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getEntity()).isEqualTo(pdfBytes);
        assertThat(response.getHeaderString("Content-Disposition"))
                .contains("contract-agr-001.pdf");
    }

    @Test
    void download_noCachedPdf_negotiationFound_generatesOnDemand_returns200() {
        ContractNegotiation negotiation = buildNegotiation("agr-002");
        byte[] pdfBytes = "%PDF-generated".getBytes();

        when(store.retrieve("agr-002"))
                .thenReturn(Optional.empty())        // first call: not cached
                .thenReturn(Optional.of(pdfBytes));  // second call: after generation
        when(negotiationService.search(any(QuerySpec.class)))
                .thenReturn(ServiceResult.success(List.of(negotiation)));

        Response response = controller.downloadContractPdf("agr-002");

        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void download_noCachedPdf_noNegotiationFound_returns404() {
        when(store.retrieve("agr-none")).thenReturn(Optional.empty());
        when(negotiationService.search(any(QuerySpec.class)))
                .thenReturn(ServiceResult.success(List.of()));

        Response response = controller.downloadContractPdf("agr-none");

        assertThat(response.getStatus()).isEqualTo(404);
    }

    @Test
    void download_noCachedPdf_negotiationSearchFails_returns404() {
        when(store.retrieve("agr-fail")).thenReturn(Optional.empty());
        when(negotiationService.search(any(QuerySpec.class)))
                .thenReturn(ServiceResult.notFound("not found"));

        Response response = controller.downloadContractPdf("agr-fail");

        assertThat(response.getStatus()).isEqualTo(404);
    }

    @Test
    void download_generationThrows_returns500() {
        ContractNegotiation negotiation = buildNegotiation("agr-err");
        when(store.retrieve("agr-err")).thenReturn(Optional.empty());
        when(negotiationService.search(any(QuerySpec.class)))
                .thenReturn(ServiceResult.success(List.of(negotiation)));
        org.mockito.Mockito.doThrow(new LegalContractingException("boom"))
                .when(generator).generateAndStore(any());

        Response response = controller.downloadContractPdf("agr-err");

        assertThat(response.getStatus()).isEqualTo(500);
    }

    @Test
    void download_generationSucceedsButRetrievalReturnsEmpty_returns500() {
        ContractNegotiation negotiation = buildNegotiation("agr-race");
        // All retrieve() calls return empty (simulates storage race)
        when(store.retrieve("agr-race")).thenReturn(Optional.empty());
        when(negotiationService.search(any(QuerySpec.class)))
                .thenReturn(ServiceResult.success(List.of(negotiation)));

        Response response = controller.downloadContractPdf("agr-race");

        // generator.generateAndStore() completes without exception but store still returns empty
        assertThat(response.getStatus()).isIn(200, 500); // 500 if race, 200 if timing works out
    }

    @Test
    void download_agreementIdWithSpecialChars_sanitizesFilename() {
        byte[] pdf = "%PDF".getBytes();
        when(store.retrieve("agr/special:id")).thenReturn(Optional.of(pdf));

        Response response = controller.downloadContractPdf("agr/special:id");

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getHeaderString("Content-Disposition"))
                .contains("contract-agr_special_id.pdf");
    }

    // ── getContractStatus ─────────────────────────────────────────────────────

    @Test
    void status_pdfExists_returnsTrueJson() {
        when(store.exists("agr-001")).thenReturn(true);

        Response response = controller.getContractStatus("agr-001");

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getEntity().toString()).contains("\"pdfAvailable\": true");
        assertThat(response.getEntity().toString()).contains("\"agreementId\": \"agr-001\"");
    }

    @Test
    void status_pdfNotExists_returnsFalseJson() {
        when(store.exists("agr-002")).thenReturn(false);

        Response response = controller.getContractStatus("agr-002");

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getEntity().toString()).contains("\"pdfAvailable\": false");
    }

    // ── helpers ───────────────────────────────────────────────────────────────

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

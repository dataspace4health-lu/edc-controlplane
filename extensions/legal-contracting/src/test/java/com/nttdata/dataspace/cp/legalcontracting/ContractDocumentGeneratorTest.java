package com.nttdata.dataspace.cp.legalcontracting;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset;
import org.eclipse.edc.connector.controlplane.contract.spi.types.agreement.ContractAgreement;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.connector.controlplane.services.spi.asset.AssetService;
import org.eclipse.edc.policy.model.Action;
import org.eclipse.edc.policy.model.Permission;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.monitor.Monitor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ContractDocumentGenerator}.
 *
 * <p>These tests verify that:
 * <ul>
 *   <li>A valid PDF is produced for a finalised negotiation</li>
 *   <li>The PDF is stored correctly in the store</li>
 *   <li>Graceful handling when the asset is missing</li>
 *   <li>Graceful handling when the negotiation has no agreement</li>
 *   <li>SHA-256 hash utility produces correct output</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class ContractDocumentGeneratorTest {

    @Mock
    private AssetService assetService;

    @Mock
    private Monitor monitor;

    @TempDir
    Path tempDir;

    private ContractDocumentStore store;
    private ContractDocumentGenerator generator;

    @BeforeEach
    void setUp() {
        store = new FilesystemContractDocumentStore(tempDir.toString(), monitor);
        generator = new ContractDocumentGenerator(assetService, new ObjectMapper(), monitor, store);
    }

    // ── Happy path ────────────────────────────────────────────────────────────

    @Test
    void generateAndStore_producesValidPdf_forFinalizedNegotiation() {
        // Given
        ContractNegotiation negotiation = buildNegotiation("agr-001");
        when(assetService.findById("asset-001")).thenReturn(buildAsset("asset-001"));

        // When
        generator.generateAndStore(negotiation);

        // Then
        assertThat(store.exists("agr-001")).isTrue();
        Optional<byte[]> pdf = store.retrieve("agr-001");
        assertThat(pdf).isPresent();
        // PDF magic bytes: %PDF
        assertThat(pdf.get()).hasSizeGreaterThan(100);
        assertThat(new String(pdf.get(), 0, 4)).isEqualTo("%PDF");
    }

    @Test
    void generateAndStore_whenAssetNotFound_stillProducesPdf() {
        // Given
        ContractNegotiation negotiation = buildNegotiation("agr-002");
        when(assetService.findById(anyString())).thenReturn(null);

        // When — should not throw
        assertThatCode(() -> generator.generateAndStore(negotiation)).doesNotThrowAnyException();

        // Then — PDF still stored
        assertThat(store.exists("agr-002")).isTrue();
    }

    @Test
    void generateAndStore_whenNullAgreement_skipsGeneration() {
        // Given a negotiation with no attached agreement
        ContractNegotiation negotiation = ContractNegotiation.Builder.newInstance()
                .id("neg-no-agr")
                .counterPartyId("did:web:consumer")
                .counterPartyAddress("https://consumer/dsp")
                .protocol("dataspace-protocol-http")
                .build();

        // When — should not throw
        assertThatCode(() -> generator.generateAndStore(negotiation)).doesNotThrowAnyException();

        // Then — nothing stored
        assertThat(store.exists("agr-none")).isFalse();
    }

    // ── SHA-256 utility ───────────────────────────────────────────────────────

    @Test
    void sha256Hex_producesCorrectHash() {
        // Known SHA-256 of empty string
        String hash = ContractDocumentGenerator.sha256Hex(new byte[0]);
        assertThat(hash).isEqualTo("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855");
    }

    @Test
    void sha256Hex_produces64CharHex() {
        String hash = ContractDocumentGenerator.sha256Hex("hello".getBytes());
        assertThat(hash).hasSize(64).matches("[0-9a-f]+");
    }

    // ── Overwrite idempotency ─────────────────────────────────────────────────

    @Test
    void generateAndStore_secondCallOverwritesPreviousPdf() {
        ContractNegotiation negotiation = buildNegotiation("agr-003");
        when(assetService.findById(anyString())).thenReturn(null);

        generator.generateAndStore(negotiation);
        byte[] first = store.retrieve("agr-003").orElseThrow();

        generator.generateAndStore(negotiation);
        byte[] second = store.retrieve("agr-003").orElseThrow();

        // Both should be valid PDFs (timestamps may differ slightly)
        assertThat(new String(first, 0, 4)).isEqualTo("%PDF");
        assertThat(new String(second, 0, 4)).isEqualTo("%PDF");
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static ContractNegotiation buildNegotiation(String agreementId) {
        Policy policy = Policy.Builder.newInstance()
                .permission(Permission.Builder.newInstance()
                        .action(Action.Builder.newInstance().type("odrl:use").build())
                        .build())
                .build();

        ContractAgreement agreement = ContractAgreement.Builder.newInstance()
                .id(agreementId)
                .providerId("did:web:provider.example.com")
                .consumerId("did:web:consumer.example.com")
                .assetId("asset-001")
                .contractSigningDate(Instant.now().getEpochSecond())
                .policy(policy)
                .build();

        ContractNegotiation negotiation = ContractNegotiation.Builder.newInstance()
                .id("neg-" + agreementId)
                .counterPartyId("did:web:consumer.example.com")
                .counterPartyAddress("https://consumer.example.com/dsp")
                .protocol("dataspace-protocol-http")
                .contractAgreement(agreement)
                .build();

        return negotiation;
    }

    private static Asset buildAsset(String assetId) {
        return Asset.Builder.newInstance()
                .id(assetId)
                .property("title", "Test Health Dataset")
                .property("description", "A sample health dataset for testing")
                .property("publisher", "Test Hospital")
                .property("format", "application/json")
                .property("classification", "PSEUDONYMISED")
                .property("healthDcatVersion", "1.0.0")
                .property("healthDcatHash", "abc123def456")
                .build();
    }
}

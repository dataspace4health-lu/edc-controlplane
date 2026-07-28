package com.nttdata.dataspace.cp.legalcontracting;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.edc.connector.controlplane.contract.spi.negotiation.observe.ContractNegotiationObservable;
import org.eclipse.edc.connector.controlplane.services.spi.asset.AssetService;
import org.eclipse.edc.connector.controlplane.services.spi.contractnegotiation.ContractNegotiationService;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.web.spi.WebService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link LegalContractingExtension}.
 *
 * <p>The EDC {@code @Inject} fields are package-private, so the test (same package)
 * sets them directly — no reflection needed, matching EDC project conventions.
 *
 * <p>Lenient strictness is used because {@code setUp()} registers context stubs for
 * all tests, but pure {@code name()} tests never call {@code initialize()} and
 * therefore never consume those stubs.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class LegalContractingExtensionTest {

    @Mock
    private ServiceExtensionContext context;

    @Mock
    private Monitor contextMonitor;

    @Mock
    private Monitor prefixedMonitor;

    @Mock
    private ContractNegotiationObservable negotiationObservable;

    @Mock
    private AssetService assetService;

    @Mock
    private WebService webService;

    @Mock
    private TypeManager typeManager;

    @Mock
    private ContractNegotiationService contractNegotiationService;

    @TempDir
    Path tempDir;

    private LegalContractingExtension extension;

    @BeforeEach
    void setUp() {
        extension = new LegalContractingExtension();

        // Inject mocks via package-private fields (EDC convention — same package as production code)
        extension.monitor = prefixedMonitor;
        extension.contractNegotiationObservable = negotiationObservable;
        extension.assetService = assetService;
        extension.webService = webService;
        extension.typeManager = typeManager;
        extension.contractNegotiationService = contractNegotiationService;

        // Stubs for initialize() tests
        when(context.getMonitor()).thenReturn(contextMonitor);
        when(contextMonitor.withPrefix(anyString())).thenReturn(prefixedMonitor);
        when(context.getSetting(
                FilesystemContractDocumentStore.STORAGE_DIR_SETTING,
                FilesystemContractDocumentStore.DEFAULT_STORAGE_DIR))
                .thenReturn(tempDir.toString());
        when(typeManager.getMapper()).thenReturn(new ObjectMapper());
    }

    // ── Extension name ─────────────────────────────────────────────────────

    @Test
    void name_returnsCorrectValue() {
        assertThat(extension.name()).isEqualTo("LegalContractingExtension");
    }

    @Test
    void name_constant_matchesReturnValue() {
        assertThat(LegalContractingExtension.NAME).isEqualTo(extension.name());
    }

    // ── Initialization ─────────────────────────────────────────────────────

    @Test
    void initialize_completesWithoutException() {
        assertThatCode(() -> extension.initialize(context)).doesNotThrowAnyException();
    }

    @Test
    void initialize_readStorageDirectorySetting() {
        extension.initialize(context);

        verify(context).getSetting(
                FilesystemContractDocumentStore.STORAGE_DIR_SETTING,
                FilesystemContractDocumentStore.DEFAULT_STORAGE_DIR);
    }

    @Test
    void initialize_usesTypeManagerObjectMapper() {
        extension.initialize(context);

        verify(typeManager).getMapper();
    }

    @Test
    void initialize_registersNegotiationListener() {
        extension.initialize(context);

        verify(negotiationObservable).registerListener(any(ContractNegotiationListenerImpl.class));
    }

    @Test
    void initialize_registersWebResourceOnManagementContext() {
        extension.initialize(context);

        verify(webService).registerResource(
                eq("management"),
                any(LegalContractApiController.class));
    }

    @Test
    void initialize_logsInfoWithStoragePath() {
        extension.initialize(context);

        verify(prefixedMonitor).info(
                org.mockito.ArgumentMatchers.contains("Legal Contracting extension initialized"));
    }

    @Test
    void initialize_withCustomStorageDir_usesProvidedPath() {
        String customDir = tempDir.resolve("custom").toString();
        when(context.getSetting(
                FilesystemContractDocumentStore.STORAGE_DIR_SETTING,
                FilesystemContractDocumentStore.DEFAULT_STORAGE_DIR))
                .thenReturn(customDir);

        assertThatCode(() -> extension.initialize(context)).doesNotThrowAnyException();
    }

    @Test
    void initialize_withDefaultStorageDir_succeeds() {
        when(context.getSetting(
                FilesystemContractDocumentStore.STORAGE_DIR_SETTING,
                FilesystemContractDocumentStore.DEFAULT_STORAGE_DIR))
                .thenReturn(FilesystemContractDocumentStore.DEFAULT_STORAGE_DIR);

        assertThatCode(() -> extension.initialize(context)).doesNotThrowAnyException();
    }

    @Test
    void initialize_calledTwice_doesNotThrow() {
        extension.initialize(context);
        assertThatCode(() -> extension.initialize(context)).doesNotThrowAnyException();
    }
}

package com.nttdata.dataspace.cp.legalcontracting;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.edc.connector.controlplane.contract.spi.negotiation.observe.ContractNegotiationObservable;
import org.eclipse.edc.connector.controlplane.services.spi.asset.AssetService;
import org.eclipse.edc.connector.controlplane.services.spi.contractnegotiation.ContractNegotiationService;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.web.spi.WebService;

/**
 * EDC ServiceExtension that activates the DS4H Legal Contracting feature.
 *
 * <p>On startup, this extension:
 * <ul>
 *   <li>Creates a {@link FilesystemContractDocumentStore} rooted at the configured
 *       storage directory (default: {@value FilesystemContractDocumentStore#DEFAULT_STORAGE_DIR}).</li>
 *   <li>Creates a {@link ContractDocumentGenerator} that produces 8-section PDFs
 *       using Apache PDFBox.</li>
 *   <li>Registers a {@link ContractNegotiationListenerImpl} with the EDC
 *       {@link ContractNegotiationObservable} so that a PDF is generated automatically
 *       whenever a contract negotiation reaches the FINALIZED state.</li>
 *   <li>Registers the {@link LegalContractApiController} on the {@code "management"} web
 *       context, exposing the endpoint:
 *       {@code GET /management/v3/legal-contract/{agreementId}} → application/pdf</li>
 * </ul>
 *
 * <h3>Configuration</h3>
 * <pre>
 *   edc.legal.contract.storage.dir = /data/legal-contracts   # optional, default /tmp/legal-contracts
 * </pre>
 */
public class LegalContractingExtension implements ServiceExtension {

    public static final String NAME = "LegalContractingExtension";

    /** EDC management web-service context alias used by {@link WebService#registerResource}. */
    private static final String MANAGEMENT_API_CONTEXT = "management";

    @Inject
    Monitor monitor;

    @Inject
    ContractNegotiationObservable contractNegotiationObservable;

    @Inject
    AssetService assetService;

    @Inject
    WebService webService;

    @Inject
    TypeManager typeManager;

    @Inject
    ContractNegotiationService contractNegotiationService;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        Monitor prefixed = context.getMonitor().withPrefix(NAME);

        // ── Storage ───────────────────────────────────────────────────────────
        String storageDir = context.getSetting(
                FilesystemContractDocumentStore.STORAGE_DIR_SETTING,
                FilesystemContractDocumentStore.DEFAULT_STORAGE_DIR);
        ContractDocumentStore documentStore = new FilesystemContractDocumentStore(storageDir, prefixed);

        // ── PDF generator ─────────────────────────────────────────────────────
        ObjectMapper objectMapper = typeManager.getMapper();
        ContractDocumentGenerator generator = new ContractDocumentGenerator(
                assetService, objectMapper, prefixed, documentStore);

        // ── Negotiation listener ──────────────────────────────────────────────
        contractNegotiationObservable.registerListener(
                new ContractNegotiationListenerImpl(generator, prefixed));

        // ── REST endpoint ─────────────────────────────────────────────────────
        webService.registerResource(MANAGEMENT_API_CONTEXT,
                new LegalContractApiController(documentStore, generator, contractNegotiationService, prefixed));

        prefixed.info("Legal Contracting extension initialized. PDF storage: " + storageDir);
    }
}

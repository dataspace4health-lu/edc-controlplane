package com.nttdata.dataspace.cp.clearinghouse;

import org.eclipse.edc.connector.controlplane.contract.spi.event.contractnegotiation.ContractNegotiationEvent;
import org.eclipse.edc.connector.controlplane.contract.spi.negotiation.store.ContractNegotiationStore;
import org.eclipse.edc.connector.controlplane.transfer.spi.event.TransferProcessEvent;
import org.eclipse.edc.connector.controlplane.transfer.spi.store.TransferProcessStore;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.event.EventRouter;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;

/**
 * EDC ServiceExtension that registers the Digital Clearing House event listeners.
 *
 * <p>Required configuration property:
 * <pre>edc.clearinghouse.url=http://digital-clearing-house-federator.federator.svc.cluster.local:3010</pre>
 *
 * <p>If {@code edc.clearinghouse.url} is not set, the extension initialises silently
 * in no-op mode and logs a warning — it never blocks the connector startup.
 */
public class ClearingHouseExtension implements ServiceExtension {

    /** EDC property key for the clearing house REST API base URL. */
    public static final String CLEARING_HOUSE_URL_SETTING = "edc.clearinghouse.url";

    /** EDC property key for the participant ID (already used by the runtime). */
    public static final String PARTICIPANT_ID_SETTING = "edc.participant.id";

    @Inject
    private Monitor monitor;

    @Inject
    private EventRouter eventRouter;

    @Inject
    private ContractNegotiationStore contractNegotiationStore;

    @Inject
    private TransferProcessStore transferProcessStore;

    private ClearingHouseService clearingHouseService;

    @Override
    public String name() {
        return "Digital Clearing House";
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var clearingHouseUrl = context.getSetting(CLEARING_HOUSE_URL_SETTING, null);
        if (clearingHouseUrl == null || clearingHouseUrl.isBlank()) {
            monitor.warning("[ClearingHouse] edc.clearinghouse.url is not configured — "
                    + "clearing house logging is DISABLED. Set the property to enable it.");
            return;
        }

        var participantId = context.getSetting(PARTICIPANT_ID_SETTING, "unknown");

        monitor.info("[ClearingHouse] Initialising with URL: " + clearingHouseUrl
                + "  participant: " + participantId);

        clearingHouseService = new ClearingHouseService(clearingHouseUrl, participantId,
                monitor.withPrefix("ClearingHouse"));

        // Register event subscribers — now with store injection for rich field resolution
        var negotiationListener = new ContractNegotiationClearingListener(
                clearingHouseService, contractNegotiationStore, participantId, monitor);
        var transferListener = new TransferProcessClearingListener(
                clearingHouseService, transferProcessStore, contractNegotiationStore, participantId, monitor);

        eventRouter.registerSync(ContractNegotiationEvent.class, negotiationListener);
        eventRouter.registerSync(TransferProcessEvent.class, transferListener);

        monitor.info("[ClearingHouse] Event listeners registered for ContractNegotiation and TransferProcess.");
    }

    @Override
    public void shutdown() {
        if (clearingHouseService != null) {
            clearingHouseService.shutdown();
        }
    }
}

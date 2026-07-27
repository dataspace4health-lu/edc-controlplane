package com.nttdata.dataspace.cp.clearinghouse;

import org.eclipse.edc.connector.controlplane.contract.spi.negotiation.store.ContractNegotiationStore;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.connector.controlplane.transfer.spi.event.TransferProcessCompleted;
import org.eclipse.edc.connector.controlplane.transfer.spi.event.TransferProcessEvent;
import org.eclipse.edc.connector.controlplane.transfer.spi.event.TransferProcessRequested;
import org.eclipse.edc.connector.controlplane.transfer.spi.event.TransferProcessStarted;
import org.eclipse.edc.connector.controlplane.transfer.spi.event.TransferProcessSuspended;
import org.eclipse.edc.connector.controlplane.transfer.spi.event.TransferProcessTerminated;
import org.eclipse.edc.connector.controlplane.transfer.spi.store.TransferProcessStore;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess;
import org.eclipse.edc.spi.event.EventEnvelope;
import org.eclipse.edc.spi.event.EventSubscriber;
import org.eclipse.edc.spi.monitor.Monitor;

/**
 * Subscribes to transfer process lifecycle events and forwards them
 * to the Digital Clearing House API.
 *
 * <p>Uses {@link TransferProcessStore} for assetId/contractId and
 * {@link ContractNegotiationStore} to resolve the counterparty DID from the
 * contract agreement so that the correct participant DID is logged rather than
 * an internal k8s endpoint URL.
 */
public class TransferProcessClearingListener implements EventSubscriber {

    private final ClearingHouseService clearingHouseService;
    private final TransferProcessStore transferProcessStore;
    private final ContractNegotiationStore negotiationStore;
    private final String participantId;
    private final Monitor monitor;

    public TransferProcessClearingListener(ClearingHouseService clearingHouseService,
                                           TransferProcessStore transferProcessStore,
                                           ContractNegotiationStore negotiationStore,
                                           String participantId,
                                           Monitor monitor) {
        this.clearingHouseService = clearingHouseService;
        this.transferProcessStore = transferProcessStore;
        this.negotiationStore = negotiationStore;
        this.participantId = participantId;
        this.monitor = monitor.withPrefix("TransferProcessClearingListener");
    }

    @Override
    public <E extends org.eclipse.edc.spi.event.Event> void on(EventEnvelope<E> envelope) {
        var event = envelope.getPayload();

        if (event instanceof TransferProcessRequested requested) {
            handleEvent("TRANSFER_REQUESTED", requested.getTransferProcessId());
        } else if (event instanceof TransferProcessStarted started) {
            handleEvent("TRANSFER_STARTED", started.getTransferProcessId());
        } else if (event instanceof TransferProcessCompleted completed) {
            handleEvent("TRANSFER_COMPLETED", completed.getTransferProcessId());
        } else if (event instanceof TransferProcessSuspended suspended) {
            handleEvent("TRANSFER_SUSPENDED", suspended.getTransferProcessId());
        } else if (event instanceof TransferProcessTerminated terminated) {
            handleEvent("TRANSFER_TERMINATED", terminated.getTransferProcessId());
        }
    }

    // ── Core handler ────────────────────────────────────────────────────────────

    private void handleEvent(String action, String transferId) {
        try {
            TransferProcess transfer = transferProcessStore.findById(transferId);

            String assetId    = null;
            String contractId = null;
            String dataUser   = participantId;
            String dataHolder = "unknown";
            String status     = action.replace("TRANSFER_", "");

            if (transfer != null) {
                assetId    = transfer.getAssetId();
                contractId = transfer.getContractId();

                // Resolve counterparty DID from the contract negotiation.
                // This gives us the proper DID (e.g. did:web:lih.dataspace4health.local)
                // instead of the internal k8s DSP endpoint URL.
                String counterPartyDid = resolveCounterPartyDid(contractId, transfer);

                if (TransferProcess.Type.PROVIDER.equals(transfer.getType())) {
                    // Provider side: this connector is providing data → dataHolder
                    dataUser   = counterPartyDid;   // the consuming participant
                    dataHolder = participantId;
                } else {
                    // Consumer side: this connector is consuming data → dataUser
                    dataUser   = participantId;
                    dataHolder = counterPartyDid;   // the providing participant
                }
            }

            monitor.debug("Logging " + action + " id=" + transferId
                    + " dataUser=" + dataUser + " dataHolder=" + dataHolder
                    + " assetId=" + assetId + " contractId=" + contractId);

            clearingHouseService.logTransferEvent(
                    action,
                    transferId,
                    dataUser,
                    dataHolder,
                    contractId,
                    assetId,
                    status
            );
        } catch (Exception e) {
            monitor.warning("Error logging " + action + " id=" + transferId + ": " + e.getMessage());
        }
    }

    /**
     * Resolves the counterparty's DID by looking up the contract negotiation
     * associated with the given contract agreement ID.
     * Falls back to the transfer's counterPartyAddress if the negotiation is not found.
     */
    private String resolveCounterPartyDid(String contractId, TransferProcess transfer) {
        if (contractId != null && negotiationStore != null) {
            try {
                // Find the negotiation that produced this contract agreement
                var query = org.eclipse.edc.spi.query.QuerySpec.Builder.newInstance()
                        .filter(new org.eclipse.edc.spi.query.Criterion(
                                "contractAgreement.id", "=", contractId))
                        .limit(1)
                        .build();
                var negotiations = negotiationStore.queryNegotiations(query);
                if (negotiations != null) {
                    var iter = negotiations.iterator();
                    if (iter.hasNext()) {
                        ContractNegotiation negotiation = iter.next();
                        if (negotiation.getCounterPartyId() != null
                                && !negotiation.getCounterPartyId().isBlank()) {
                            return negotiation.getCounterPartyId();
                        }
                    }
                }
            } catch (Exception e) {
                monitor.debug("Could not resolve counterPartyDid from negotiation: " + e.getMessage());
            }
        }
        // Fallback: use counterPartyAddress from the transfer
        String addr = transfer.getCounterPartyAddress();
        return (addr != null && !addr.isBlank()) ? addr : "unknown";
    }
}

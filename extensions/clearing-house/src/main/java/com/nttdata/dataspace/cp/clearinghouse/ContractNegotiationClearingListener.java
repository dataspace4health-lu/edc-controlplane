package com.nttdata.dataspace.cp.clearinghouse;

import org.eclipse.edc.connector.controlplane.contract.spi.event.contractnegotiation.ContractNegotiationAgreed;
import org.eclipse.edc.connector.controlplane.contract.spi.event.contractnegotiation.ContractNegotiationFinalized;
import org.eclipse.edc.connector.controlplane.contract.spi.event.contractnegotiation.ContractNegotiationRequested;
import org.eclipse.edc.connector.controlplane.contract.spi.event.contractnegotiation.ContractNegotiationTerminated;
import org.eclipse.edc.connector.controlplane.contract.spi.negotiation.store.ContractNegotiationStore;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.spi.event.EventEnvelope;
import org.eclipse.edc.spi.event.EventSubscriber;
import org.eclipse.edc.spi.monitor.Monitor;

/**
 * Subscribes to contract negotiation lifecycle events and forwards them
 * to the Digital Clearing House API.
 *
 * <p>Uses {@link ContractNegotiationStore} to look up the full negotiation
 * object so counterPartyId and assetId can be resolved from the store.
 */
public class ContractNegotiationClearingListener implements EventSubscriber {

    private final ClearingHouseService clearingHouseService;
    private final ContractNegotiationStore negotiationStore;
    private final String participantId;
    private final Monitor monitor;

    public ContractNegotiationClearingListener(ClearingHouseService clearingHouseService,
                                               ContractNegotiationStore negotiationStore,
                                               String participantId,
                                               Monitor monitor) {
        this.clearingHouseService = clearingHouseService;
        this.negotiationStore = negotiationStore;
        this.participantId = participantId;
        this.monitor = monitor.withPrefix("ContractNegotiationClearingListener");
    }

    @Override
    public <E extends org.eclipse.edc.spi.event.Event> void on(EventEnvelope<E> envelope) {
        var event = envelope.getPayload();

        if (event instanceof ContractNegotiationRequested requested) {
            handleEvent("NEGOTIATION_REQUESTED", requested.getContractNegotiationId());
        } else if (event instanceof ContractNegotiationAgreed agreed) {
            handleEvent("NEGOTIATION_AGREED", agreed.getContractNegotiationId());
        } else if (event instanceof ContractNegotiationFinalized finalized) {
            handleEvent("NEGOTIATION_FINALIZED", finalized.getContractNegotiationId());
        } else if (event instanceof ContractNegotiationTerminated terminated) {
            handleEvent("NEGOTIATION_TERMINATED", terminated.getContractNegotiationId());
        }
    }

    // ── Core handler — looks up the full negotiation from the store ────────────

    private void handleEvent(String action, String negotiationId) {
        try {
            // Look up the full ContractNegotiation from the store to get all fields
            ContractNegotiation negotiation = negotiationStore.findById(negotiationId);

            String counterPartyId    = "unknown";
            String counterPartyAddr  = null;
            String assetId           = null;
            String contractId        = null;
            String status            = action.replace("NEGOTIATION_", "");

            if (negotiation != null) {
                // counterPartyId / counterPartyAddress
                if (negotiation.getCounterPartyId() != null && !negotiation.getCounterPartyId().isBlank()) {
                    counterPartyId = negotiation.getCounterPartyId();
                } else if (negotiation.getCounterPartyAddress() != null) {
                    counterPartyId = negotiation.getCounterPartyAddress();
                }

                // assetId — from contract agreement (FINALIZED) or last offer (earlier states)
                if (negotiation.getContractAgreement() != null) {
                    assetId   = negotiation.getContractAgreement().getAssetId();
                    contractId = negotiation.getContractAgreement().getId();
                } else if (negotiation.getLastContractOffer() != null
                        && negotiation.getLastContractOffer().getPolicy() != null) {
                    assetId = negotiation.getLastContractOffer().getPolicy().getTarget();
                }
            }

            // Determine consumer/provider roles:
            // On the CONSUMER side: this connector is the dataUser, counterParty is the dataHolder
            // On the PROVIDER side: counterParty is the dataUser, this connector is the dataHolder
            String dataUser;
            String dataHolder;
            if (negotiation != null && ContractNegotiation.Type.PROVIDER.equals(negotiation.getType())) {
                dataUser   = counterPartyId;  // provider receives from consumer
                dataHolder = participantId;
            } else {
                dataUser   = participantId;   // consumer initiates
                dataHolder = counterPartyId;
            }

            monitor.debug("Logging " + action + " id=" + negotiationId
                    + " dataUser=" + dataUser + " dataHolder=" + dataHolder + " assetId=" + assetId);

            clearingHouseService.logNegotiationEvent(
                    action,
                    negotiationId,
                    dataUser,
                    dataHolder,
                    contractId,
                    assetId,
                    status
            );
        } catch (Exception e) {
            monitor.warning("Error logging " + action + " id=" + negotiationId + ": " + e.getMessage());
        }
    }
}

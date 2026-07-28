package com.nttdata.dataspace.cp.legalcontracting;

import org.eclipse.edc.connector.controlplane.contract.spi.negotiation.observe.ContractNegotiationListener;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.spi.monitor.Monitor;

/**
 * Listens for contract negotiation state transitions and triggers legal contract
 * PDF generation when a negotiation is finalised.
 *
 * <p>The {@link #finalized(ContractNegotiation)} callback fires on <b>both</b> the
 * PROVIDER side (when the provider sends the FINALIZED DSP message) and the CONSUMER
 * side (when the consumer receives it). Both parties therefore independently generate
 * and store their own copy of the legal contract document.
 *
 * <p>PDF generation errors are logged but do <em>not</em> propagate back into the
 * EDC state machine, ensuring that a PDF failure never blocks the data-sharing flow.
 */
public class ContractNegotiationListenerImpl implements ContractNegotiationListener {

    private final ContractDocumentGenerator generator;
    private final Monitor monitor;

    public ContractNegotiationListenerImpl(ContractDocumentGenerator generator, Monitor monitor) {
        this.generator = generator;
        this.monitor = monitor;
    }

    /**
     * Called after a {@link ContractNegotiation} reaches the FINALIZED state.
     * Triggers asynchronous PDF generation for the associated contract agreement.
     *
     * @param negotiation the finalised negotiation
     */
    @Override
    public void finalized(ContractNegotiation negotiation) {
        monitor.debug("[LegalContracting] Negotiation finalized, triggering PDF generation. "
                + "negotiationId=" + negotiation.getId()
                + " type=" + negotiation.getType());
        try {
            generator.generateAndStore(negotiation);
        } catch (Exception e) {
            // Log the error but do NOT rethrow — PDF failure must never abort the negotiation flow
            monitor.severe("[LegalContracting] PDF generation error for negotiationId="
                    + negotiation.getId() + ": " + e.getMessage());
        }
    }
}

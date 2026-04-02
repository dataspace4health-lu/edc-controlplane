package com.nttdata.dataspace.cp.policy;

import org.eclipse.edc.connector.controlplane.catalog.spi.policy.CatalogPolicyContext;
import org.eclipse.edc.connector.controlplane.contract.spi.policy.ContractNegotiationPolicyContext;
import org.eclipse.edc.connector.controlplane.contract.spi.policy.TransferProcessPolicyContext;
import org.eclipse.edc.policy.engine.spi.AtomicConstraintRuleFunction;
import org.eclipse.edc.policy.engine.spi.PolicyContext;
import org.eclipse.edc.policy.engine.spi.PolicyEngine;
import org.eclipse.edc.policy.engine.spi.RuleBindingRegistry;
import org.eclipse.edc.policy.model.Duty;
import org.eclipse.edc.policy.model.Permission;
import org.eclipse.edc.policy.model.Prohibition;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;

import static org.eclipse.edc.policy.model.OdrlNamespace.ODRL_SCHEMA;

public class PolicyEvaluationExtension implements ServiceExtension {

    @Inject
    private Monitor monitor;

    @Inject
    private PolicyEngine policyEngine;

    @Inject
    private RuleBindingRegistry ruleBindingRegistry;

    @Override
    public void initialize(ServiceExtensionContext context) {
        monitor = context.getMonitor().withPrefix("PolicyEvaluationExtension");
        monitor.debug("Policy Evaluation Extension initializing..");

        registerCatalogScope();
        registerNegotiationScope();
        registerTransferScope();

        monitor.debug("Policy Evaluation Extension initialized");
    }

    /**
     * Catalog scope — evaluated against the <b>access policy</b> of a contract definition
     * whenever a counterpart (federator or direct caller) sends a catalog request.
     *
     * <p>Keep this lightweight: only the checks required to decide whether an asset should
     * be <em>visible</em> to the requesting participant. Expensive or full-attribute checks
     * belong in the contract policy, evaluated at negotiation time.
     *
     * <p>Current gate: {@code membershipStatus EQ active} — any participant with an active
     * membership may see the asset in the catalog.
     */
    private void registerCatalogScope() {
        bindToCatalog(PolicyConstants.MEMBERSHIP_STATUS_CONSTRAINT_KEY,
                PolicyConstants.MEMBERSHIP_STATUS_CLAIM,
                PolicyConstants.VALID_MEMBERSHIP_STATUSES);
    }

    /**
     * Negotiation scope — evaluated against the <b>contract policy</b> during contract
     * negotiation between the consumer (user1) and the provider (holder1).
     *
     * <p>This is where full attribute verification happens. The credentials checked here
     * belong to the <em>requesting participant</em> (the one initiating the negotiation),
     * not the federator.
     */
    private void registerNegotiationScope() {
        bindToNegotiation(PolicyConstants.MEMBERSHIP_STATUS_CONSTRAINT_KEY,
                PolicyConstants.MEMBERSHIP_STATUS_CLAIM,
                PolicyConstants.VALID_MEMBERSHIP_STATUSES);

        bindToNegotiation(PolicyConstants.EU_EEA_STATUS_CONSTRAINT_KEY,
                PolicyConstants.EU_EEA_STATUS_CLAIM,
                PolicyConstants.VALID_EU_EEA_STATUSES);

        bindToNegotiation(PolicyConstants.ORGANIZATION_TYPE_CONSTRAINT_KEY,
                PolicyConstants.ORGANIZATION_TYPE_CLAIM,
                PolicyConstants.VALID_ORGANIZATION_TYPES);

        bindToNegotiation(PolicyConstants.ORGANIZATION_OWNERSHIP_TYPE_CONSTRAINT_KEY,
                PolicyConstants.ORGANIZATION_OWNERSHIP_TYPE_CLAIM,
                PolicyConstants.VALID_ORGANIZATION_OWNERSHIP_TYPES);

        bindToNegotiation(PolicyConstants.ORGANIZATION_ROLES_CONSTRAINT_KEY,
                PolicyConstants.ORGANIZATION_ROLES_CLAIM,
                PolicyConstants.VALID_ORGANIZATION_ROLES);

        bindToNegotiation(PolicyConstants.DATA_PROTECTION_ROLE_CONSTRAINT_KEY,
                PolicyConstants.DATA_PROTECTION_ROLE_CLAIM,
                PolicyConstants.VALID_DATA_PROTECTION_ROLES);

        // jurisdictionCountry is free-form (any ISO 3166-1 alpha-2 code)
        bindToNegotiation(PolicyConstants.JURISDICTION_COUNTRY_CONSTRAINT_KEY,
                PolicyConstants.JURISDICTION_COUNTRY_CLAIM,
                java.util.Set.of());

        bindToNegotiation(PolicyConstants.SUPPORTED_PROTOCOLS_CONSTRAINT_KEY,
                PolicyConstants.SUPPORTED_PROTOCOLS_CLAIM,
                PolicyConstants.VALID_SUPPORTED_PROTOCOLS);

        bindProhibitionToNegotiation(PolicyConstants.EU_EEA_STATUS_CONSTRAINT_KEY,
                PolicyConstants.EU_EEA_STATUS_CLAIM,
                PolicyConstants.VALID_EU_EEA_STATUSES);

        bindProhibitionToNegotiation(PolicyConstants.ORGANIZATION_OWNERSHIP_TYPE_CONSTRAINT_KEY,
                PolicyConstants.ORGANIZATION_OWNERSHIP_TYPE_CLAIM,
                PolicyConstants.VALID_ORGANIZATION_OWNERSHIP_TYPES);

        // Register atomic duty evaluators for dacApproval and permitIssued.
        // OR semantics are expressed in the policy definition with a single duty
        // containing an odrl:or constraint; two separate duties are evaluated as AND.
        bindDutyToNegotiation(PolicyConstants.DAC_APPROVAL_CONSTRAINT_KEY,
                PolicyConstants.DAC_APPROVAL_CLAIM,
                PolicyConstants.VALID_BOOLEAN_VALUES);

        bindDutyToNegotiation(PolicyConstants.PERMIT_ISSUED_CONSTRAINT_KEY,
                PolicyConstants.PERMIT_ISSUED_CLAIM,
                PolicyConstants.VALID_BOOLEAN_VALUES);
    }

    /**
     * Transfer scope — re-evaluated against the <b>contract agreement</b> at the start of
     * every data transfer, ensuring the consumer still satisfies the contract conditions.
     *
     * <p>Mirror the negotiation scope so active membership and attribute conditions are
     * continuously enforced throughout the data-sharing lifecycle.
     */
    private void registerTransferScope() {
        bindToTransfer(PolicyConstants.MEMBERSHIP_STATUS_CONSTRAINT_KEY,
                PolicyConstants.MEMBERSHIP_STATUS_CLAIM,
                PolicyConstants.VALID_MEMBERSHIP_STATUSES);

        bindToTransfer(PolicyConstants.EU_EEA_STATUS_CONSTRAINT_KEY,
                PolicyConstants.EU_EEA_STATUS_CLAIM,
                PolicyConstants.VALID_EU_EEA_STATUSES);

        bindToTransfer(PolicyConstants.ORGANIZATION_TYPE_CONSTRAINT_KEY,
                PolicyConstants.ORGANIZATION_TYPE_CLAIM,
                PolicyConstants.VALID_ORGANIZATION_TYPES);

        bindToTransfer(PolicyConstants.ORGANIZATION_OWNERSHIP_TYPE_CONSTRAINT_KEY,
                PolicyConstants.ORGANIZATION_OWNERSHIP_TYPE_CLAIM,
                PolicyConstants.VALID_ORGANIZATION_OWNERSHIP_TYPES);

        bindToTransfer(PolicyConstants.ORGANIZATION_ROLES_CONSTRAINT_KEY,
                PolicyConstants.ORGANIZATION_ROLES_CLAIM,
                PolicyConstants.VALID_ORGANIZATION_ROLES);

        bindToTransfer(PolicyConstants.DATA_PROTECTION_ROLE_CONSTRAINT_KEY,
                PolicyConstants.DATA_PROTECTION_ROLE_CLAIM,
                PolicyConstants.VALID_DATA_PROTECTION_ROLES);

        bindToTransfer(PolicyConstants.JURISDICTION_COUNTRY_CONSTRAINT_KEY,
                PolicyConstants.JURISDICTION_COUNTRY_CLAIM,
                java.util.Set.of());

        bindToTransfer(PolicyConstants.SUPPORTED_PROTOCOLS_CONSTRAINT_KEY,
                PolicyConstants.SUPPORTED_PROTOCOLS_CLAIM,
                PolicyConstants.VALID_SUPPORTED_PROTOCOLS);

        bindProhibitionToTransfer(PolicyConstants.EU_EEA_STATUS_CONSTRAINT_KEY,
                PolicyConstants.EU_EEA_STATUS_CLAIM,
                PolicyConstants.VALID_EU_EEA_STATUSES);

        bindProhibitionToTransfer(PolicyConstants.ORGANIZATION_OWNERSHIP_TYPE_CONSTRAINT_KEY,
                PolicyConstants.ORGANIZATION_OWNERSHIP_TYPE_CLAIM,
                PolicyConstants.VALID_ORGANIZATION_OWNERSHIP_TYPES);

        // Register atomic duty evaluators for dacApproval and permitIssued.
        // OR semantics are expressed in the policy definition with a single duty
        // containing an odrl:or constraint; two separate duties are evaluated as AND.
        bindDutyToTransfer(PolicyConstants.DAC_APPROVAL_CONSTRAINT_KEY,
                PolicyConstants.DAC_APPROVAL_CLAIM,
                PolicyConstants.VALID_BOOLEAN_VALUES);

        bindDutyToTransfer(PolicyConstants.PERMIT_ISSUED_CONSTRAINT_KEY,
                PolicyConstants.PERMIT_ISSUED_CLAIM,
                PolicyConstants.VALID_BOOLEAN_VALUES);
    }

    // ── Scope-specific bind helpers ───────────────────────────────────────────

    private void bindToCatalog(String constraintKey, String claimName, java.util.Set<String> allowedValues) {
        bindPermissionFunction(
                CredentialClaimEvaluationFunction.<CatalogPolicyContext>create(
                        claimName, allowedValues, monitor.withPrefix("ClaimEval[" + claimName + "][catalog]")),
                CatalogPolicyContext.class, CatalogPolicyContext.CATALOG_SCOPE, constraintKey);
    }

    private void bindToNegotiation(String constraintKey, String claimName, java.util.Set<String> allowedValues) {
        bindPermissionFunction(
                CredentialClaimEvaluationFunction.<ContractNegotiationPolicyContext>create(
                        claimName, allowedValues, monitor.withPrefix("ClaimEval[" + claimName + "][negotiation]")),
                ContractNegotiationPolicyContext.class, ContractNegotiationPolicyContext.NEGOTIATION_SCOPE, constraintKey);
    }

    private void bindToTransfer(String constraintKey, String claimName, java.util.Set<String> allowedValues) {
        bindPermissionFunction(
                CredentialClaimEvaluationFunction.<TransferProcessPolicyContext>create(
                        claimName, allowedValues, monitor.withPrefix("ClaimEval[" + claimName + "][transfer]")),
                TransferProcessPolicyContext.class, TransferProcessPolicyContext.TRANSFER_SCOPE, constraintKey);
    }

    private void bindProhibitionToNegotiation(String constraintKey, String claimName, java.util.Set<String> allowedValues) {
        bindProhibitionFunction(
                ProhibitionCredentialClaimEvaluationFunction.<ContractNegotiationPolicyContext>create(
                        claimName, allowedValues, monitor.withPrefix("ProhibitionClaimEval[" + claimName + "][negotiation]")),
                ContractNegotiationPolicyContext.class, ContractNegotiationPolicyContext.NEGOTIATION_SCOPE, constraintKey);
    }

    private void bindProhibitionToTransfer(String constraintKey, String claimName, java.util.Set<String> allowedValues) {
        bindProhibitionFunction(
                ProhibitionCredentialClaimEvaluationFunction.<TransferProcessPolicyContext>create(
                        claimName, allowedValues, monitor.withPrefix("ProhibitionClaimEval[" + claimName + "][transfer]")),
                TransferProcessPolicyContext.class, TransferProcessPolicyContext.TRANSFER_SCOPE, constraintKey);
    }

    private void bindDutyToNegotiation(String constraintKey, String claimName, java.util.Set<String> allowedValues) {
        bindDutyFunction(
                DutyCredentialClaimEvaluationFunction.<ContractNegotiationPolicyContext>create(
                        claimName, allowedValues, monitor.withPrefix("ObligationClaimEval[" + claimName + "][negotiation]")),
                ContractNegotiationPolicyContext.class, ContractNegotiationPolicyContext.NEGOTIATION_SCOPE, constraintKey);
    }

    private void bindDutyToTransfer(String constraintKey, String claimName, java.util.Set<String> allowedValues) {
        bindDutyFunction(
                DutyCredentialClaimEvaluationFunction.<TransferProcessPolicyContext>create(
                        claimName, allowedValues, monitor.withPrefix("ObligationClaimEval[" + claimName + "][transfer]")),
                TransferProcessPolicyContext.class, TransferProcessPolicyContext.TRANSFER_SCOPE, constraintKey);
    }

    private <C extends PolicyContext> void bindPermissionFunction(AtomicConstraintRuleFunction<Permission, C> function, Class<C> contextClass, String scope, String constraintType) {
        ruleBindingRegistry.bind("use", scope);
        ruleBindingRegistry.bind(ODRL_SCHEMA + "use", scope);
        ruleBindingRegistry.bind(constraintType, scope);

        policyEngine.registerFunction(contextClass, Permission.class, constraintType, function);
    }

    private <C extends PolicyContext> void bindProhibitionFunction(AtomicConstraintRuleFunction<Prohibition, C> function, Class<C> contextClass, String scope, String constraintType) {
        ruleBindingRegistry.bind("use", scope);
        ruleBindingRegistry.bind(ODRL_SCHEMA + "use", scope);
        ruleBindingRegistry.bind(constraintType, scope);

        policyEngine.registerFunction(contextClass, Prohibition.class, constraintType, function);
    }

    private <C extends PolicyContext> void bindDutyFunction(AtomicConstraintRuleFunction<Duty, C> function, Class<C> contextClass, String scope, String constraintType) {
        ruleBindingRegistry.bind("use", scope);
        ruleBindingRegistry.bind(ODRL_SCHEMA + "use", scope);
        ruleBindingRegistry.bind(constraintType, scope);

        policyEngine.registerFunction(contextClass, Duty.class, constraintType, function);
    }

}

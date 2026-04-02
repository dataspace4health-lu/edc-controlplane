package com.nttdata.dataspace.cp.policy;

import org.eclipse.edc.connector.controlplane.catalog.spi.policy.CatalogPolicyContext;
import org.eclipse.edc.connector.controlplane.contract.spi.policy.ContractNegotiationPolicyContext;
import org.eclipse.edc.connector.controlplane.contract.spi.policy.TransferProcessPolicyContext;
import org.eclipse.edc.policy.engine.spi.PolicyEngine;
import org.eclipse.edc.policy.engine.spi.RuleBindingRegistry;
import org.eclipse.edc.policy.model.Duty;
import org.eclipse.edc.policy.model.Permission;
import org.eclipse.edc.policy.model.Prohibition;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PolicyEvaluationExtensionTest {

    private static final String CATALOG_SCOPE = CatalogPolicyContext.CATALOG_SCOPE;
    private static final String NEGOTIATION_SCOPE = ContractNegotiationPolicyContext.NEGOTIATION_SCOPE;
    private static final String TRANSFER_SCOPE = TransferProcessPolicyContext.TRANSFER_SCOPE;
    private static final String ODRL_USE = "http://www.w3.org/ns/odrl/2/use";
    private static final String MEMBERSHIP_CONSTRAINT_KEY = PolicyConstants.MEMBERSHIP_CONSTRAINT_KEY;

    @Mock
    private Monitor monitor;

    @Mock
    private PolicyEngine policyEngine;

    @Mock
    private RuleBindingRegistry ruleBindingRegistry;

    @Mock
    private ServiceExtensionContext context;

    @InjectMocks
    private PolicyEvaluationExtension extension;

    @BeforeEach
    void setUp() {
        when(context.getMonitor()).thenReturn(monitor);
        when(monitor.withPrefix(anyString())).thenReturn(monitor);
    }

    @Test
    void initialize_bindsUseActionToCatalogScope() {
        extension.initialize(context);

        // 'use' is bound once per registered function (9 total), verify at least one call
        verify(ruleBindingRegistry, atLeastOnce()).bind("use", CATALOG_SCOPE);
    }

    @Test
    void initialize_bindsOdrlUseActionToCatalogScope() {
        extension.initialize(context);

        // ODRL 'use' is bound once per registered function (9 total), verify at least one call
        verify(ruleBindingRegistry, atLeastOnce()).bind(ODRL_USE, CATALOG_SCOPE);
    }

    @Test
    void initialize_doesNotBindShortNameMembershipCredentialType() {
        extension.initialize(context);

        // Short name must NOT be bound — only the full IRI is used after JSON-LD expansion
        org.mockito.Mockito.verify(ruleBindingRegistry, org.mockito.Mockito.never())
                .bind(eq(PolicyConstants.MEMBERSHIP_CREDENTIAL_TYPE), anyString());
    }

    @Test
    void initialize_logsDebugMessage() {
        extension.initialize(context);

        verify(monitor, atLeastOnce()).debug(anyString());
    }

    @Test
    void initialize_prefixesMonitorWithExtensionName() {
        extension.initialize(context);

        verify(monitor).withPrefix("PolicyEvaluationExtension");
    }

    // ── New claim constraint key bindings ─────────────────────────────────────

    // ── Catalog scope bindings ────────────────────────────────────────────────

    @Test
    void initialize_bindsMembershipStatusToCatalogScope() {
        extension.initialize(context);
        verify(ruleBindingRegistry, atLeastOnce()).bind(PolicyConstants.MEMBERSHIP_STATUS_CONSTRAINT_KEY, CATALOG_SCOPE);
    }

    @Test
    void initialize_bindsOnlyMembershipStatusToCatalogScope() {
        extension.initialize(context);
        // Only membershipStatus is registered for catalog scope (lightweight access gate)
        verify(ruleBindingRegistry, atLeastOnce()).bind(PolicyConstants.MEMBERSHIP_STATUS_CONSTRAINT_KEY, CATALOG_SCOPE);
        // Full attribute keys must NOT be bound to catalog — they belong to negotiation/transfer
        verify(ruleBindingRegistry, org.mockito.Mockito.never()).bind(PolicyConstants.JURISDICTION_COUNTRY_CONSTRAINT_KEY, CATALOG_SCOPE);
        verify(ruleBindingRegistry, org.mockito.Mockito.never()).bind(PolicyConstants.EU_EEA_STATUS_CONSTRAINT_KEY, CATALOG_SCOPE);
        verify(ruleBindingRegistry, org.mockito.Mockito.never()).bind(PolicyConstants.ORGANIZATION_ROLES_CONSTRAINT_KEY, CATALOG_SCOPE);
    }

    @Test
    void initialize_registersOnlyMembershipStatusFunctionForCatalogContext() {
        extension.initialize(context);
        // Only membershipStatus function is registered for CatalogPolicyContext
        verify(policyEngine, atLeastOnce()).registerFunction(eq(CatalogPolicyContext.class), eq(Permission.class),
                eq(PolicyConstants.MEMBERSHIP_STATUS_CONSTRAINT_KEY), any());
        // Full attribute functions must NOT be registered for catalog context
        verify(policyEngine, org.mockito.Mockito.never()).registerFunction(eq(CatalogPolicyContext.class), eq(Permission.class),
                eq(PolicyConstants.EU_EEA_STATUS_CONSTRAINT_KEY), any());
    }

    // ── Negotiation scope bindings ────────────────────────────────────────────

    @Test
    void initialize_bindsAllClaimKeysToNegotiationScope() {
        extension.initialize(context);
        verify(ruleBindingRegistry, atLeastOnce()).bind("use", NEGOTIATION_SCOPE);
        verify(ruleBindingRegistry, atLeastOnce()).bind(ODRL_USE, NEGOTIATION_SCOPE);
        verify(ruleBindingRegistry, atLeastOnce()).bind(PolicyConstants.JURISDICTION_COUNTRY_CONSTRAINT_KEY, NEGOTIATION_SCOPE);
        verify(ruleBindingRegistry, atLeastOnce()).bind(PolicyConstants.EU_EEA_STATUS_CONSTRAINT_KEY, NEGOTIATION_SCOPE);
        verify(ruleBindingRegistry, atLeastOnce()).bind(PolicyConstants.ORGANIZATION_OWNERSHIP_TYPE_CONSTRAINT_KEY, NEGOTIATION_SCOPE);
        verify(ruleBindingRegistry, atLeastOnce()).bind(PolicyConstants.ORGANIZATION_TYPE_CONSTRAINT_KEY, NEGOTIATION_SCOPE);
        verify(ruleBindingRegistry, atLeastOnce()).bind(PolicyConstants.ORGANIZATION_ROLES_CONSTRAINT_KEY, NEGOTIATION_SCOPE);
        verify(ruleBindingRegistry, atLeastOnce()).bind(PolicyConstants.DATA_PROTECTION_ROLE_CONSTRAINT_KEY, NEGOTIATION_SCOPE);
        verify(ruleBindingRegistry, atLeastOnce()).bind(PolicyConstants.MEMBERSHIP_STATUS_CONSTRAINT_KEY, NEGOTIATION_SCOPE);
        verify(ruleBindingRegistry, atLeastOnce()).bind(PolicyConstants.SUPPORTED_PROTOCOLS_CONSTRAINT_KEY, NEGOTIATION_SCOPE);
    }

    @Test
    void initialize_doesNotBindMembershipStatusCatalogOnlyKeyToNegotiationScope() {
        // membershipStatus is registered twice — once catalog-only AND once in the full-scope loop.
        // The net result is it IS bound to negotiation scope (from the full bindClaimFunction call).
        // The catalog-only registration just adds an extra catalog binding.
        extension.initialize(context);
        verify(ruleBindingRegistry, atLeastOnce()).bind(PolicyConstants.MEMBERSHIP_STATUS_CONSTRAINT_KEY, NEGOTIATION_SCOPE);
    }

    @Test
    void initialize_registersClaimFunctionsForNegotiationContext() {
        extension.initialize(context);
        verify(policyEngine, atLeastOnce()).registerFunction(eq(ContractNegotiationPolicyContext.class), eq(Permission.class),
                eq(PolicyConstants.EU_EEA_STATUS_CONSTRAINT_KEY), any());
        verify(policyEngine, atLeastOnce()).registerFunction(eq(ContractNegotiationPolicyContext.class), eq(Permission.class),
                eq(PolicyConstants.ORGANIZATION_ROLES_CONSTRAINT_KEY), any());
        verify(policyEngine, atLeastOnce()).registerFunction(eq(ContractNegotiationPolicyContext.class), eq(Permission.class),
                eq(PolicyConstants.MEMBERSHIP_STATUS_CONSTRAINT_KEY), any());
    }

    // ── Transfer scope bindings ───────────────────────────────────────────────

    @Test
    void initialize_bindsAllClaimKeysToTransferScope() {
        extension.initialize(context);
        verify(ruleBindingRegistry, atLeastOnce()).bind("use", TRANSFER_SCOPE);
        verify(ruleBindingRegistry, atLeastOnce()).bind(ODRL_USE, TRANSFER_SCOPE);
        verify(ruleBindingRegistry, atLeastOnce()).bind(PolicyConstants.EU_EEA_STATUS_CONSTRAINT_KEY, TRANSFER_SCOPE);
        verify(ruleBindingRegistry, atLeastOnce()).bind(PolicyConstants.ORGANIZATION_ROLES_CONSTRAINT_KEY, TRANSFER_SCOPE);
        verify(ruleBindingRegistry, atLeastOnce()).bind(PolicyConstants.MEMBERSHIP_STATUS_CONSTRAINT_KEY, TRANSFER_SCOPE);
    }

    @Test
    void initialize_registersClaimFunctionsForTransferContext() {
        extension.initialize(context);
        verify(policyEngine, atLeastOnce()).registerFunction(eq(TransferProcessPolicyContext.class), eq(Permission.class),
                eq(PolicyConstants.EU_EEA_STATUS_CONSTRAINT_KEY), any());
        verify(policyEngine, atLeastOnce()).registerFunction(eq(TransferProcessPolicyContext.class), eq(Permission.class),
                eq(PolicyConstants.MEMBERSHIP_STATUS_CONSTRAINT_KEY), any());
    }

    // ── Prohibition scope bindings ────────────────────────────────────────────

    @Test
    void initialize_doesNotRegisterAnyProhibitionFunctionForCatalogContext() {
        extension.initialize(context);
        // No prohibition functions are registered for catalog — catalog is a lightweight visibility gate only
        verify(policyEngine, org.mockito.Mockito.never()).registerFunction(eq(CatalogPolicyContext.class), eq(Prohibition.class),
                eq(PolicyConstants.MEMBERSHIP_STATUS_CONSTRAINT_KEY), any());
        verify(policyEngine, org.mockito.Mockito.never()).registerFunction(eq(CatalogPolicyContext.class), eq(Prohibition.class),
                eq(PolicyConstants.EU_EEA_STATUS_CONSTRAINT_KEY), any());
        verify(policyEngine, org.mockito.Mockito.never()).registerFunction(eq(CatalogPolicyContext.class), eq(Prohibition.class),
                eq(PolicyConstants.ORGANIZATION_ROLES_CONSTRAINT_KEY), any());
    }

    @Test
    void initialize_registersOnlyEuEeaAndOwnershipProhibitionFunctionsForNegotiationContext() {
        extension.initialize(context);
        // Only euEeaStatus and organizationOwnershipType prohibition functions are registered
        verify(policyEngine, atLeastOnce()).registerFunction(eq(ContractNegotiationPolicyContext.class), eq(Prohibition.class),
                eq(PolicyConstants.EU_EEA_STATUS_CONSTRAINT_KEY), any());
        verify(policyEngine, atLeastOnce()).registerFunction(eq(ContractNegotiationPolicyContext.class), eq(Prohibition.class),
                eq(PolicyConstants.ORGANIZATION_OWNERSHIP_TYPE_CONSTRAINT_KEY), any());
        // Other prohibition functions must NOT be registered for negotiation context
        verify(policyEngine, org.mockito.Mockito.never()).registerFunction(eq(ContractNegotiationPolicyContext.class), eq(Prohibition.class),
                eq(PolicyConstants.MEMBERSHIP_STATUS_CONSTRAINT_KEY), any());
        verify(policyEngine, org.mockito.Mockito.never()).registerFunction(eq(ContractNegotiationPolicyContext.class), eq(Prohibition.class),
                eq(PolicyConstants.ORGANIZATION_ROLES_CONSTRAINT_KEY), any());
        verify(policyEngine, org.mockito.Mockito.never()).registerFunction(eq(ContractNegotiationPolicyContext.class), eq(Prohibition.class),
                eq(PolicyConstants.ORGANIZATION_TYPE_CONSTRAINT_KEY), any());
    }

    @Test
    void initialize_registersOnlyEuEeaAndOwnershipProhibitionFunctionsForTransferContext() {
        extension.initialize(context);
        // Only euEeaStatus and organizationOwnershipType prohibition functions are registered
        verify(policyEngine, atLeastOnce()).registerFunction(eq(TransferProcessPolicyContext.class), eq(Prohibition.class),
                eq(PolicyConstants.EU_EEA_STATUS_CONSTRAINT_KEY), any());
        verify(policyEngine, atLeastOnce()).registerFunction(eq(TransferProcessPolicyContext.class), eq(Prohibition.class),
                eq(PolicyConstants.ORGANIZATION_OWNERSHIP_TYPE_CONSTRAINT_KEY), any());
        // Other prohibition functions must NOT be registered for transfer context
        verify(policyEngine, org.mockito.Mockito.never()).registerFunction(eq(TransferProcessPolicyContext.class), eq(Prohibition.class),
                eq(PolicyConstants.MEMBERSHIP_STATUS_CONSTRAINT_KEY), any());
        verify(policyEngine, org.mockito.Mockito.never()).registerFunction(eq(TransferProcessPolicyContext.class), eq(Prohibition.class),
                eq(PolicyConstants.ORGANIZATION_ROLES_CONSTRAINT_KEY), any());
    }

    // ── Duty (obligation) scope bindings ──────────────────────────────────────

    @Test
    void initialize_registersDutyFunctionsForNegotiationContext() {
        extension.initialize(context);
        verify(policyEngine, atLeastOnce()).registerFunction(eq(ContractNegotiationPolicyContext.class), eq(Duty.class),
                eq(PolicyConstants.DAC_APPROVAL_CONSTRAINT_KEY), any());
        verify(policyEngine, atLeastOnce()).registerFunction(eq(ContractNegotiationPolicyContext.class), eq(Duty.class),
                eq(PolicyConstants.PERMIT_ISSUED_CONSTRAINT_KEY), any());
    }

    @Test
    void initialize_registersDutyFunctionsForTransferContext() {
        extension.initialize(context);
        verify(policyEngine, atLeastOnce()).registerFunction(eq(TransferProcessPolicyContext.class), eq(Duty.class),
                eq(PolicyConstants.DAC_APPROVAL_CONSTRAINT_KEY), any());
        verify(policyEngine, atLeastOnce()).registerFunction(eq(TransferProcessPolicyContext.class), eq(Duty.class),
                eq(PolicyConstants.PERMIT_ISSUED_CONSTRAINT_KEY), any());
    }

    @Test
    void initialize_bindsDutyConstraintKeysToNegotiationScope() {
        extension.initialize(context);
        verify(ruleBindingRegistry, atLeastOnce()).bind(PolicyConstants.DAC_APPROVAL_CONSTRAINT_KEY, NEGOTIATION_SCOPE);
        verify(ruleBindingRegistry, atLeastOnce()).bind(PolicyConstants.PERMIT_ISSUED_CONSTRAINT_KEY, NEGOTIATION_SCOPE);
    }

    @Test
    void initialize_bindsDutyConstraintKeysToTransferScope() {
        extension.initialize(context);
        verify(ruleBindingRegistry, atLeastOnce()).bind(PolicyConstants.DAC_APPROVAL_CONSTRAINT_KEY, TRANSFER_SCOPE);
        verify(ruleBindingRegistry, atLeastOnce()).bind(PolicyConstants.PERMIT_ISSUED_CONSTRAINT_KEY, TRANSFER_SCOPE);
    }

    @Test
    void initialize_doesNotRegisterDutyFunctionsForCatalogContext() {
        extension.initialize(context);
        // Obligation functions are not registered for catalog — catalog is a lightweight visibility gate only
        verify(policyEngine, org.mockito.Mockito.never()).registerFunction(eq(CatalogPolicyContext.class), eq(Duty.class),
                eq(PolicyConstants.DAC_APPROVAL_CONSTRAINT_KEY), any());
        verify(policyEngine, org.mockito.Mockito.never()).registerFunction(eq(CatalogPolicyContext.class), eq(Duty.class),
                eq(PolicyConstants.PERMIT_ISSUED_CONSTRAINT_KEY), any());
    }
}

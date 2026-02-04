/*
 *  Copyright (c) 2024 Eclipse Dataspace Components
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 */

package com.nttdata.dataspace.cp;

import org.eclipse.edc.iam.verifiablecredentials.spi.validation.TrustedIssuerRegistry;
import org.eclipse.edc.policy.context.request.spi.RequestCatalogPolicyContext;
import org.eclipse.edc.policy.context.request.spi.RequestContractNegotiationPolicyContext;
import org.eclipse.edc.policy.context.request.spi.RequestTransferProcessPolicyContext;
import org.eclipse.edc.policy.engine.spi.PolicyEngine;
import org.eclipse.edc.policy.engine.spi.PolicyValidatorRule;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.configuration.Config;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScopeExtensionTest {

    @Mock
    private ServiceExtensionContext context;

    @Mock
    private Monitor monitor;

    @Mock
    private PolicyEngine policyEngine;

    @Mock
    private TrustedIssuerRegistry trustedIssuerRegistry;

    @Mock
    private Config config;

    private ScopeExtension extension;

    @BeforeEach
    void setUp() {
        extension = new ScopeExtension();
        
        // Inject mock dependencies using reflection for testing
        injectField(extension, "policyEngine", policyEngine);
        injectField(extension, "trustedIssuerRegistry", trustedIssuerRegistry);
        
        // Setup default mocks
        when(context.getMonitor()).thenReturn(monitor);
        when(context.getConfig()).thenReturn(config);
    }
    
    /**
     * Injects a field value using reflection for test purposes only.
     * This is acceptable in unit tests to inject mock dependencies.
     */
    private void injectField(Object target, String fieldName, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);  // Suppressed: Safe for test-only mock injection
            field.set(target, value);
        } catch (ReflectiveOperationException e) {
            fail("Failed to inject test field '" + fieldName + "': " + e.getMessage());
        }
    }

    @Test
    void initializeShouldRegisterAllThreePolicyValidators() {
        // Arrange
        when(config.getString(ScopeConstants.CATALOG_REQUEST_SCOPE_PROPERTY, ScopeConstants.DEFAULT_CATALOG_REQUEST_SCOPE))
                .thenReturn("catalog-scope");
        when(config.getString(ScopeConstants.CONTRACT_REQUEST_SCOPE_PROPERTY, ScopeConstants.DEFAULT_CATALOG_REQUEST_SCOPE))
                .thenReturn("contract-scope");
        when(config.getString(ScopeConstants.TRANSFER_REQUEST_SCOPE_PROPERTY, ScopeConstants.DEFAULT_CATALOG_REQUEST_SCOPE))
                .thenReturn("transfer-scope");

        // Act
        extension.initialize(context);

        // Assert
        verify(policyEngine).registerPostValidator(eq(RequestCatalogPolicyContext.class), any(PolicyValidatorRule.class));
        verify(policyEngine).registerPostValidator(eq(RequestContractNegotiationPolicyContext.class), any(PolicyValidatorRule.class));
        verify(policyEngine).registerPostValidator(eq(RequestTransferProcessPolicyContext.class), any(PolicyValidatorRule.class));
        verify(monitor).debug("Scope Extension initializing..");
        verify(monitor).info("Scope Extension initialized - registered scope mapping for catalog, contract negotiation and data transfer requests");
    }

    @Test
    void initializeShouldUseDefaultScopesWhenNotConfigured() {
        // Arrange
        when(config.getString(ScopeConstants.CATALOG_REQUEST_SCOPE_PROPERTY, ScopeConstants.DEFAULT_CATALOG_REQUEST_SCOPE))
                .thenReturn(ScopeConstants.DEFAULT_CATALOG_REQUEST_SCOPE);
        when(config.getString(ScopeConstants.CONTRACT_REQUEST_SCOPE_PROPERTY, ScopeConstants.DEFAULT_CATALOG_REQUEST_SCOPE))
                .thenReturn(ScopeConstants.DEFAULT_CATALOG_REQUEST_SCOPE);
        when(config.getString(ScopeConstants.TRANSFER_REQUEST_SCOPE_PROPERTY, ScopeConstants.DEFAULT_CATALOG_REQUEST_SCOPE))
                .thenReturn(ScopeConstants.DEFAULT_CATALOG_REQUEST_SCOPE);

        // Act
        extension.initialize(context);

        // Assert
        verify(config).getString(ScopeConstants.CATALOG_REQUEST_SCOPE_PROPERTY, ScopeConstants.DEFAULT_CATALOG_REQUEST_SCOPE);
        verify(config).getString(ScopeConstants.CONTRACT_REQUEST_SCOPE_PROPERTY, ScopeConstants.DEFAULT_CATALOG_REQUEST_SCOPE);
        verify(config).getString(ScopeConstants.TRANSFER_REQUEST_SCOPE_PROPERTY, ScopeConstants.DEFAULT_CATALOG_REQUEST_SCOPE);
        verify(policyEngine, times(3)).registerPostValidator(any(Class.class), any(PolicyValidatorRule.class));
    }

    @Test
    void initializeShouldHandleMultipleScopesInConfiguration() {
        // Arrange
        String multipleCatalogScopes = "scope1,scope2,scope3";
        String multipleContractScopes = "contract-scope1,contract-scope2";
        String multipleTransferScopes = "transfer-scope1,transfer-scope2,transfer-scope3";
        
        when(config.getString(ScopeConstants.CATALOG_REQUEST_SCOPE_PROPERTY, ScopeConstants.DEFAULT_CATALOG_REQUEST_SCOPE))
                .thenReturn(multipleCatalogScopes);
        when(config.getString(ScopeConstants.CONTRACT_REQUEST_SCOPE_PROPERTY, ScopeConstants.DEFAULT_CATALOG_REQUEST_SCOPE))
                .thenReturn(multipleContractScopes);
        when(config.getString(ScopeConstants.TRANSFER_REQUEST_SCOPE_PROPERTY, ScopeConstants.DEFAULT_CATALOG_REQUEST_SCOPE))
                .thenReturn(multipleTransferScopes);

        // Act
        extension.initialize(context);

        // Assert
        verify(policyEngine).registerPostValidator(eq(RequestCatalogPolicyContext.class), any(PolicyValidatorRule.class));
        verify(policyEngine).registerPostValidator(eq(RequestContractNegotiationPolicyContext.class), any(PolicyValidatorRule.class));
        verify(policyEngine).registerPostValidator(eq(RequestTransferProcessPolicyContext.class), any(PolicyValidatorRule.class));
        verify(monitor).info(anyString());
    }

    @Test
    void initializeShouldRegisterDifferentValidatorsForEachContext() {
        // Arrange
        when(config.getString(ScopeConstants.CATALOG_REQUEST_SCOPE_PROPERTY, ScopeConstants.DEFAULT_CATALOG_REQUEST_SCOPE))
                .thenReturn("catalog-scope");
        when(config.getString(ScopeConstants.CONTRACT_REQUEST_SCOPE_PROPERTY, ScopeConstants.DEFAULT_CATALOG_REQUEST_SCOPE))
                .thenReturn("contract-scope");
        when(config.getString(ScopeConstants.TRANSFER_REQUEST_SCOPE_PROPERTY, ScopeConstants.DEFAULT_CATALOG_REQUEST_SCOPE))
                .thenReturn("transfer-scope");

        // Act
        extension.initialize(context);

        // Assert - verify each context type is registered separately
        verify(policyEngine).registerPostValidator(eq(RequestCatalogPolicyContext.class), any(PolicyValidatorRule.class));
        verify(policyEngine).registerPostValidator(eq(RequestContractNegotiationPolicyContext.class), any(PolicyValidatorRule.class));
        verify(policyEngine).registerPostValidator(eq(RequestTransferProcessPolicyContext.class), any(PolicyValidatorRule.class));
    }

    @Test
    void initializeShouldLogDebugAndInfoMessages() {
        // Arrange
        when(config.getString(anyString(), anyString())).thenReturn("test-scope");

        // Act
        extension.initialize(context);

        // Assert
        verify(monitor).debug("Scope Extension initializing..");
        verify(monitor).info("Scope Extension initialized - registered scope mapping for catalog, contract negotiation and data transfer requests");
    }

    @Test
    void initializeShouldHandleEmptyScopes() {
        // Arrange
        when(config.getString(ScopeConstants.CATALOG_REQUEST_SCOPE_PROPERTY, ScopeConstants.DEFAULT_CATALOG_REQUEST_SCOPE))
                .thenReturn("");
        when(config.getString(ScopeConstants.CONTRACT_REQUEST_SCOPE_PROPERTY, ScopeConstants.DEFAULT_CATALOG_REQUEST_SCOPE))
                .thenReturn("");
        when(config.getString(ScopeConstants.TRANSFER_REQUEST_SCOPE_PROPERTY, ScopeConstants.DEFAULT_CATALOG_REQUEST_SCOPE))
                .thenReturn("");

        // Act
        extension.initialize(context);

        // Assert - should still register validators even with empty scopes
        verify(policyEngine, times(3)).registerPostValidator(any(Class.class), any(PolicyValidatorRule.class));
        verify(monitor).info(anyString());
    }
}

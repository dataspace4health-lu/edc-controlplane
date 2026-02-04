/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package com.nttdata.dataspace.cp.issuer;

import org.eclipse.edc.iam.verifiablecredentials.spi.model.Issuer;
import org.eclipse.edc.iam.verifiablecredentials.spi.validation.TrustedIssuerRegistry;
import org.eclipse.edc.json.JacksonTypeManager;
import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.eclipse.edc.spi.types.TypeManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(DependencyInjectionExtension.class)
class TrustedIssuerConfigurationExtensionTest {

    private static final String ISSUER1_LITERAL = "issuer1";
    private static final String CONFIG_PREFIX = "edc.iam.trustedissuer";
    private static final String ISSUER_ID_1 = "issuerId1";
    private static final String ISSUER_ID_2 = "issuerId2";

    private final TrustedIssuerRegistry trustedIssuerRegistry = mock();
    private final Monitor monitor = mock();
    private final Monitor prefixedMonitor = mock();
    private final TypeManager typeManager = new JacksonTypeManager();

    @BeforeEach
    void setup(ServiceExtensionContext context) {
        when(monitor.withPrefix(anyString())).thenReturn(prefixedMonitor);
        when(context.getMonitor()).thenReturn(monitor);
        
        context.registerService(TrustedIssuerRegistry.class, trustedIssuerRegistry);
        context.registerService(TypeManager.class, typeManager);
        context.registerService(Monitor.class, monitor);
    }

    @Test
    void initializeIssuerWithSupportedTypes(ServiceExtensionContext context, TrustedIssuerConfigurationExtension ext) {
        var cfg = ConfigFactory.fromMap(Map.of("issuer1.id", ISSUER1_LITERAL, "issuer1.supportedtypes", "[\"type1\", \"type2\"]"));
        when(context.getConfig(CONFIG_PREFIX)).thenReturn(cfg);

        ext.initialize(context);

        verify(trustedIssuerRegistry).register(argThat(issuer -> issuer.id().equals(ISSUER1_LITERAL)), eq("type1"));
        verify(trustedIssuerRegistry).register(argThat(issuer -> issuer.id().equals(ISSUER1_LITERAL)), eq("type1"));
    }

    @Test
    void initializeIssuerWithoutSupportedType(ServiceExtensionContext context, TrustedIssuerConfigurationExtension ext) {
        var cfg = ConfigFactory.fromMap(Map.of("issuer1.id", ISSUER1_LITERAL));
        when(context.getConfig(CONFIG_PREFIX)).thenReturn(cfg);

        ext.initialize(context);

        verify(trustedIssuerRegistry).register(argThat(issuer -> issuer.id().equals(ISSUER1_LITERAL)), eq(TrustedIssuerRegistry.WILDCARD));
    }

    @Test
    void initializeWithNoIssuer(ServiceExtensionContext context, TrustedIssuerConfigurationExtension ext) {
        var cfg = ConfigFactory.fromMap(Map.of());
        when(context.getConfig(CONFIG_PREFIX)).thenReturn(cfg);

        ext.initialize(context);

        verifyNoMoreInteractions(trustedIssuerRegistry);

        verify(prefixedMonitor).warning("The list of trusted issuers is empty");

    }

    @Test
    void initializeWithProperties(ServiceExtensionContext context, TrustedIssuerConfigurationExtension ext) {
        var properties = "{\"custom\": \"test\"}";
        var cfg = ConfigFactory.fromMap(Map.of("issuer1.id", ISSUER_ID_1, "issuer1.properties", properties));
        when(context.getConfig(CONFIG_PREFIX)).thenReturn(cfg);

        ext.initialize(context);

        verify(trustedIssuerRegistry).register(argThat(issuer -> issuer.additionalProperties().get("custom").equals("test")), eq(TrustedIssuerRegistry.WILDCARD));
    }

    @Test
    void initializeWithTwoIssuers(ServiceExtensionContext context, TrustedIssuerConfigurationExtension ext) {
        var cfg = ConfigFactory.fromMap(Map.of("issuer1.id", ISSUER_ID_1, "issuer2.id", ISSUER_ID_2));
        when(context.getConfig(CONFIG_PREFIX)).thenReturn(cfg);

        ext.initialize(context);

        var issuers = ArgumentCaptor.forClass(Issuer.class);

        verify(trustedIssuerRegistry, times(2)).register(issuers.capture(), any());

        assertThat(issuers.getAllValues()).hasSize(2)
                .extracting(Issuer::id)
                .contains(ISSUER_ID_1, ISSUER_ID_2);
    }
}

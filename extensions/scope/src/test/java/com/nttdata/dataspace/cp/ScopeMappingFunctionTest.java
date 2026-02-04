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

import org.eclipse.edc.policy.context.request.spi.RequestPolicyContext;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.iam.RequestScope;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScopeMappingFunctionTest {

    private static final String SCOPE_1 = "scope1";
    private static final String SCOPE_2 = "scope2";
    private static final String SCOPE_3 = "scope3";
    private static final String SCOPE_4 = "scope4";
    private static final String SCOPE_5 = "scope5";

    @Mock
    private RequestPolicyContext requestPolicyContext;

    @Mock
    private RequestScope.Builder requestScopeBuilder;

    @Mock
    private RequestScope requestScope;

    @Mock
    private Policy policy;

    private ScopeMappingFunction scopeMappingFunction;
    private Set<String> defaultScopes;

    @BeforeEach
    void setUp() {
        defaultScopes = Set.of(SCOPE_1, SCOPE_2);
        scopeMappingFunction = new ScopeMappingFunction(defaultScopes);
    }

    @Test
    void applyShouldMergeDefaultScopesWithExistingScopes() {
        // Arrange
        Set<String> existingScopes = Set.of(SCOPE_3, SCOPE_4);
        when(requestPolicyContext.requestScopeBuilder()).thenReturn(requestScopeBuilder);
        when(requestScopeBuilder.build()).thenReturn(requestScope);
        when(requestScope.getScopes()).thenReturn(existingScopes);

        // Act
        Boolean result = scopeMappingFunction.apply(policy, requestPolicyContext);

        // Assert
        assertThat(result).isTrue();
        
        ArgumentCaptor<Set<String>> scopesCaptor = ArgumentCaptor.forClass(Set.class);
        verify(requestScopeBuilder).scopes(scopesCaptor.capture());
        
        Set<String> capturedScopes = scopesCaptor.getValue();
        assertThat(capturedScopes)
                .hasSize(4)
                .containsAll(defaultScopes)
                .containsAll(existingScopes);
    }

    @Test
    void applyShouldAddDefaultScopesWhenNoExistingScopes() {
        // Arrange
        Set<String> emptyScopes = Set.of();
        when(requestPolicyContext.requestScopeBuilder()).thenReturn(requestScopeBuilder);
        when(requestScopeBuilder.build()).thenReturn(requestScope);
        when(requestScope.getScopes()).thenReturn(emptyScopes);

        // Act
        Boolean result = scopeMappingFunction.apply(policy, requestPolicyContext);

        // Assert
        assertThat(result).isTrue();
        
        ArgumentCaptor<Set<String>> scopesCaptor = ArgumentCaptor.forClass(Set.class);
        verify(requestScopeBuilder).scopes(scopesCaptor.capture());
        
        Set<String> capturedScopes = scopesCaptor.getValue();
        assertThat(capturedScopes)
                .hasSize(2)
                .containsExactlyInAnyOrderElementsOf(defaultScopes);
    }

    @Test
    void applyShouldHandleDuplicateScopes() {
        // Arrange - existing scopes overlap with default scopes
        Set<String> existingScopes = Set.of(SCOPE_1, SCOPE_5);
        when(requestPolicyContext.requestScopeBuilder()).thenReturn(requestScopeBuilder);
        when(requestScopeBuilder.build()).thenReturn(requestScope);
        when(requestScope.getScopes()).thenReturn(existingScopes);

        // Act
        Boolean result = scopeMappingFunction.apply(policy, requestPolicyContext);

        // Assert
        assertThat(result).isTrue();
        
        ArgumentCaptor<Set<String>> scopesCaptor = ArgumentCaptor.forClass(Set.class);
        verify(requestScopeBuilder).scopes(scopesCaptor.capture());
        
        Set<String> capturedScopes = scopesCaptor.getValue();
        // Should contain scope1, scope2, scope5 (no duplicates)
        assertThat(capturedScopes)
                .hasSize(3)
                .contains(SCOPE_1, SCOPE_2, SCOPE_5);
    }

    @Test
    void applyShouldWorkWithSingleDefaultScope() {
        // Arrange
        Set<String> singleScope = Set.of("single-scope");
        scopeMappingFunction = new ScopeMappingFunction(singleScope);
        
        Set<String> existingScopes = Set.of("existing-scope");
        when(requestPolicyContext.requestScopeBuilder()).thenReturn(requestScopeBuilder);
        when(requestScopeBuilder.build()).thenReturn(requestScope);
        when(requestScope.getScopes()).thenReturn(existingScopes);

        // Act
        Boolean result = scopeMappingFunction.apply(policy, requestPolicyContext);

        // Assert
        assertThat(result).isTrue();
        
        ArgumentCaptor<Set<String>> scopesCaptor = ArgumentCaptor.forClass(Set.class);
        verify(requestScopeBuilder).scopes(scopesCaptor.capture());
        
        Set<String> capturedScopes = scopesCaptor.getValue();
        assertThat(capturedScopes)
                .hasSize(2)
                .contains("single-scope", "existing-scope");
    }

    @Test
    void applyShouldAlwaysReturnTrue() {
        // Arrange
        when(requestPolicyContext.requestScopeBuilder()).thenReturn(requestScopeBuilder);
        when(requestScopeBuilder.build()).thenReturn(requestScope);
        when(requestScope.getScopes()).thenReturn(Set.of());

        // Act
        Boolean result = scopeMappingFunction.apply(policy, requestPolicyContext);

        // Assert
        assertThat(result).isTrue();
    }
}

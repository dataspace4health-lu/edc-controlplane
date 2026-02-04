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
import org.eclipse.edc.policy.engine.spi.PolicyValidatorRule;
import org.eclipse.edc.policy.model.Policy;

import java.util.HashSet;
import java.util.Set;

/**
 * Policy validator rule that merges default scopes with existing request scopes.
 * Follows the MVD pattern for scope mapping in catalog policy contexts.
 */
public class ScopeMappingFunction implements PolicyValidatorRule<RequestPolicyContext> {
    private final Set<String> defaultScopes;

    public ScopeMappingFunction(Set<String> defaultScopes) {
        this.defaultScopes = defaultScopes;
    }

    @Override
    public Boolean apply(Policy policy, RequestPolicyContext requestPolicyContext) {
        var requestScopeBuilder = requestPolicyContext.requestScopeBuilder();
        var existingScopes = requestScopeBuilder.build().getScopes();
        var mergedScopes = new HashSet<>(defaultScopes);
        mergedScopes.addAll(existingScopes);
        requestScopeBuilder.scopes(mergedScopes);
        return true;
    }
}

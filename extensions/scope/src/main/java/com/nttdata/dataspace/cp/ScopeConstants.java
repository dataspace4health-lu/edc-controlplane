package com.nttdata.dataspace.cp;

public class ScopeConstants {
    private ScopeConstants() {
    }
    
    public static final String CATALOG_REQUEST_SCOPE_PROPERTY = "edc.policy.catalog.scope";
    public static final String CONTRACT_REQUEST_SCOPE_PROPERTY = "edc.policy.contract.scope";
    public static final String TRANSFER_REQUEST_SCOPE_PROPERTY = "edc.policy.transfer.scope";

    public static final String DEFAULT_CATALOG_REQUEST_SCOPE = "org.eclipse.edc.vc.type:MembershipCredential:read";
    
}

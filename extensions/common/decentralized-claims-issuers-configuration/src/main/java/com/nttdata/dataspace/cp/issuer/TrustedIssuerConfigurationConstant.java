package com.nttdata.dataspace.cp.issuer;

import org.eclipse.edc.runtime.metamodel.annotation.Setting;

public class TrustedIssuerConfigurationConstant {

    private TrustedIssuerConfigurationConstant() {
    }
    
    public static final String CONFIG_PREFIX = "edc.iam.trustedissuer";
    public static final String CONFIG_ALIAS = CONFIG_PREFIX + ".<issuerAlias>.";

    @Setting(context = CONFIG_ALIAS, value = "ID of the issuer.", required = true)
    public static final String ID_SUFFIX = "id";
    @Setting(context = CONFIG_ALIAS, value = "Additional properties of the issuer.")
    public static final String PROPERTIES_SUFFIX = "properties";
    @Setting(context = CONFIG_ALIAS, value = "List of supported credential types for this issuer.")
    public static final String SUPPORTEDTYPES_SUFFIX = "supportedtypes";

}

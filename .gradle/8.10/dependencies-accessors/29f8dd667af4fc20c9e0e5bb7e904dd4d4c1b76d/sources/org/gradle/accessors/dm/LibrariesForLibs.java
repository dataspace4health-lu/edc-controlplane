package org.gradle.accessors.dm;

import org.gradle.api.NonNullApi;
import org.gradle.api.artifacts.MinimalExternalModuleDependency;
import org.gradle.plugin.use.PluginDependency;
import org.gradle.api.artifacts.ExternalModuleDependencyBundle;
import org.gradle.api.artifacts.MutableVersionConstraint;
import org.gradle.api.provider.Provider;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.ProviderFactory;
import org.gradle.api.internal.catalog.AbstractExternalDependencyFactory;
import org.gradle.api.internal.catalog.DefaultVersionCatalog;
import java.util.Map;
import org.gradle.api.internal.attributes.ImmutableAttributesFactory;
import org.gradle.api.internal.artifacts.dsl.CapabilityNotationParser;
import javax.inject.Inject;

/**
 * A catalog of dependencies accessible via the {@code libs} extension.
 */
@NonNullApi
public class LibrariesForLibs extends AbstractExternalDependencyFactory {

    private final AbstractExternalDependencyFactory owner = this;
    private final ApacheLibraryAccessors laccForApacheLibraryAccessors = new ApacheLibraryAccessors(owner);
    private final EdcLibraryAccessors laccForEdcLibraryAccessors = new EdcLibraryAccessors(owner);
    private final JakartaLibraryAccessors laccForJakartaLibraryAccessors = new JakartaLibraryAccessors(owner);
    private final MinioLibraryAccessors laccForMinioLibraryAccessors = new MinioLibraryAccessors(owner);
    private final OkhttpLibraryAccessors laccForOkhttpLibraryAccessors = new OkhttpLibraryAccessors(owner);
    private final VersionAccessors vaccForVersionAccessors = new VersionAccessors(providers, config);
    private final BundleAccessors baccForBundleAccessors = new BundleAccessors(objects, providers, config, attributesFactory, capabilityNotationParser);
    private final PluginAccessors paccForPluginAccessors = new PluginAccessors(providers, config);

    @Inject
    public LibrariesForLibs(DefaultVersionCatalog config, ProviderFactory providers, ObjectFactory objects, ImmutableAttributesFactory attributesFactory, CapabilityNotationParser capabilityNotationParser) {
        super(config, providers, objects, attributesFactory, capabilityNotationParser);
    }

    /**
     * Dependency provider for <b>awaitility</b> with <b>org.awaitility:awaitility</b> coordinates and
     * with version reference <b>awaitility</b>
     * <p>
     * This dependency was declared in catalog libs.versions.toml
     */
    public Provider<MinimalExternalModuleDependency> getAwaitility() {
        return create("awaitility");
    }

    /**
     * Dependency provider for <b>postgres</b> with <b>org.postgresql:postgresql</b> coordinates and
     * with version reference <b>postgres</b>
     * <p>
     * This dependency was declared in catalog libs.versions.toml
     */
    public Provider<MinimalExternalModuleDependency> getPostgres() {
        return create("postgres");
    }

    /**
     * Dependency provider for <b>restAssured</b> with <b>io.rest-assured:rest-assured</b> coordinates and
     * with version reference <b>restAssured</b>
     * <p>
     * This dependency was declared in catalog libs.versions.toml
     */
    public Provider<MinimalExternalModuleDependency> getRestAssured() {
        return create("restAssured");
    }

    /**
     * Group of libraries at <b>apache</b>
     */
    public ApacheLibraryAccessors getApache() {
        return laccForApacheLibraryAccessors;
    }

    /**
     * Group of libraries at <b>edc</b>
     */
    public EdcLibraryAccessors getEdc() {
        return laccForEdcLibraryAccessors;
    }

    /**
     * Group of libraries at <b>jakarta</b>
     */
    public JakartaLibraryAccessors getJakarta() {
        return laccForJakartaLibraryAccessors;
    }

    /**
     * Group of libraries at <b>minio</b>
     */
    public MinioLibraryAccessors getMinio() {
        return laccForMinioLibraryAccessors;
    }

    /**
     * Group of libraries at <b>okhttp</b>
     */
    public OkhttpLibraryAccessors getOkhttp() {
        return laccForOkhttpLibraryAccessors;
    }

    /**
     * Group of versions at <b>versions</b>
     */
    public VersionAccessors getVersions() {
        return vaccForVersionAccessors;
    }

    /**
     * Group of bundles at <b>bundles</b>
     */
    public BundleAccessors getBundles() {
        return baccForBundleAccessors;
    }

    /**
     * Group of plugins at <b>plugins</b>
     */
    public PluginAccessors getPlugins() {
        return paccForPluginAccessors;
    }

    public static class ApacheLibraryAccessors extends SubDependencyFactory {
        private final ApacheCommonsLibraryAccessors laccForApacheCommonsLibraryAccessors = new ApacheCommonsLibraryAccessors(owner);

        public ApacheLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Group of libraries at <b>apache.commons</b>
         */
        public ApacheCommonsLibraryAccessors getCommons() {
            return laccForApacheCommonsLibraryAccessors;
        }

    }

    public static class ApacheCommonsLibraryAccessors extends SubDependencyFactory {

        public ApacheCommonsLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Dependency provider for <b>lang</b> with <b>org.apache.commons:commons-lang3</b> coordinates and
         * with version <b>3.14.0</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getLang() {
            return create("apache.commons.lang");
        }

    }

    public static class EdcLibraryAccessors extends SubDependencyFactory {
        private final EdcApiLibraryAccessors laccForEdcApiLibraryAccessors = new EdcApiLibraryAccessors(owner);
        private final EdcAssetLibraryAccessors laccForEdcAssetLibraryAccessors = new EdcAssetLibraryAccessors(owner);
        private final EdcAuthLibraryAccessors laccForEdcAuthLibraryAccessors = new EdcAuthLibraryAccessors(owner);
        private final EdcBomLibraryAccessors laccForEdcBomLibraryAccessors = new EdcBomLibraryAccessors(owner);
        private final EdcBuildLibraryAccessors laccForEdcBuildLibraryAccessors = new EdcBuildLibraryAccessors(owner);
        private final EdcCatalogLibraryAccessors laccForEdcCatalogLibraryAccessors = new EdcCatalogLibraryAccessors(owner);
        private final EdcConfigurationLibraryAccessors laccForEdcConfigurationLibraryAccessors = new EdcConfigurationLibraryAccessors(owner);
        private final EdcConnectorLibraryAccessors laccForEdcConnectorLibraryAccessors = new EdcConnectorLibraryAccessors(owner);
        private final EdcContractLibraryAccessors laccForEdcContractLibraryAccessors = new EdcContractLibraryAccessors(owner);
        private final EdcControlLibraryAccessors laccForEdcControlLibraryAccessors = new EdcControlLibraryAccessors(owner);
        private final EdcCoreLibraryAccessors laccForEdcCoreLibraryAccessors = new EdcCoreLibraryAccessors(owner);
        private final EdcDataLibraryAccessors laccForEdcDataLibraryAccessors = new EdcDataLibraryAccessors(owner);
        private final EdcEdrLibraryAccessors laccForEdcEdrLibraryAccessors = new EdcEdrLibraryAccessors(owner);
        private final EdcFcLibraryAccessors laccForEdcFcLibraryAccessors = new EdcFcLibraryAccessors(owner);
        private final EdcIamLibraryAccessors laccForEdcIamLibraryAccessors = new EdcIamLibraryAccessors(owner);
        private final EdcJsonLibraryAccessors laccForEdcJsonLibraryAccessors = new EdcJsonLibraryAccessors(owner);
        private final EdcManagementLibraryAccessors laccForEdcManagementLibraryAccessors = new EdcManagementLibraryAccessors(owner);
        private final EdcMonitorLibraryAccessors laccForEdcMonitorLibraryAccessors = new EdcMonitorLibraryAccessors(owner);
        private final EdcNegotiationLibraryAccessors laccForEdcNegotiationLibraryAccessors = new EdcNegotiationLibraryAccessors(owner);
        private final EdcParticipantLibraryAccessors laccForEdcParticipantLibraryAccessors = new EdcParticipantLibraryAccessors(owner);
        private final EdcPolicyLibraryAccessors laccForEdcPolicyLibraryAccessors = new EdcPolicyLibraryAccessors(owner);
        private final EdcProtocolLibraryAccessors laccForEdcProtocolLibraryAccessors = new EdcProtocolLibraryAccessors(owner);
        private final EdcRuntimeLibraryAccessors laccForEdcRuntimeLibraryAccessors = new EdcRuntimeLibraryAccessors(owner);
        private final EdcSqlLibraryAccessors laccForEdcSqlLibraryAccessors = new EdcSqlLibraryAccessors(owner);
        private final EdcTokenLibraryAccessors laccForEdcTokenLibraryAccessors = new EdcTokenLibraryAccessors(owner);
        private final EdcTransactionLibraryAccessors laccForEdcTransactionLibraryAccessors = new EdcTransactionLibraryAccessors(owner);
        private final EdcTransactionsLibraryAccessors laccForEdcTransactionsLibraryAccessors = new EdcTransactionsLibraryAccessors(owner);
        private final EdcTransferLibraryAccessors laccForEdcTransferLibraryAccessors = new EdcTransferLibraryAccessors(owner);
        private final EdcUtilLibraryAccessors laccForEdcUtilLibraryAccessors = new EdcUtilLibraryAccessors(owner);
        private final EdcValidatorLibraryAccessors laccForEdcValidatorLibraryAccessors = new EdcValidatorLibraryAccessors(owner);
        private final EdcVaultLibraryAccessors laccForEdcVaultLibraryAccessors = new EdcVaultLibraryAccessors(owner);
        private final EdcWebLibraryAccessors laccForEdcWebLibraryAccessors = new EdcWebLibraryAccessors(owner);

        public EdcLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Dependency provider for <b>boot</b> with <b>org.eclipse.edc:boot</b> coordinates and
         * with version reference <b>edc</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getBoot() {
            return create("edc.boot");
        }

        /**
         * Dependency provider for <b>dsp</b> with <b>org.eclipse.edc:dsp</b> coordinates and
         * with version reference <b>edc</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getDsp() {
            return create("edc.dsp");
        }

        /**
         * Dependency provider for <b>http</b> with <b>org.eclipse.edc:http</b> coordinates and
         * with version reference <b>edc</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getHttp() {
            return create("edc.http");
        }

        /**
         * Dependency provider for <b>junit</b> with <b>org.eclipse.edc:junit</b> coordinates and
         * with version reference <b>edc</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getJunit() {
            return create("edc.junit");
        }

        /**
         * Group of libraries at <b>edc.api</b>
         */
        public EdcApiLibraryAccessors getApi() {
            return laccForEdcApiLibraryAccessors;
        }

        /**
         * Group of libraries at <b>edc.asset</b>
         */
        public EdcAssetLibraryAccessors getAsset() {
            return laccForEdcAssetLibraryAccessors;
        }

        /**
         * Group of libraries at <b>edc.auth</b>
         */
        public EdcAuthLibraryAccessors getAuth() {
            return laccForEdcAuthLibraryAccessors;
        }

        /**
         * Group of libraries at <b>edc.bom</b>
         */
        public EdcBomLibraryAccessors getBom() {
            return laccForEdcBomLibraryAccessors;
        }

        /**
         * Group of libraries at <b>edc.build</b>
         */
        public EdcBuildLibraryAccessors getBuild() {
            return laccForEdcBuildLibraryAccessors;
        }

        /**
         * Group of libraries at <b>edc.catalog</b>
         */
        public EdcCatalogLibraryAccessors getCatalog() {
            return laccForEdcCatalogLibraryAccessors;
        }

        /**
         * Group of libraries at <b>edc.configuration</b>
         */
        public EdcConfigurationLibraryAccessors getConfiguration() {
            return laccForEdcConfigurationLibraryAccessors;
        }

        /**
         * Group of libraries at <b>edc.connector</b>
         */
        public EdcConnectorLibraryAccessors getConnector() {
            return laccForEdcConnectorLibraryAccessors;
        }

        /**
         * Group of libraries at <b>edc.contract</b>
         */
        public EdcContractLibraryAccessors getContract() {
            return laccForEdcContractLibraryAccessors;
        }

        /**
         * Group of libraries at <b>edc.control</b>
         */
        public EdcControlLibraryAccessors getControl() {
            return laccForEdcControlLibraryAccessors;
        }

        /**
         * Group of libraries at <b>edc.core</b>
         */
        public EdcCoreLibraryAccessors getCore() {
            return laccForEdcCoreLibraryAccessors;
        }

        /**
         * Group of libraries at <b>edc.data</b>
         */
        public EdcDataLibraryAccessors getData() {
            return laccForEdcDataLibraryAccessors;
        }

        /**
         * Group of libraries at <b>edc.edr</b>
         */
        public EdcEdrLibraryAccessors getEdr() {
            return laccForEdcEdrLibraryAccessors;
        }

        /**
         * Group of libraries at <b>edc.fc</b>
         */
        public EdcFcLibraryAccessors getFc() {
            return laccForEdcFcLibraryAccessors;
        }

        /**
         * Group of libraries at <b>edc.iam</b>
         */
        public EdcIamLibraryAccessors getIam() {
            return laccForEdcIamLibraryAccessors;
        }

        /**
         * Group of libraries at <b>edc.json</b>
         */
        public EdcJsonLibraryAccessors getJson() {
            return laccForEdcJsonLibraryAccessors;
        }

        /**
         * Group of libraries at <b>edc.management</b>
         */
        public EdcManagementLibraryAccessors getManagement() {
            return laccForEdcManagementLibraryAccessors;
        }

        /**
         * Group of libraries at <b>edc.monitor</b>
         */
        public EdcMonitorLibraryAccessors getMonitor() {
            return laccForEdcMonitorLibraryAccessors;
        }

        /**
         * Group of libraries at <b>edc.negotiation</b>
         */
        public EdcNegotiationLibraryAccessors getNegotiation() {
            return laccForEdcNegotiationLibraryAccessors;
        }

        /**
         * Group of libraries at <b>edc.participant</b>
         */
        public EdcParticipantLibraryAccessors getParticipant() {
            return laccForEdcParticipantLibraryAccessors;
        }

        /**
         * Group of libraries at <b>edc.policy</b>
         */
        public EdcPolicyLibraryAccessors getPolicy() {
            return laccForEdcPolicyLibraryAccessors;
        }

        /**
         * Group of libraries at <b>edc.protocol</b>
         */
        public EdcProtocolLibraryAccessors getProtocol() {
            return laccForEdcProtocolLibraryAccessors;
        }

        /**
         * Group of libraries at <b>edc.runtime</b>
         */
        public EdcRuntimeLibraryAccessors getRuntime() {
            return laccForEdcRuntimeLibraryAccessors;
        }

        /**
         * Group of libraries at <b>edc.sql</b>
         */
        public EdcSqlLibraryAccessors getSql() {
            return laccForEdcSqlLibraryAccessors;
        }

        /**
         * Group of libraries at <b>edc.token</b>
         */
        public EdcTokenLibraryAccessors getToken() {
            return laccForEdcTokenLibraryAccessors;
        }

        /**
         * Group of libraries at <b>edc.transaction</b>
         */
        public EdcTransactionLibraryAccessors getTransaction() {
            return laccForEdcTransactionLibraryAccessors;
        }

        /**
         * Group of libraries at <b>edc.transactions</b>
         */
        public EdcTransactionsLibraryAccessors getTransactions() {
            return laccForEdcTransactionsLibraryAccessors;
        }

        /**
         * Group of libraries at <b>edc.transfer</b>
         */
        public EdcTransferLibraryAccessors getTransfer() {
            return laccForEdcTransferLibraryAccessors;
        }

        /**
         * Group of libraries at <b>edc.util</b>
         */
        public EdcUtilLibraryAccessors getUtil() {
            return laccForEdcUtilLibraryAccessors;
        }

        /**
         * Group of libraries at <b>edc.validator</b>
         */
        public EdcValidatorLibraryAccessors getValidator() {
            return laccForEdcValidatorLibraryAccessors;
        }

        /**
         * Group of libraries at <b>edc.vault</b>
         */
        public EdcVaultLibraryAccessors getVault() {
            return laccForEdcVaultLibraryAccessors;
        }

        /**
         * Group of libraries at <b>edc.web</b>
         */
        public EdcWebLibraryAccessors getWeb() {
            return laccForEdcWebLibraryAccessors;
        }

    }

    public static class EdcApiLibraryAccessors extends SubDependencyFactory {

        public EdcApiLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Dependency provider for <b>observability</b> with <b>org.eclipse.edc:api-observability</b> coordinates and
         * with version reference <b>edc</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getObservability() {
            return create("edc.api.observability");
        }

    }

    public static class EdcAssetLibraryAccessors extends SubDependencyFactory {

        public EdcAssetLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Dependency provider for <b>spi</b> with <b>org.eclipse.edc:asset-spi</b> coordinates and
         * with version reference <b>edc</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getSpi() {
            return create("edc.asset.spi");
        }

    }

    public static class EdcAuthLibraryAccessors extends SubDependencyFactory {

        public EdcAuthLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Dependency provider for <b>tokenbased</b> with <b>org.eclipse.edc:auth-tokenbased</b> coordinates and
         * with version reference <b>edc</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getTokenbased() {
            return create("edc.auth.tokenbased");
        }

    }

    public static class EdcBomLibraryAccessors extends SubDependencyFactory {
        private final EdcBomControlplaneLibraryAccessors laccForEdcBomControlplaneLibraryAccessors = new EdcBomControlplaneLibraryAccessors(owner);
        private final EdcBomDataplaneLibraryAccessors laccForEdcBomDataplaneLibraryAccessors = new EdcBomDataplaneLibraryAccessors(owner);

        public EdcBomLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Group of libraries at <b>edc.bom.controlplane</b>
         */
        public EdcBomControlplaneLibraryAccessors getControlplane() {
            return laccForEdcBomControlplaneLibraryAccessors;
        }

        /**
         * Group of libraries at <b>edc.bom.dataplane</b>
         */
        public EdcBomDataplaneLibraryAccessors getDataplane() {
            return laccForEdcBomDataplaneLibraryAccessors;
        }

    }

    public static class EdcBomControlplaneLibraryAccessors extends SubDependencyFactory {

        public EdcBomControlplaneLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Dependency provider for <b>base</b> with <b>org.eclipse.edc:controlplane-base-bom</b> coordinates and
         * with version reference <b>edc</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getBase() {
            return create("edc.bom.controlplane.base");
        }

    }

    public static class EdcBomDataplaneLibraryAccessors extends SubDependencyFactory {

        public EdcBomDataplaneLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Dependency provider for <b>base</b> with <b>org.eclipse.edc:dataplane-base-bom</b> coordinates and
         * with version reference <b>edc</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getBase() {
            return create("edc.bom.dataplane.base");
        }

    }

    public static class EdcBuildLibraryAccessors extends SubDependencyFactory {

        public EdcBuildLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Dependency provider for <b>plugin</b> with <b>org.eclipse.edc.edc-build:org.eclipse.edc.edc-build.gradle.plugin</b> coordinates and
         * with version reference <b>edc</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getPlugin() {
            return create("edc.build.plugin");
        }

    }

    public static class EdcCatalogLibraryAccessors extends SubDependencyFactory {

        public EdcCatalogLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Dependency provider for <b>spi</b> with <b>org.eclipse.edc:catalog-spi</b> coordinates and
         * with version reference <b>edc</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getSpi() {
            return create("edc.catalog.spi");
        }

    }

    public static class EdcConfigurationLibraryAccessors extends SubDependencyFactory {

        public EdcConfigurationLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Dependency provider for <b>filesystem</b> with <b>org.eclipse.edc:configuration-filesystem</b> coordinates and
         * with version reference <b>edc</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getFilesystem() {
            return create("edc.configuration.filesystem");
        }

    }

    public static class EdcConnectorLibraryAccessors extends SubDependencyFactory {

        public EdcConnectorLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Dependency provider for <b>core</b> with <b>org.eclipse.edc:connector-core</b> coordinates and
         * with version reference <b>edc</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getCore() {
            return create("edc.connector.core");
        }

    }

    public static class EdcContractLibraryAccessors extends SubDependencyFactory {

        public EdcContractLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Dependency provider for <b>spi</b> with <b>org.eclipse.edc:contract-spi</b> coordinates and
         * with version reference <b>edc</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getSpi() {
            return create("edc.contract.spi");
        }

    }

    public static class EdcControlLibraryAccessors extends SubDependencyFactory {
        private final EdcControlApiLibraryAccessors laccForEdcControlApiLibraryAccessors = new EdcControlApiLibraryAccessors(owner);
        private final EdcControlPlaneLibraryAccessors laccForEdcControlPlaneLibraryAccessors = new EdcControlPlaneLibraryAccessors(owner);

        public EdcControlLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Group of libraries at <b>edc.control.api</b>
         */
        public EdcControlApiLibraryAccessors getApi() {
            return laccForEdcControlApiLibraryAccessors;
        }

        /**
         * Group of libraries at <b>edc.control.plane</b>
         */
        public EdcControlPlaneLibraryAccessors getPlane() {
            return laccForEdcControlPlaneLibraryAccessors;
        }

    }

    public static class EdcControlApiLibraryAccessors extends SubDependencyFactory {

        public EdcControlApiLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Dependency provider for <b>configuration</b> with <b>org.eclipse.edc:control-api-configuration</b> coordinates and
         * with version reference <b>edc</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getConfiguration() {
            return create("edc.control.api.configuration");
        }

    }

    public static class EdcControlPlaneLibraryAccessors extends SubDependencyFactory {
        private final EdcControlPlaneApiLibraryAccessors laccForEdcControlPlaneApiLibraryAccessors = new EdcControlPlaneApiLibraryAccessors(owner);

        public EdcControlPlaneLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Dependency provider for <b>core</b> with <b>org.eclipse.edc:control-plane-core</b> coordinates and
         * with version reference <b>edc</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getCore() {
            return create("edc.control.plane.core");
        }

        /**
         * Dependency provider for <b>spi</b> with <b>org.eclipse.edc:control-plane-spi</b> coordinates and
         * with version reference <b>edc</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getSpi() {
            return create("edc.control.plane.spi");
        }

        /**
         * Group of libraries at <b>edc.control.plane.api</b>
         */
        public EdcControlPlaneApiLibraryAccessors getApi() {
            return laccForEdcControlPlaneApiLibraryAccessors;
        }

    }

    public static class EdcControlPlaneApiLibraryAccessors extends SubDependencyFactory implements DependencyNotationSupplier {

        public EdcControlPlaneApiLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Dependency provider for <b>api</b> with <b>org.eclipse.edc:control-plane-api</b> coordinates and
         * with version reference <b>edc</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> asProvider() {
            return create("edc.control.plane.api");
        }

        /**
         * Dependency provider for <b>client</b> with <b>org.eclipse.edc:control-plane-api-client</b> coordinates and
         * with version reference <b>edc</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getClient() {
            return create("edc.control.plane.api.client");
        }

    }

    public static class EdcCoreLibraryAccessors extends SubDependencyFactory {

        public EdcCoreLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Dependency provider for <b>spi</b> with <b>org.eclipse.edc:core-spi</b> coordinates and
         * with version reference <b>edc</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getSpi() {
            return create("edc.core.spi");
        }

    }

    public static class EdcDataLibraryAccessors extends SubDependencyFactory {
        private final EdcDataPlaneLibraryAccessors laccForEdcDataPlaneLibraryAccessors = new EdcDataPlaneLibraryAccessors(owner);

        public EdcDataLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Group of libraries at <b>edc.data.plane</b>
         */
        public EdcDataPlaneLibraryAccessors getPlane() {
            return laccForEdcDataPlaneLibraryAccessors;
        }

    }

    public static class EdcDataPlaneLibraryAccessors extends SubDependencyFactory {
        private final EdcDataPlaneAwsLibraryAccessors laccForEdcDataPlaneAwsLibraryAccessors = new EdcDataPlaneAwsLibraryAccessors(owner);
        private final EdcDataPlaneAzureLibraryAccessors laccForEdcDataPlaneAzureLibraryAccessors = new EdcDataPlaneAzureLibraryAccessors(owner);
        private final EdcDataPlanePublicLibraryAccessors laccForEdcDataPlanePublicLibraryAccessors = new EdcDataPlanePublicLibraryAccessors(owner);
        private final EdcDataPlaneSelectorLibraryAccessors laccForEdcDataPlaneSelectorLibraryAccessors = new EdcDataPlaneSelectorLibraryAccessors(owner);
        private final EdcDataPlaneSelfLibraryAccessors laccForEdcDataPlaneSelfLibraryAccessors = new EdcDataPlaneSelfLibraryAccessors(owner);
        private final EdcDataPlaneSignalingLibraryAccessors laccForEdcDataPlaneSignalingLibraryAccessors = new EdcDataPlaneSignalingLibraryAccessors(owner);

        public EdcDataPlaneLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Dependency provider for <b>core</b> with <b>org.eclipse.edc:data-plane-core</b> coordinates and
         * with version reference <b>edc</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getCore() {
            return create("edc.data.plane.core");
        }

        /**
         * Dependency provider for <b>http</b> with <b>org.eclipse.edc:data-plane-http</b> coordinates and
         * with version reference <b>edc</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getHttp() {
            return create("edc.data.plane.http");
        }

        /**
         * Dependency provider for <b>iam</b> with <b>org.eclipse.edc:data-plane-iam</b> coordinates and
         * with version reference <b>edc</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getIam() {
            return create("edc.data.plane.iam");
        }

        /**
         * Dependency provider for <b>kafka</b> with <b>org.eclipse.edc:data-plane-kafka</b> coordinates and
         * with version reference <b>edc</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getKafka() {
            return create("edc.data.plane.kafka");
        }

        /**
         * Dependency provider for <b>spi</b> with <b>org.eclipse.edc:data-plane-spi</b> coordinates and
         * with version reference <b>edc</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getSpi() {
            return create("edc.data.plane.spi");
        }

        /**
         * Group of libraries at <b>edc.data.plane.aws</b>
         */
        public EdcDataPlaneAwsLibraryAccessors getAws() {
            return laccForEdcDataPlaneAwsLibraryAccessors;
        }

        /**
         * Group of libraries at <b>edc.data.plane.azure</b>
         */
        public EdcDataPlaneAzureLibraryAccessors getAzure() {
            return laccForEdcDataPlaneAzureLibraryAccessors;
        }

        /**
         * Group of libraries at <b>edc.data.plane.public</b>
         */
        public EdcDataPlanePublicLibraryAccessors getPublic() {
            return laccForEdcDataPlanePublicLibraryAccessors;
        }

        /**
         * Group of libraries at <b>edc.data.plane.selector</b>
         */
        public EdcDataPlaneSelectorLibraryAccessors getSelector() {
            return laccForEdcDataPlaneSelectorLibraryAccessors;
        }

        /**
         * Group of libraries at <b>edc.data.plane.self</b>
         */
        public EdcDataPlaneSelfLibraryAccessors getSelf() {
            return laccForEdcDataPlaneSelfLibraryAccessors;
        }

        /**
         * Group of libraries at <b>edc.data.plane.signaling</b>
         */
        public EdcDataPlaneSignalingLibraryAccessors getSignaling() {
            return laccForEdcDataPlaneSignalingLibraryAccessors;
        }

    }

    public static class EdcDataPlaneAwsLibraryAccessors extends SubDependencyFactory {

        public EdcDataPlaneAwsLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Dependency provider for <b>s3</b> with <b>org.eclipse.edc.aws:data-plane-aws-s3</b> coordinates and
         * with version reference <b>edc</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getS3() {
            return create("edc.data.plane.aws.s3");
        }

    }

    public static class EdcDataPlaneAzureLibraryAccessors extends SubDependencyFactory {

        public EdcDataPlaneAzureLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Dependency provider for <b>storage</b> with <b>org.eclipse.edc.azure:data-plane-azure-storage</b> coordinates and
         * with version reference <b>edc</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getStorage() {
            return create("edc.data.plane.azure.storage");
        }

    }

    public static class EdcDataPlanePublicLibraryAccessors extends SubDependencyFactory {

        public EdcDataPlanePublicLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Dependency provider for <b>api</b> with <b>org.eclipse.edc:data-plane-public-api-v2</b> coordinates and
         * with version reference <b>edc</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getApi() {
            return create("edc.data.plane.public.api");
        }

    }

    public static class EdcDataPlaneSelectorLibraryAccessors extends SubDependencyFactory {

        public EdcDataPlaneSelectorLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Dependency provider for <b>api</b> with <b>org.eclipse.edc:data-plane-selector-api</b> coordinates and
         * with version reference <b>edc</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getApi() {
            return create("edc.data.plane.selector.api");
        }

        /**
         * Dependency provider for <b>core</b> with <b>org.eclipse.edc:data-plane-selector-core</b> coordinates and
         * with version reference <b>edc</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getCore() {
            return create("edc.data.plane.selector.core");
        }

    }

    public static class EdcDataPlaneSelfLibraryAccessors extends SubDependencyFactory {

        public EdcDataPlaneSelfLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Dependency provider for <b>registration</b> with <b>org.eclipse.edc:data-plane-self-registration</b> coordinates and
         * with version reference <b>edc</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getRegistration() {
            return create("edc.data.plane.self.registration");
        }

    }

    public static class EdcDataPlaneSignalingLibraryAccessors extends SubDependencyFactory {

        public EdcDataPlaneSignalingLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Dependency provider for <b>api</b> with <b>org.eclipse.edc:data-plane-signaling-api</b> coordinates and
         * with version reference <b>edc</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getApi() {
            return create("edc.data.plane.signaling.api");
        }

        /**
         * Dependency provider for <b>client</b> with <b>org.eclipse.edc:data-plane-signaling-client</b> coordinates and
         * with version reference <b>edc</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getClient() {
            return create("edc.data.plane.signaling.client");
        }

    }

    public static class EdcEdrLibraryAccessors extends SubDependencyFactory {
        private final EdcEdrCacheLibraryAccessors laccForEdcEdrCacheLibraryAccessors = new EdcEdrCacheLibraryAccessors(owner);
        private final EdcEdrStoreLibraryAccessors laccForEdcEdrStoreLibraryAccessors = new EdcEdrStoreLibraryAccessors(owner);

        public EdcEdrLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Group of libraries at <b>edc.edr.cache</b>
         */
        public EdcEdrCacheLibraryAccessors getCache() {
            return laccForEdcEdrCacheLibraryAccessors;
        }

        /**
         * Group of libraries at <b>edc.edr.store</b>
         */
        public EdcEdrStoreLibraryAccessors getStore() {
            return laccForEdcEdrStoreLibraryAccessors;
        }

    }

    public static class EdcEdrCacheLibraryAccessors extends SubDependencyFactory {

        public EdcEdrCacheLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Dependency provider for <b>api</b> with <b>org.eclipse.edc:edr-cache-api</b> coordinates and
         * with version reference <b>edc</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getApi() {
            return create("edc.edr.cache.api");
        }

    }

    public static class EdcEdrStoreLibraryAccessors extends SubDependencyFactory {

        public EdcEdrStoreLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Dependency provider for <b>core</b> with <b>org.eclipse.edc:edr-store-core</b> coordinates and
         * with version reference <b>edc</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getCore() {
            return create("edc.edr.store.core");
        }

        /**
         * Dependency provider for <b>receiver</b> with <b>org.eclipse.edc:edr-store-receiver</b> coordinates and
         * with version reference <b>edc</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getReceiver() {
            return create("edc.edr.store.receiver");
        }

    }

    public static class EdcFcLibraryAccessors extends SubDependencyFactory {
        private final EdcFcExtLibraryAccessors laccForEdcFcExtLibraryAccessors = new EdcFcExtLibraryAccessors(owner);
        private final EdcFcSpiLibraryAccessors laccForEdcFcSpiLibraryAccessors = new EdcFcSpiLibraryAccessors(owner);

        public EdcFcLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Dependency provider for <b>api</b> with <b>org.eclipse.edc:federated-catalog-api</b> coordinates and
         * with version reference <b>edc</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getApi() {
            return create("edc.fc.api");
        }

        /**
         * Dependency provider for <b>core</b> with <b>org.eclipse.edc:federated-catalog-core</b> coordinates and
         * with version reference <b>edc</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getCore() {
            return create("edc.fc.core");
        }

        /**
         * Dependency provider for <b>core08</b> with <b>org.eclipse.edc:federated-catalog-core-08</b> coordinates and
         * with version reference <b>edc</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getCore08() {
            return create("edc.fc.core08");
        }

        /**
         * Dependency provider for <b>core2025</b> with <b>org.eclipse.edc:federated-catalog-core-2025</b> coordinates and
         * with version reference <b>edc</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getCore2025() {
            return create("edc.fc.core2025");
        }

        /**
         * Group of libraries at <b>edc.fc.ext</b>
         */
        public EdcFcExtLibraryAccessors getExt() {
            return laccForEdcFcExtLibraryAccessors;
        }

        /**
         * Group of libraries at <b>edc.fc.spi</b>
         */
        public EdcFcSpiLibraryAccessors getSpi() {
            return laccForEdcFcSpiLibraryAccessors;
        }

    }

    public static class EdcFcExtLibraryAccessors extends SubDependencyFactory {

        public EdcFcExtLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Dependency provider for <b>api</b> with <b>org.eclipse.edc:federated-catalog-api</b> coordinates and
         * with version reference <b>edc</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getApi() {
            return create("edc.fc.ext.api");
        }

    }

    public static class EdcFcSpiLibraryAccessors extends SubDependencyFactory {

        public EdcFcSpiLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Dependency provider for <b>core</b> with <b>org.eclipse.edc:federated-catalog-spi</b> coordinates and
         * with version reference <b>edc</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getCore() {
            return create("edc.fc.spi.core");
        }

        /**
         * Dependency provider for <b>crawler</b> with <b>org.eclipse.edc:crawler-spi</b> coordinates and
         * with version reference <b>edc</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getCrawler() {
            return create("edc.fc.spi.crawler");
        }

    }

    public static class EdcIamLibraryAccessors extends SubDependencyFactory {
        private final EdcIamSpiLibraryAccessors laccForEdcIamSpiLibraryAccessors = new EdcIamSpiLibraryAccessors(owner);

        public EdcIamLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Dependency provider for <b>mock</b> with <b>org.eclipse.edc:iam-mock</b> coordinates and
         * with version reference <b>edc</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getMock() {
            return create("edc.iam.mock");
        }

        /**
         * Group of libraries at <b>edc.iam.spi</b>
         */
        public EdcIamSpiLibraryAccessors getSpi() {
            return laccForEdcIamSpiLibraryAccessors;
        }

    }

    public static class EdcIamSpiLibraryAccessors extends SubDependencyFactory {

        public EdcIamSpiLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Dependency provider for <b>core</b> with <b>org.eclipse.edc:identity-hub-spi</b> coordinates and
         * with version reference <b>edc</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getCore() {
            return create("edc.iam.spi.core");
        }

        /**
         * Dependency provider for <b>did</b> with <b>org.eclipse.edc:did-spi</b> coordinates and
         * with version reference <b>edc</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getDid() {
            return create("edc.iam.spi.did");
        }

    }

    public static class EdcJsonLibraryAccessors extends SubDependencyFactory {
        private final EdcJsonLdLibraryAccessors laccForEdcJsonLdLibraryAccessors = new EdcJsonLdLibraryAccessors(owner);

        public EdcJsonLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Group of libraries at <b>edc.json.ld</b>
         */
        public EdcJsonLdLibraryAccessors getLd() {
            return laccForEdcJsonLdLibraryAccessors;
        }

    }

    public static class EdcJsonLdLibraryAccessors extends SubDependencyFactory {

        public EdcJsonLdLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Dependency provider for <b>lib</b> with <b>org.eclipse.edc:json-ld-lib</b> coordinates and
         * with version reference <b>edc</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getLib() {
            return create("edc.json.ld.lib");
        }

        /**
         * Dependency provider for <b>spi</b> with <b>org.eclipse.edc:json-ld-spi</b> coordinates and
         * with version reference <b>edc</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getSpi() {
            return create("edc.json.ld.spi");
        }

    }

    public static class EdcManagementLibraryAccessors extends SubDependencyFactory {
        private final EdcManagementApiLibraryAccessors laccForEdcManagementApiLibraryAccessors = new EdcManagementApiLibraryAccessors(owner);

        public EdcManagementLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Group of libraries at <b>edc.management.api</b>
         */
        public EdcManagementApiLibraryAccessors getApi() {
            return laccForEdcManagementApiLibraryAccessors;
        }

    }

    public static class EdcManagementApiLibraryAccessors extends SubDependencyFactory implements DependencyNotationSupplier {
        private final EdcManagementApiTestLibraryAccessors laccForEdcManagementApiTestLibraryAccessors = new EdcManagementApiTestLibraryAccessors(owner);

        public EdcManagementApiLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Dependency provider for <b>api</b> with <b>org.eclipse.edc:management-api</b> coordinates and
         * with version reference <b>edc</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> asProvider() {
            return create("edc.management.api");
        }

        /**
         * Group of libraries at <b>edc.management.api.test</b>
         */
        public EdcManagementApiTestLibraryAccessors getTest() {
            return laccForEdcManagementApiTestLibraryAccessors;
        }

    }

    public static class EdcManagementApiTestLibraryAccessors extends SubDependencyFactory {

        public EdcManagementApiTestLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Dependency provider for <b>fixtures</b> with <b>org.eclipse.edc:management-api-test-fixtures</b> coordinates and
         * with version reference <b>edc</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getFixtures() {
            return create("edc.management.api.test.fixtures");
        }

    }

    public static class EdcMonitorLibraryAccessors extends SubDependencyFactory {
        private final EdcMonitorJdkLibraryAccessors laccForEdcMonitorJdkLibraryAccessors = new EdcMonitorJdkLibraryAccessors(owner);

        public EdcMonitorLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Group of libraries at <b>edc.monitor.jdk</b>
         */
        public EdcMonitorJdkLibraryAccessors getJdk() {
            return laccForEdcMonitorJdkLibraryAccessors;
        }

    }

    public static class EdcMonitorJdkLibraryAccessors extends SubDependencyFactory {

        public EdcMonitorJdkLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Dependency provider for <b>logger</b> with <b>org.eclipse.edc:monitor-jdk-logger</b> coordinates and
         * with version reference <b>edc</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getLogger() {
            return create("edc.monitor.jdk.logger");
        }

    }

    public static class EdcNegotiationLibraryAccessors extends SubDependencyFactory {

        public EdcNegotiationLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Dependency provider for <b>spi</b> with <b>org.eclipse.edc:contract-negotiation-api</b> coordinates and
         * with version reference <b>edc</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getSpi() {
            return create("edc.negotiation.spi");
        }

    }

    public static class EdcParticipantLibraryAccessors extends SubDependencyFactory {

        public EdcParticipantLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Dependency provider for <b>spi</b> with <b>org.eclipse.edc:participant-spi</b> coordinates and
         * with version reference <b>edc</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getSpi() {
            return create("edc.participant.spi");
        }

    }

    public static class EdcPolicyLibraryAccessors extends SubDependencyFactory {
        private final EdcPolicyEngineLibraryAccessors laccForEdcPolicyEngineLibraryAccessors = new EdcPolicyEngineLibraryAccessors(owner);
        private final EdcPolicyRequestLibraryAccessors laccForEdcPolicyRequestLibraryAccessors = new EdcPolicyRequestLibraryAccessors(owner);

        public EdcPolicyLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Dependency provider for <b>spi</b> with <b>org.eclipse.edc:policy-spi</b> coordinates and
         * with version reference <b>edc</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getSpi() {
            return create("edc.policy.spi");
        }

        /**
         * Group of libraries at <b>edc.policy.engine</b>
         */
        public EdcPolicyEngineLibraryAccessors getEngine() {
            return laccForEdcPolicyEngineLibraryAccessors;
        }

        /**
         * Group of libraries at <b>edc.policy.request</b>
         */
        public EdcPolicyRequestLibraryAccessors getRequest() {
            return laccForEdcPolicyRequestLibraryAccessors;
        }

    }

    public static class EdcPolicyEngineLibraryAccessors extends SubDependencyFactory {

        public EdcPolicyEngineLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Dependency provider for <b>spi</b> with <b>org.eclipse.edc:policy-engine-spi</b> coordinates and
         * with version reference <b>edc</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getSpi() {
            return create("edc.policy.engine.spi");
        }

    }

    public static class EdcPolicyRequestLibraryAccessors extends SubDependencyFactory {
        private final EdcPolicyRequestContextLibraryAccessors laccForEdcPolicyRequestContextLibraryAccessors = new EdcPolicyRequestContextLibraryAccessors(owner);

        public EdcPolicyRequestLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Group of libraries at <b>edc.policy.request.context</b>
         */
        public EdcPolicyRequestContextLibraryAccessors getContext() {
            return laccForEdcPolicyRequestContextLibraryAccessors;
        }

    }

    public static class EdcPolicyRequestContextLibraryAccessors extends SubDependencyFactory {

        public EdcPolicyRequestContextLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Dependency provider for <b>spi</b> with <b>org.eclipse.edc:policy-request-context-spi</b> coordinates and
         * with version reference <b>edc</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getSpi() {
            return create("edc.policy.request.context.spi");
        }

    }

    public static class EdcProtocolLibraryAccessors extends SubDependencyFactory {
        private final EdcProtocolVersionLibraryAccessors laccForEdcProtocolVersionLibraryAccessors = new EdcProtocolVersionLibraryAccessors(owner);

        public EdcProtocolLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Group of libraries at <b>edc.protocol.version</b>
         */
        public EdcProtocolVersionLibraryAccessors getVersion() {
            return laccForEdcProtocolVersionLibraryAccessors;
        }

    }

    public static class EdcProtocolVersionLibraryAccessors extends SubDependencyFactory {

        public EdcProtocolVersionLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Dependency provider for <b>spi</b> with <b>org.eclipse.edc:protocol-version-spi</b> coordinates and
         * with version reference <b>edc</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getSpi() {
            return create("edc.protocol.version.spi");
        }

    }

    public static class EdcRuntimeLibraryAccessors extends SubDependencyFactory {

        public EdcRuntimeLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Dependency provider for <b>core</b> with <b>org.eclipse.edc:runtime-core</b> coordinates and
         * with version reference <b>edc</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getCore() {
            return create("edc.runtime.core");
        }

    }

    public static class EdcSqlLibraryAccessors extends SubDependencyFactory {
        private final EdcSqlDataplaneLibraryAccessors laccForEdcSqlDataplaneLibraryAccessors = new EdcSqlDataplaneLibraryAccessors(owner);

        public EdcSqlLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Dependency provider for <b>assetindex</b> with <b>org.eclipse.edc:asset-index-sql</b> coordinates and
         * with version reference <b>edc</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getAssetindex() {
            return create("edc.sql.assetindex");
        }

        /**
         * Dependency provider for <b>bootstrapper</b> with <b>org.eclipse.edc:sql-bootstrapper</b> coordinates and
         * with version reference <b>edc</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getBootstrapper() {
            return create("edc.sql.bootstrapper");
        }

        /**
         * Dependency provider for <b>contractdef</b> with <b>org.eclipse.edc:contract-definition-store-sql</b> coordinates and
         * with version reference <b>edc</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getContractdef() {
            return create("edc.sql.contractdef");
        }

        /**
         * Dependency provider for <b>contractneg</b> with <b>org.eclipse.edc:contract-negotiation-store-sql</b> coordinates and
         * with version reference <b>edc</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getContractneg() {
            return create("edc.sql.contractneg");
        }

        /**
         * Dependency provider for <b>core</b> with <b>org.eclipse.edc:sql-core</b> coordinates and
         * with version reference <b>edc</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getCore() {
            return create("edc.sql.core");
        }

        /**
         * Dependency provider for <b>edrcache</b> with <b>org.eclipse.edc:edr-index-sql</b> coordinates and
         * with version reference <b>edc</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getEdrcache() {
            return create("edc.sql.edrcache");
        }

        /**
         * Dependency provider for <b>lease</b> with <b>org.eclipse.edc:sql-lease</b> coordinates and
         * with version reference <b>edc</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getLease() {
            return create("edc.sql.lease");
        }

        /**
         * Dependency provider for <b>lib</b> with <b>org.eclipse.edc:sql-lib</b> coordinates and
         * with version reference <b>edc</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getLib() {
            return create("edc.sql.lib");
        }

        /**
         * Dependency provider for <b>policydef</b> with <b>org.eclipse.edc:policy-definition-store-sql</b> coordinates and
         * with version reference <b>edc</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getPolicydef() {
            return create("edc.sql.policydef");
        }

        /**
         * Dependency provider for <b>pool</b> with <b>org.eclipse.edc:sql-pool-apache-commons</b> coordinates and
         * with version reference <b>edc</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getPool() {
            return create("edc.sql.pool");
        }

        /**
         * Dependency provider for <b>transactionlocal</b> with <b>org.eclipse.edc:transaction-local</b> coordinates and
         * with version reference <b>edc</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getTransactionlocal() {
            return create("edc.sql.transactionlocal");
        }

        /**
         * Dependency provider for <b>transferprocess</b> with <b>org.eclipse.edc:transfer-process-store-sql</b> coordinates and
         * with version reference <b>edc</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getTransferprocess() {
            return create("edc.sql.transferprocess");
        }

        /**
         * Group of libraries at <b>edc.sql.dataplane</b>
         */
        public EdcSqlDataplaneLibraryAccessors getDataplane() {
            return laccForEdcSqlDataplaneLibraryAccessors;
        }

    }

    public static class EdcSqlDataplaneLibraryAccessors extends SubDependencyFactory {

        public EdcSqlDataplaneLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Dependency provider for <b>instancestore</b> with <b>org.eclipse.edc:data-plane-instance-store-sql</b> coordinates and
         * with version reference <b>edc</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getInstancestore() {
            return create("edc.sql.dataplane.instancestore");
        }

    }

    public static class EdcTokenLibraryAccessors extends SubDependencyFactory {

        public EdcTokenLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Dependency provider for <b>core</b> with <b>org.eclipse.edc:token-core</b> coordinates and
         * with version reference <b>edc</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getCore() {
            return create("edc.token.core");
        }

    }

    public static class EdcTransactionLibraryAccessors extends SubDependencyFactory {

        public EdcTransactionLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Dependency provider for <b>spi</b> with <b>org.eclipse.edc:transaction-spi</b> coordinates and
         * with version reference <b>edc</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getSpi() {
            return create("edc.transaction.spi");
        }

    }

    public static class EdcTransactionsLibraryAccessors extends SubDependencyFactory {
        private final EdcTransactionsDatasourceLibraryAccessors laccForEdcTransactionsDatasourceLibraryAccessors = new EdcTransactionsDatasourceLibraryAccessors(owner);

        public EdcTransactionsLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Group of libraries at <b>edc.transactions.datasource</b>
         */
        public EdcTransactionsDatasourceLibraryAccessors getDatasource() {
            return laccForEdcTransactionsDatasourceLibraryAccessors;
        }

    }

    public static class EdcTransactionsDatasourceLibraryAccessors extends SubDependencyFactory {

        public EdcTransactionsDatasourceLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Dependency provider for <b>spi</b> with <b>org.eclipse.edc:transaction-datasource-spi</b> coordinates and
         * with version reference <b>edc</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getSpi() {
            return create("edc.transactions.datasource.spi");
        }

    }

    public static class EdcTransferLibraryAccessors extends SubDependencyFactory {
        private final EdcTransferDataLibraryAccessors laccForEdcTransferDataLibraryAccessors = new EdcTransferDataLibraryAccessors(owner);

        public EdcTransferLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Dependency provider for <b>spi</b> with <b>org.eclipse.edc:transfer-spi</b> coordinates and
         * with version reference <b>edc</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getSpi() {
            return create("edc.transfer.spi");
        }

        /**
         * Group of libraries at <b>edc.transfer.data</b>
         */
        public EdcTransferDataLibraryAccessors getData() {
            return laccForEdcTransferDataLibraryAccessors;
        }

    }

    public static class EdcTransferDataLibraryAccessors extends SubDependencyFactory {
        private final EdcTransferDataPlaneLibraryAccessors laccForEdcTransferDataPlaneLibraryAccessors = new EdcTransferDataPlaneLibraryAccessors(owner);

        public EdcTransferDataLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Group of libraries at <b>edc.transfer.data.plane</b>
         */
        public EdcTransferDataPlaneLibraryAccessors getPlane() {
            return laccForEdcTransferDataPlaneLibraryAccessors;
        }

    }

    public static class EdcTransferDataPlaneLibraryAccessors extends SubDependencyFactory {

        public EdcTransferDataPlaneLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Dependency provider for <b>signaling</b> with <b>org.eclipse.edc:transfer-data-plane-signaling</b> coordinates and
         * with version reference <b>edc</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getSignaling() {
            return create("edc.transfer.data.plane.signaling");
        }

    }

    public static class EdcUtilLibraryAccessors extends SubDependencyFactory {

        public EdcUtilLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Dependency provider for <b>lib</b> with <b>org.eclipse.edc:util-lib</b> coordinates and
         * with version reference <b>edc</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getLib() {
            return create("edc.util.lib");
        }

    }

    public static class EdcValidatorLibraryAccessors extends SubDependencyFactory {
        private final EdcValidatorDataLibraryAccessors laccForEdcValidatorDataLibraryAccessors = new EdcValidatorDataLibraryAccessors(owner);

        public EdcValidatorLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Group of libraries at <b>edc.validator.data</b>
         */
        public EdcValidatorDataLibraryAccessors getData() {
            return laccForEdcValidatorDataLibraryAccessors;
        }

    }

    public static class EdcValidatorDataLibraryAccessors extends SubDependencyFactory {
        private final EdcValidatorDataAddressLibraryAccessors laccForEdcValidatorDataAddressLibraryAccessors = new EdcValidatorDataAddressLibraryAccessors(owner);

        public EdcValidatorDataLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Group of libraries at <b>edc.validator.data.address</b>
         */
        public EdcValidatorDataAddressLibraryAccessors getAddress() {
            return laccForEdcValidatorDataAddressLibraryAccessors;
        }

    }

    public static class EdcValidatorDataAddressLibraryAccessors extends SubDependencyFactory {
        private final EdcValidatorDataAddressHttpLibraryAccessors laccForEdcValidatorDataAddressHttpLibraryAccessors = new EdcValidatorDataAddressHttpLibraryAccessors(owner);

        public EdcValidatorDataAddressLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Group of libraries at <b>edc.validator.data.address.http</b>
         */
        public EdcValidatorDataAddressHttpLibraryAccessors getHttp() {
            return laccForEdcValidatorDataAddressHttpLibraryAccessors;
        }

    }

    public static class EdcValidatorDataAddressHttpLibraryAccessors extends SubDependencyFactory {

        public EdcValidatorDataAddressHttpLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Dependency provider for <b>data</b> with <b>org.eclipse.edc:validator-data-address-http-data</b> coordinates and
         * with version reference <b>edc</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getData() {
            return create("edc.validator.data.address.http.data");
        }

    }

    public static class EdcVaultLibraryAccessors extends SubDependencyFactory {
        private final EdcVaultHashicorpLibraryAccessors laccForEdcVaultHashicorpLibraryAccessors = new EdcVaultHashicorpLibraryAccessors(owner);

        public EdcVaultLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Dependency provider for <b>filesystem</b> with <b>org.eclipse.edc:vault-filesystem</b> coordinates and
         * with version reference <b>edc.vault</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getFilesystem() {
            return create("edc.vault.filesystem");
        }

        /**
         * Group of libraries at <b>edc.vault.hashicorp</b>
         */
        public EdcVaultHashicorpLibraryAccessors getHashicorp() {
            return laccForEdcVaultHashicorpLibraryAccessors;
        }

    }

    public static class EdcVaultHashicorpLibraryAccessors extends SubDependencyFactory implements DependencyNotationSupplier {

        public EdcVaultHashicorpLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Dependency provider for <b>hashicorp</b> with <b>org.eclipse.edc:vault-hashicorp</b> coordinates and
         * with version reference <b>edc</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> asProvider() {
            return create("edc.vault.hashicorp");
        }

        /**
         * Dependency provider for <b>spi</b> with <b>org.eclipse.edc:vault-hashicorp-spi</b> coordinates and
         * with version reference <b>edc</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getSpi() {
            return create("edc.vault.hashicorp.spi");
        }

    }

    public static class EdcWebLibraryAccessors extends SubDependencyFactory {

        public EdcWebLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Dependency provider for <b>spi</b> with <b>org.eclipse.edc:web-spi</b> coordinates and
         * with version reference <b>edc</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getSpi() {
            return create("edc.web.spi");
        }

    }

    public static class JakartaLibraryAccessors extends SubDependencyFactory {

        public JakartaLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Dependency provider for <b>rsApi</b> with <b>jakarta.ws.rs:jakarta.ws.rs-api</b> coordinates and
         * with version reference <b>rsApi</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getRsApi() {
            return create("jakarta.rsApi");
        }

    }

    public static class MinioLibraryAccessors extends SubDependencyFactory {

        public MinioLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Dependency provider for <b>io</b> with <b>io.minio:minio</b> coordinates and
         * with version <b>8.5.17</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getIo() {
            return create("minio.io");
        }

    }

    public static class OkhttpLibraryAccessors extends SubDependencyFactory {

        public OkhttpLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Dependency provider for <b>mockwebserver</b> with <b>com.squareup.okhttp3:mockwebserver</b> coordinates and
         * with version reference <b>okhttp.mockwebserver</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getMockwebserver() {
            return create("okhttp.mockwebserver");
        }

    }

    public static class VersionAccessors extends VersionFactory  {

        private final EdcVersionAccessors vaccForEdcVersionAccessors = new EdcVersionAccessors(providers, config);
        private final OkhttpVersionAccessors vaccForOkhttpVersionAccessors = new OkhttpVersionAccessors(providers, config);
        public VersionAccessors(ProviderFactory providers, DefaultVersionCatalog config) { super(providers, config); }

        /**
         * Version alias <b>awaitility</b> with value <b>4.2.2</b>
         * <p>
         * If the version is a rich version and cannot be represented as a
         * single version string, an empty string is returned.
         * <p>
         * This version was declared in catalog libs.versions.toml
         */
        public Provider<String> getAwaitility() { return getVersion("awaitility"); }

        /**
         * Version alias <b>kafkaClients</b> with value <b>4.0.0</b>
         * <p>
         * If the version is a rich version and cannot be represented as a
         * single version string, an empty string is returned.
         * <p>
         * This version was declared in catalog libs.versions.toml
         */
        public Provider<String> getKafkaClients() { return getVersion("kafkaClients"); }

        /**
         * Version alias <b>postgres</b> with value <b>42.7.3</b>
         * <p>
         * If the version is a rich version and cannot be represented as a
         * single version string, an empty string is returned.
         * <p>
         * This version was declared in catalog libs.versions.toml
         */
        public Provider<String> getPostgres() { return getVersion("postgres"); }

        /**
         * Version alias <b>restAssured</b> with value <b>5.5.5</b>
         * <p>
         * If the version is a rich version and cannot be represented as a
         * single version string, an empty string is returned.
         * <p>
         * This version was declared in catalog libs.versions.toml
         */
        public Provider<String> getRestAssured() { return getVersion("restAssured"); }

        /**
         * Version alias <b>rsApi</b> with value <b>4.0.0</b>
         * <p>
         * If the version is a rich version and cannot be represented as a
         * single version string, an empty string is returned.
         * <p>
         * This version was declared in catalog libs.versions.toml
         */
        public Provider<String> getRsApi() { return getVersion("rsApi"); }

        /**
         * Version alias <b>testcontainers</b> with value <b>1.21.3</b>
         * <p>
         * If the version is a rich version and cannot be represented as a
         * single version string, an empty string is returned.
         * <p>
         * This version was declared in catalog libs.versions.toml
         */
        public Provider<String> getTestcontainers() { return getVersion("testcontainers"); }

        /**
         * Group of versions at <b>versions.edc</b>
         */
        public EdcVersionAccessors getEdc() {
            return vaccForEdcVersionAccessors;
        }

        /**
         * Group of versions at <b>versions.okhttp</b>
         */
        public OkhttpVersionAccessors getOkhttp() {
            return vaccForOkhttpVersionAccessors;
        }

    }

    public static class EdcVersionAccessors extends VersionFactory  implements VersionNotationSupplier {

        public EdcVersionAccessors(ProviderFactory providers, DefaultVersionCatalog config) { super(providers, config); }

        /**
         * Version alias <b>edc</b> with value <b>0.13.0</b>
         * <p>
         * If the version is a rich version and cannot be represented as a
         * single version string, an empty string is returned.
         * <p>
         * This version was declared in catalog libs.versions.toml
         */
        public Provider<String> asProvider() { return getVersion("edc"); }

        /**
         * Version alias <b>edc.vault</b> with value <b>0.6.4</b>
         * <p>
         * If the version is a rich version and cannot be represented as a
         * single version string, an empty string is returned.
         * <p>
         * This version was declared in catalog libs.versions.toml
         */
        public Provider<String> getVault() { return getVersion("edc.vault"); }

    }

    public static class OkhttpVersionAccessors extends VersionFactory  {

        public OkhttpVersionAccessors(ProviderFactory providers, DefaultVersionCatalog config) { super(providers, config); }

        /**
         * Version alias <b>okhttp.mockwebserver</b> with value <b>5.1.0</b>
         * <p>
         * If the version is a rich version and cannot be represented as a
         * single version string, an empty string is returned.
         * <p>
         * This version was declared in catalog libs.versions.toml
         */
        public Provider<String> getMockwebserver() { return getVersion("okhttp.mockwebserver"); }

    }

    public static class BundleAccessors extends BundleFactory {
        private final SqlBundleAccessors baccForSqlBundleAccessors = new SqlBundleAccessors(objects, providers, config, attributesFactory, capabilityNotationParser);

        public BundleAccessors(ObjectFactory objects, ProviderFactory providers, DefaultVersionCatalog config, ImmutableAttributesFactory attributesFactory, CapabilityNotationParser capabilityNotationParser) { super(objects, providers, config, attributesFactory, capabilityNotationParser); }

        /**
         * Group of bundles at <b>bundles.sql</b>
         */
        public SqlBundleAccessors getSql() {
            return baccForSqlBundleAccessors;
        }

    }

    public static class SqlBundleAccessors extends BundleFactory {

        public SqlBundleAccessors(ObjectFactory objects, ProviderFactory providers, DefaultVersionCatalog config, ImmutableAttributesFactory attributesFactory, CapabilityNotationParser capabilityNotationParser) { super(objects, providers, config, attributesFactory, capabilityNotationParser); }

        /**
         * Dependency bundle provider for <b>sql.edc</b> which contains the following dependencies:
         * <ul>
         *    <li>org.eclipse.edc:sql-core</li>
         *    <li>org.eclipse.edc:sql-lease</li>
         *    <li>org.eclipse.edc:sql-pool-apache-commons</li>
         *    <li>org.eclipse.edc:transaction-local</li>
         *    <li>org.postgresql:postgresql</li>
         *    <li>org.eclipse.edc:asset-index-sql</li>
         *    <li>org.eclipse.edc:policy-definition-store-sql</li>
         *    <li>org.eclipse.edc:contract-definition-store-sql</li>
         *    <li>org.eclipse.edc:contract-negotiation-store-sql</li>
         *    <li>org.eclipse.edc:transfer-process-store-sql</li>
         * </ul>
         * <p>
         * This bundle was declared in catalog libs.versions.toml
         */
        public Provider<ExternalModuleDependencyBundle> getEdc() {
            return createBundle("sql.edc");
        }

    }

    public static class PluginAccessors extends PluginFactory {

        public PluginAccessors(ProviderFactory providers, DefaultVersionCatalog config) { super(providers, config); }

        /**
         * Plugin provider for <b>shadow</b> with plugin id <b>com.gradleup.shadow</b> and
         * with version <b>8.3.8</b>
         * <p>
         * This plugin was declared in catalog libs.versions.toml
         */
        public Provider<PluginDependency> getShadow() { return createPlugin("shadow"); }

    }

}

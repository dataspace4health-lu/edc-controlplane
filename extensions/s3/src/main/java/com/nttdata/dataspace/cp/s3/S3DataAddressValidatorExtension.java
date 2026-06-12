package com.nttdata.dataspace.cp.s3;

import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.validator.spi.DataAddressValidatorRegistry;

import static com.nttdata.dataspace.cp.s3.S3ControlPlaneConstants.EXTENSION_NAME;
import static com.nttdata.dataspace.cp.s3.S3ControlPlaneConstants.MONITOR_PREFIX;
import static com.nttdata.dataspace.cp.s3.S3ControlPlaneConstants.S3_DATA_TYPE;
import static com.nttdata.dataspace.cp.s3.S3ControlPlaneConstants.SUPPORTED_PROPERTIES;

@Extension(EXTENSION_NAME)
public class S3DataAddressValidatorExtension implements ServiceExtension {

    @Inject
    private DataAddressValidatorRegistry dataAddressValidatorRegistry;

    @Override
    public String name() {
        return EXTENSION_NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var monitor = context.getMonitor().withPrefix(MONITOR_PREFIX);

        dataAddressValidatorRegistry.registerSourceValidator(S3_DATA_TYPE, new AmazonS3SourceDataAddressValidator());
        dataAddressValidatorRegistry.registerDestinationValidator(S3_DATA_TYPE, new AmazonS3DestinationDataAddressValidator());

        monitor.info("Registered AmazonS3 DataAddress validators. Supported properties: %s"
                .formatted(String.join(", ", SUPPORTED_PROPERTIES)));
    }
}
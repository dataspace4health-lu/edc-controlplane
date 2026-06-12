package com.nttdata.dataspace.cp.s3;

import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.validator.spi.DataAddressValidatorRegistry;
import org.eclipse.edc.validator.spi.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.lang.reflect.Field;

import static com.nttdata.dataspace.cp.s3.S3ControlPlaneConstants.EXTENSION_NAME;
import static com.nttdata.dataspace.cp.s3.S3ControlPlaneConstants.S3_DATA_TYPE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class S3DataAddressValidatorExtensionTest {

    @Mock
    private DataAddressValidatorRegistry validatorRegistry;

    @Mock
    private ServiceExtensionContext context;

    @Mock
    private Monitor monitor;

    private S3DataAddressValidatorExtension extension;

    @BeforeEach
    void setUp() throws Exception {
        extension = new S3DataAddressValidatorExtension();
        injectField("dataAddressValidatorRegistry", validatorRegistry);

        when(context.getMonitor()).thenReturn(monitor);
        when(monitor.withPrefix(anyString())).thenReturn(monitor);
    }

    @Test
    void nameReturnsExtensionName() {
        assertThat(extension.name()).isEqualTo(EXTENSION_NAME);
    }

    @Test
    void initializeRegistersAmazonS3Validators() {
        extension.initialize(context);

        verify(validatorRegistry).registerSourceValidator(eq(S3_DATA_TYPE), any(Validator.class));
        verify(validatorRegistry).registerDestinationValidator(eq(S3_DATA_TYPE), any(Validator.class));
        verify(monitor).info(anyString());
    }

    private void injectField(String fieldName, Object value) throws Exception {
        Field field = S3DataAddressValidatorExtension.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(extension, value);
    }
}
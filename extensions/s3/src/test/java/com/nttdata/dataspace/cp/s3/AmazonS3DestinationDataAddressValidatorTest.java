package com.nttdata.dataspace.cp.s3;

import org.eclipse.edc.spi.types.domain.DataAddress;
import org.junit.jupiter.api.Test;

import static com.nttdata.dataspace.cp.s3.S3ControlPlaneConstants.ACCESS_KEY_ID_PROPERTY;
import static com.nttdata.dataspace.cp.s3.S3ControlPlaneConstants.BUCKET_NAME_PROPERTY;
import static com.nttdata.dataspace.cp.s3.S3ControlPlaneConstants.FOLDER_NAME_PROPERTY;
import static com.nttdata.dataspace.cp.s3.S3ControlPlaneConstants.OBJECT_NAME_PROPERTY;
import static com.nttdata.dataspace.cp.s3.S3ControlPlaneConstants.S3_DATA_TYPE;
import static com.nttdata.dataspace.cp.s3.S3ControlPlaneConstants.SECRET_ACCESS_KEY_PROPERTY;
import static org.assertj.core.api.Assertions.assertThat;

class AmazonS3DestinationDataAddressValidatorTest {

        private static final String TEST_BUCKET = "bucket-a";

    private final AmazonS3DestinationDataAddressValidator validator = new AmazonS3DestinationDataAddressValidator();

    @Test
    void validateSucceedsForObjectDestination() {
        var address = DataAddress.Builder.newInstance()
                .type(S3_DATA_TYPE)
                .property(BUCKET_NAME_PROPERTY, TEST_BUCKET)
                .property(OBJECT_NAME_PROPERTY, "exports/out.json")
                .build();

        var result = validator.validate(address);

        assertThat(result.succeeded()).isTrue();
    }

    @Test
    void validateSucceedsForFolderDestination() {
        var address = DataAddress.Builder.newInstance()
                .type(S3_DATA_TYPE)
                .property(BUCKET_NAME_PROPERTY, TEST_BUCKET)
                .property(FOLDER_NAME_PROPERTY, "exports")
                .build();

        var result = validator.validate(address);

        assertThat(result.succeeded()).isTrue();
    }

    @Test
    void validateFailsWithoutBucket() {
        var address = DataAddress.Builder.newInstance()
                .type(S3_DATA_TYPE)
                .property(OBJECT_NAME_PROPERTY, "exports/out.json")
                .build();

        var result = validator.validate(address);

        assertThat(result.succeeded()).isFalse();
        assertThat(result.getFailure().getViolations())
                .extracting(violation -> violation.path())
                .contains(BUCKET_NAME_PROPERTY);
    }

    @Test
    void validateFailsWithoutObjectOrFolder() {
        var address = DataAddress.Builder.newInstance()
                .type(S3_DATA_TYPE)
                .property(BUCKET_NAME_PROPERTY, TEST_BUCKET)
                .build();

        var result = validator.validate(address);

        assertThat(result.succeeded()).isFalse();
        assertThat(result.getFailure().getViolations())
                .extracting(violation -> violation.path())
                .contains("objectLocator");
    }

    @Test
    void validateFailsForPartialInlineCredentials() {
        var address = DataAddress.Builder.newInstance()
                .type(S3_DATA_TYPE)
                .property(BUCKET_NAME_PROPERTY, TEST_BUCKET)
                .property(OBJECT_NAME_PROPERTY, "exports/out.json")
                .property(SECRET_ACCESS_KEY_PROPERTY, "secret")
                .build();

        var result = validator.validate(address);

        assertThat(result.succeeded()).isFalse();
        assertThat(result.getFailure().getViolations())
                .extracting(violation -> violation.path())
                .contains(ACCESS_KEY_ID_PROPERTY, SECRET_ACCESS_KEY_PROPERTY);
    }
}
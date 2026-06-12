package com.nttdata.dataspace.cp.s3;

import org.eclipse.edc.spi.types.domain.DataAddress;
import org.junit.jupiter.api.Test;

import static com.nttdata.dataspace.cp.s3.S3ControlPlaneConstants.ACCESS_KEY_ID_PROPERTY;
import static com.nttdata.dataspace.cp.s3.S3ControlPlaneConstants.BUCKET_NAME_PROPERTY;
import static com.nttdata.dataspace.cp.s3.S3ControlPlaneConstants.FOLDER_NAME_PROPERTY;
import static com.nttdata.dataspace.cp.s3.S3ControlPlaneConstants.OBJECT_NAME_PROPERTY;
import static com.nttdata.dataspace.cp.s3.S3ControlPlaneConstants.OBJECT_PREFIX_PROPERTY;
import static com.nttdata.dataspace.cp.s3.S3ControlPlaneConstants.S3_DATA_TYPE;
import static com.nttdata.dataspace.cp.s3.S3ControlPlaneConstants.SECRET_ACCESS_KEY_PROPERTY;
import static org.assertj.core.api.Assertions.assertThat;

class AmazonS3SourceDataAddressValidatorTest {

        private static final String TEST_BUCKET = "bucket-a";

    private final AmazonS3SourceDataAddressValidator validator = new AmazonS3SourceDataAddressValidator();

    @Test
    void validateSucceedsForObjectSource() {
        var address = DataAddress.Builder.newInstance()
                .type(S3_DATA_TYPE)
                .property(BUCKET_NAME_PROPERTY, TEST_BUCKET)
                .property(OBJECT_NAME_PROPERTY, "records/file.json")
                .build();

        var result = validator.validate(address);

        assertThat(result.succeeded()).isTrue();
    }

    @Test
    void validateSucceedsForPrefixSource() {
        var address = DataAddress.Builder.newInstance()
                .type(S3_DATA_TYPE)
                .property(BUCKET_NAME_PROPERTY, TEST_BUCKET)
                .property(OBJECT_PREFIX_PROPERTY, "records/")
                .build();

        var result = validator.validate(address);

        assertThat(result.succeeded()).isTrue();
    }

    @Test
    void validateFailsWithoutBucket() {
        var address = DataAddress.Builder.newInstance()
                .type(S3_DATA_TYPE)
                .property(OBJECT_NAME_PROPERTY, "records/file.json")
                .build();

        var result = validator.validate(address);

        assertThat(result.succeeded()).isFalse();
        assertThat(result.getFailure().getViolations())
                .extracting(violation -> violation.path())
                .contains(BUCKET_NAME_PROPERTY);
    }

    @Test
    void validateFailsWithoutAnyLocator() {
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
                .property(FOLDER_NAME_PROPERTY, "records")
                .property(ACCESS_KEY_ID_PROPERTY, "key-id")
                .build();

        var result = validator.validate(address);

        assertThat(result.succeeded()).isFalse();
        assertThat(result.getFailure().getViolations())
                .extracting(violation -> violation.path())
                .contains(ACCESS_KEY_ID_PROPERTY, SECRET_ACCESS_KEY_PROPERTY);
    }
}
package com.nttdata.dataspace.cp.s3;

import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.validator.spi.ValidationResult;
import org.eclipse.edc.validator.spi.Validator;

import java.util.ArrayList;
import java.util.List;

import static com.nttdata.dataspace.cp.s3.S3ControlPlaneConstants.ACCESS_KEY_ID_PROPERTY;
import static com.nttdata.dataspace.cp.s3.S3ControlPlaneConstants.BUCKET_NAME_PROPERTY;
import static com.nttdata.dataspace.cp.s3.S3ControlPlaneConstants.FOLDER_NAME_PROPERTY;
import static com.nttdata.dataspace.cp.s3.S3ControlPlaneConstants.OBJECT_NAME_PROPERTY;
import static com.nttdata.dataspace.cp.s3.S3ControlPlaneConstants.SECRET_ACCESS_KEY_PROPERTY;
import static org.eclipse.edc.validator.spi.Violation.violation;

public class AmazonS3DestinationDataAddressValidator implements Validator<DataAddress> {

    @Override
    public ValidationResult validate(DataAddress input) {
        var violations = new ArrayList<org.eclipse.edc.validator.spi.Violation>();

        if (isBlank(input.getStringProperty(BUCKET_NAME_PROPERTY))) {
            violations.add(violation("'bucketName' is mandatory for AmazonS3 destination addresses", BUCKET_NAME_PROPERTY));
        }

        if (isBlank(input.getStringProperty(OBJECT_NAME_PROPERTY)) && isBlank(input.getStringProperty(FOLDER_NAME_PROPERTY))) {
            violations.add(violation("AmazonS3 destination addresses must define 'objectName' or 'folderName'", "objectLocator"));
        }

        violations.addAll(validateInlineCredentials(input));

        return violations.isEmpty() ? ValidationResult.success() : ValidationResult.failure(violations);
    }

    private List<org.eclipse.edc.validator.spi.Violation> validateInlineCredentials(DataAddress input) {
        var accessKeyId = input.getStringProperty(ACCESS_KEY_ID_PROPERTY);
        var secretAccessKey = input.getStringProperty(SECRET_ACCESS_KEY_PROPERTY);

        if (isBlank(accessKeyId) == isBlank(secretAccessKey)) {
            return List.of();
        }

        return List.of(
                violation("AmazonS3 inline credentials require both 'accessKeyId' and 'secretAccessKey' when either one is provided", ACCESS_KEY_ID_PROPERTY, accessKeyId),
                violation("AmazonS3 inline credentials require both 'accessKeyId' and 'secretAccessKey' when either one is provided", SECRET_ACCESS_KEY_PROPERTY, secretAccessKey)
        );
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
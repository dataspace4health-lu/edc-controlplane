package com.nttdata.dataspace.cp.s3;

import java.util.List;

public final class S3ControlPlaneConstants {

    public static final String EXTENSION_NAME = "Amazon S3 Control Plane Validation Extension";
    public static final String MONITOR_PREFIX = "S3-CONTROLPLANE";
    public static final String S3_DATA_TYPE = "AmazonS3";

    public static final String BUCKET_NAME_PROPERTY = "bucketName";
    public static final String OBJECT_NAME_PROPERTY = "objectName";
    public static final String FOLDER_NAME_PROPERTY = "folderName";
    public static final String OBJECT_PREFIX_PROPERTY = "objectPrefix";
    public static final String ENDPOINT_OVERRIDE_PROPERTY = "endpointOverride";
    public static final String REGION_PROPERTY = "region";
    public static final String ACCESS_KEY_ID_PROPERTY = "accessKeyId";
    public static final String SECRET_ACCESS_KEY_PROPERTY = "secretAccessKey";
    public static final String KEY_NAME_PROPERTY = "keyName";

    public static final List<String> SUPPORTED_PROPERTIES = List.of(
            BUCKET_NAME_PROPERTY,
            OBJECT_NAME_PROPERTY,
            FOLDER_NAME_PROPERTY,
            OBJECT_PREFIX_PROPERTY,
            ENDPOINT_OVERRIDE_PROPERTY,
            REGION_PROPERTY,
            ACCESS_KEY_ID_PROPERTY,
            SECRET_ACCESS_KEY_PROPERTY,
            KEY_NAME_PROPERTY
    );

    private S3ControlPlaneConstants() {
    }
}
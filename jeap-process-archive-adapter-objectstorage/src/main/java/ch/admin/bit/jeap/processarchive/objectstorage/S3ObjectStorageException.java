package ch.admin.bit.jeap.processarchive.objectstorage;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class S3ObjectStorageException extends RuntimeException {

    public S3ObjectStorageException(String message, Throwable cause) {
        super(message, cause);
    }

    public static S3ObjectStorageException connectingFailed(Exception e, S3ObjectStorageConnectionProperties s3ObjectStorageConnectionProperties) {
        String msg = String.format("Error connecting to server using configuration %s.", s3ObjectStorageConnectionProperties.toString());
        log.error(msg, e);
        return new S3ObjectStorageException(msg, e);
    }

    public static S3ObjectStorageException checkingIfBucketExistsFailed(Exception e, String bucketName) {
        String msg = String.format("Error checking if bucket '%s' exists.", bucketName);
        log.error(msg, e);
        return new S3ObjectStorageException(msg, e);
    }

    public static S3ObjectStorageException puttingObjectFailed(Exception e, String bucketName, String objectKey, int objectLen) {
        String msg = String.format("Error putting object with key '%s' and size %d bytes into bucket '%s'.", objectKey, objectLen, bucketName);
        log.error(msg, e);
        return new S3ObjectStorageException(msg, e);
    }

    public static S3ObjectStorageException gettingObjectFailed(Exception e, String bucketName, String objectKey, String objectVersionId) {
        String msg = String.format("Error getting object with key '%s' and version '%s' from bucket '%s'.", objectKey, objectVersionId, bucketName);
        log.error(msg, e);
        return new S3ObjectStorageException(msg, e);
    }

    public static S3ObjectStorageException checkingObjectExistsFailed(Exception e, String bucketName, String objectKey) {
        String msg = String.format("Error checking if object with key '%s' exists in bucket '%s'.", objectKey, bucketName);
        log.error(msg, e);
        return new S3ObjectStorageException(msg, e);
    }

}

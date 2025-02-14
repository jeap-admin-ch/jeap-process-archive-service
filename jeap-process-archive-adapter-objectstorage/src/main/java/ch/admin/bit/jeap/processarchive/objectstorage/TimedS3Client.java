package ch.admin.bit.jeap.processarchive.objectstorage;

import io.micrometer.core.annotation.Timed;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.auth.signer.AwsS3V4Signer;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.client.config.SdkAdvancedClientOption;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.model.*;

import java.net.URI;
import java.util.Map;

import static org.springframework.util.StringUtils.hasText;

@Slf4j
public class TimedS3Client {

    private final S3Client s3Client;

    private final AwsCredentialsProvider awsCredentialsProvider;

    public TimedS3Client(S3ObjectStorageConnectionProperties connectionProperties, AwsCredentialsProvider awsCredentialsProvider) {
        log.info("Initializing s3Client with connection properties {}", connectionProperties);

        this.awsCredentialsProvider = awsCredentialsProvider;

        ClientOverrideConfiguration.Builder overrideConfig = ClientOverrideConfiguration.builder();
        overrideConfig.advancedOptions(Map.of(SdkAdvancedClientOption.SIGNER, AwsS3V4Signer.create()));

        S3ClientBuilder s3ClientBuilder = S3Client.builder()
                .region(connectionProperties.region())
                .forcePathStyle(true)
                .credentialsProvider(awsCredentialsProvider(connectionProperties))
                .httpClient(UrlConnectionHttpClient.builder().build())
                .overrideConfiguration(overrideConfig.build());
        if (hasText(connectionProperties.getAccessUrl())) {
            log.info("Overriding endpoint in S3Client...");
            s3ClientBuilder = s3ClientBuilder.endpointOverride(retrieveEndpointURI(connectionProperties.getAccessUrl()));
        }
        s3Client = s3ClientBuilder.build();

        log.info("The initialization of s3Client was successful");
    }

    private AwsCredentialsProvider awsCredentialsProvider(S3ObjectStorageConnectionProperties props) {
        if (props.getAccessKey() != null && props.getSecretKey() != null) {
            log.debug("Creating AwsCredentialsProvider using configured accessKey and secretKey...");
            return StaticCredentialsProvider.create(AwsBasicCredentials.create(props.getAccessKey(), props.getSecretKey()));
        }
        return awsCredentialsProvider;
    }

    private URI retrieveEndpointURI(String accessUrl) {
        if (accessUrl.startsWith("http://") || accessUrl.startsWith("https://")) {
            return URI.create(accessUrl);
        }
        return URI.create("https://" + accessUrl);
    }

    @Timed(value = "jeap_pas_s3_client_put_object", description = "TimedS3Client: Put object", percentiles = {0.5, 0.8, 0.95, 0.99})
    public PutObjectResponse putObject(PutObjectRequest request, RequestBody requestBody) {
        return s3Client.putObject(request, requestBody);
    }

    @Timed(value = "jeap_pas_s3_client_does_object_exist", description = "TimedS3Client: Does object exist", percentiles = {0.5, 0.80, 0.95, 0.99})
    public boolean doesObjectExist(String bucketName, String objectKey) {
        try {
            s3Client.headObject(HeadObjectRequest.builder().bucket(bucketName).key(objectKey).build());
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        }
    }

    @Timed(value = "jeap_pas_s3_client_head_object", description = "TimedS3Client: Head object request", percentiles = {0.5, 0.80, 0.95, 0.99})
    public HeadObjectResponse headObject(HeadObjectRequest headObjectRequest) {
        return s3Client.headObject(headObjectRequest);
    }

    @Timed(value = "jeap_pas_s3_client_get_bucket_lifecycle_configuration", description = "TimedS3Client: Get Bucket Lifecycle Configuration", percentiles = {0.5, 0.80, 0.95, 0.99})
    public GetBucketLifecycleConfigurationResponse getBucketLifecycleConfiguration(GetBucketLifecycleConfigurationRequest getBucketLifecycleConfigurationRequest) {
        return s3Client.getBucketLifecycleConfiguration(getBucketLifecycleConfigurationRequest);
    }

    @Timed(value = "jeap_pas_s3_client_put_bucket_lifecycle_configuration", description = "TimedS3Client: Put Bucket Lifecycle Configuration", percentiles = {0.5, 0.80, 0.95, 0.99})
    public void putBucketLifecycleConfiguration(PutBucketLifecycleConfigurationRequest putBucketLifecycleConfigurationRequest) {
        s3Client.putBucketLifecycleConfiguration(putBucketLifecycleConfigurationRequest);
    }

    /**
     * Only for tests
     */
    public ResponseBytes<GetObjectResponse> getObjectAsBytes(GetObjectRequest getObjectRequest) {
        return s3Client.getObjectAsBytes(getObjectRequest);
    }

    /**
     * Only for tests
     */
    public void createBucket(CreateBucketRequest cbr) {
        s3Client.createBucket(cbr);
    }
}

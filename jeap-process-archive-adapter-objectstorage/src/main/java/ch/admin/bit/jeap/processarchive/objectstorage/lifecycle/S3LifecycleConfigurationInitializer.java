package ch.admin.bit.jeap.processarchive.objectstorage.lifecycle;

import ch.admin.bit.jeap.processarchive.domain.archive.lifecycle.LifecyclePolicy;
import ch.admin.bit.jeap.processarchive.objectstorage.TimedS3Client;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.s3.model.*;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import static net.logstash.logback.argument.StructuredArguments.value;

@RequiredArgsConstructor
@Slf4j
public class S3LifecycleConfigurationInitializer {
    private static final String TAG_ARCHIVE_TYPE_POLICY = "ArchiveTypeLifecyclePolicy";

    private final Set<LifecyclePolicyOnBucket> initializedPolicyCache = new CopyOnWriteArraySet<>();

    private final S3LifecycleConfigurationFactory lifecycleConfigurationFactory;

    private final TimedS3Client s3Client;

    public void ensureLifecyclePolicyPresent(String bucketName, LifecyclePolicy lifecyclePolicy) {
        LifecyclePolicyOnBucket cacheKey = LifecyclePolicyOnBucket.of(lifecyclePolicy, bucketName);
        if (initializedPolicyCache.contains(cacheKey)) {
            log.debug("Lifecycle policy {} on bucket {} already exists (cached)", lifecyclePolicy, bucketName);
            return;
        }

        GetBucketLifecycleConfigurationResponse getBucketLifecycleConfigurationResponse;

        try {
            getBucketLifecycleConfigurationResponse = s3Client.getBucketLifecycleConfiguration(GetBucketLifecycleConfigurationRequest.builder()
                    .bucket(bucketName)
                    .build());
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                log.debug("Currently no lifecycle configuration on bucket {}", bucketName);
                getBucketLifecycleConfigurationResponse = null;
            } else {
                log.error("Received error for getBucketLifecycleConfiguration", e);
                throw e;
            }
        }

        if (!isPolicyConfigured(getBucketLifecycleConfigurationResponse, lifecyclePolicy)) {
            updateLifecyclePoliciesOnBucket(getBucketLifecycleConfigurationResponse, bucketName);
        } else {
            log.debug("Lifecycle policy {} on bucket {} already exists", lifecyclePolicy, bucketName);
        }

        initializedPolicyCache.add(cacheKey);
    }

    private boolean isPolicyConfigured(GetBucketLifecycleConfigurationResponse getBucketLifecycleConfigurationResponse, LifecyclePolicy lifecyclePolicy) {
        if (getBucketLifecycleConfigurationResponse == null || (getBucketLifecycleConfigurationResponse.rules() == null)) {
            return false;
        }
        return getBucketLifecycleConfigurationResponse.rules().stream()
                .anyMatch(rule -> matchesById(rule, lifecyclePolicy));
    }

    /**
     * Creates or adds lifecycle policies for all known archive type definitions. Existing rules are preserved, only
     * rules created by this PAS are added if missing. This is to ensure that manually created rules are not
     * overwritten. The full update of all PAS lifecycle rules ensures that the PAS does not suffer concurrency issues
     * when multiple instances or threads are reading/updating/writing rules at the same time.
     */
    private void updateLifecyclePoliciesOnBucket(GetBucketLifecycleConfigurationResponse getBucketLifecycleConfigurationResponse, String bucketName) {
        BucketLifecycleConfiguration updatedConfiguration = lifecycleConfigurationFactory.createOrUpdateBucketLifecycleConfiguration(getBucketLifecycleConfigurationResponse);
        log.info("Setting lifecycle config for bucket {}", value("bucketName", bucketName));
        s3Client.putBucketLifecycleConfiguration(PutBucketLifecycleConfigurationRequest.builder()
                .bucket(bucketName)
                .lifecycleConfiguration(updatedConfiguration)
                .build());
    }

    private boolean matchesById(LifecycleRule rule, LifecyclePolicy lifecyclePolicy) {
        return rule != null && rule.id() != null &&
                rule.id().equals(ruleId(lifecyclePolicy));
    }

    /**
     * ID for a lifecycle configuration rule. Based on the name of the archive data type, and its expiration days
     * configuration. If the expiration days are ever changed for an archive data type, the existing rules will thus
     * be untouched, and only archive data types created from that point in time on will be managed under the new policy.
     */
    public static String ruleId(LifecyclePolicy lifecyclePolicy) {
        return archiveTypeNameWithExpiration(lifecyclePolicy);
    }

    /**
     * Adds a tag for the archive type name that the lifecycle policy will consume and use to delete data after
     * the expiration period. Based on the name of the archive data type, and its expiration days
     * configuration. If the expiration days are ever changed for an archive data type, the existing rules will thus
     * be untouched, and only archive data types created from that point in time on will be managed under the new policy.
     * <p>
     * See <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/object-tagging.html#tagging-and-policies">Tagging and policies</a>
     */
    public static Tag lifecyclePolicyTag(LifecyclePolicy lifecyclePolicy) {
        String policyTagValue = archiveTypeNameWithExpiration(lifecyclePolicy);
        return Tag.builder()
                .key(TAG_ARCHIVE_TYPE_POLICY)
                .value(policyTagValue)
                .build();
    }

    private static String archiveTypeNameWithExpiration(LifecyclePolicy lifecyclePolicy) {
        return lifecyclePolicy.getSystemName() + "_" +
                lifecyclePolicy.getArchiveTypeName() + "_" +
                lifecyclePolicy.getCurrentVersionExpirationDays() + "_" +
                lifecyclePolicy.getPreviousVersionExpirationDays();
    }

    @Value(staticConstructor = "of")
    static class LifecyclePolicyOnBucket {

        LifecyclePolicy policy;
        String bucketName;
    }
}

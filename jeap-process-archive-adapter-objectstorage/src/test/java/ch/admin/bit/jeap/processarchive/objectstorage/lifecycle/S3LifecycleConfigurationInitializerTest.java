package ch.admin.bit.jeap.processarchive.objectstorage.lifecycle;

import ch.admin.bit.jeap.processarchive.domain.archive.lifecycle.LifecyclePolicy;
import ch.admin.bit.jeap.processarchive.domain.archive.lifecycle.LifecyclePolicyService;
import ch.admin.bit.jeap.processarchive.objectstorage.TimedS3Client;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.s3.model.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mock.Strictness.LENIENT;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class S3LifecycleConfigurationInitializerTest {

    private static final LifecyclePolicy POLICY = LifecyclePolicy.builder()
            .systemName("sys")
            .archiveTypeName("type")
            .currentVersionExpirationDays(20)
            .previousVersionExpirationDays(30)
            .retainDays(25)
            .build();
    private static final LifecyclePolicy POLICY_2 = LifecyclePolicy.builder()
            .systemName("sys2")
            .archiveTypeName("type2")
            .currentVersionExpirationDays(40)
            .previousVersionExpirationDays(50)
            .retainDays(45)
            .build();
    private static final String BUCKET = "bucket";

    @Mock(strictness = LENIENT)
    private LifecyclePolicyService lifecyclePolicyService;
    @Mock
    private TimedS3Client s3client;
    @Captor
    private ArgumentCaptor<PutBucketLifecycleConfigurationRequest> configCaptor;
    private S3LifecycleConfigurationInitializer initializer;

    @BeforeEach
    void beforeEach() {
        S3LifecycleConfigurationFactory configurationFactory = new S3LifecycleConfigurationFactory(lifecyclePolicyService);
        initializer = new S3LifecycleConfigurationInitializer(configurationFactory, s3client);
        doReturn(List.of(POLICY, POLICY_2)).when(lifecyclePolicyService).getAllLifecyclePolicies();
    }

    @Test
    void ensureLifecyclePolicyPresent_noPolicyPresent() {
        initializer.ensureLifecyclePolicyPresent(BUCKET, POLICY);

        verify(s3client).getBucketLifecycleConfiguration(GetBucketLifecycleConfigurationRequest.builder().bucket(BUCKET).build());
        verify(s3client).putBucketLifecycleConfiguration(configCaptor.capture());
        verifyNoMoreInteractions(s3client);
        List<LifecycleRule> rules = configCaptor.getValue().lifecycleConfiguration().rules();
        assertEquals(2, rules.size());
        LifecycleRule rule1 = rules.get(0);
        assertEquals("sys_type_20_30", rule1.id());
        assertEquals(ExpirationStatus.ENABLED, rule1.status());
        assertEquals(20, rule1.expiration().days());
        assertEquals(30, rule1.noncurrentVersionExpiration().noncurrentDays());
        LifecycleRule rule2 = rules.get(1);
        assertEquals("sys2_type2_40_50", rule2.id());
        assertEquals(ExpirationStatus.ENABLED, rule2.status());
        assertEquals(40, rule2.expiration().days());
        assertEquals(50, rule2.noncurrentVersionExpiration().noncurrentDays());
    }

    @Test
    void ensureLifecyclePolicyPresent_noPoliciesPresentOnBucket() {
        AwsServiceException ex = S3Exception.builder()
                .statusCode(404)
                .build();
        doThrow(ex).when(s3client).getBucketLifecycleConfiguration(GetBucketLifecycleConfigurationRequest.builder().bucket(BUCKET).build());

        initializer.ensureLifecyclePolicyPresent(BUCKET, POLICY);

        verify(s3client).putBucketLifecycleConfiguration(configCaptor.capture());
        verifyNoMoreInteractions(s3client);
        List<LifecycleRule> rules = configCaptor.getValue().lifecycleConfiguration().rules();
        assertEquals(2, rules.size());
        LifecycleRule rule1 = rules.get(0);
        assertEquals("sys_type_20_30", rule1.id());
        assertEquals(ExpirationStatus.ENABLED, rule1.status());
        assertEquals(20, rule1.expiration().days());
        assertEquals(30, rule1.noncurrentVersionExpiration().noncurrentDays());
        LifecycleRule rule2 = rules.get(1);
        assertEquals("sys2_type2_40_50", rule2.id());
        assertEquals(ExpirationStatus.ENABLED, rule2.status());
        assertEquals(40, rule2.expiration().days());
        assertEquals(50, rule2.noncurrentVersionExpiration().noncurrentDays());
    }

    @Test
    void ensureLifecyclePolicyPresent_updatePolicy() {
        S3LifecycleConfigurationFactory configurationFactory = new S3LifecycleConfigurationFactory(lifecyclePolicyService);
        S3LifecycleConfigurationInitializer initializer = new S3LifecycleConfigurationInitializer(configurationFactory, s3client);
        LifecycleRule existingRule1 = LifecycleRule.builder()
                .id("existing-id").build();
        LifecycleRule existingRule2 = LifecycleRule.builder()
                .id("sys2_type2_40_50").build();

        GetBucketLifecycleConfigurationResponse existingConfig = GetBucketLifecycleConfigurationResponse.builder()
                .rules(existingRule1, existingRule2)
                .build();

        doReturn(existingConfig).when(s3client).getBucketLifecycleConfiguration(GetBucketLifecycleConfigurationRequest.builder().bucket(BUCKET).build());

        initializer.ensureLifecyclePolicyPresent(BUCKET, POLICY);

        verify(s3client).putBucketLifecycleConfiguration(configCaptor.capture());
        verifyNoMoreInteractions(s3client);
        PutBucketLifecycleConfigurationRequest putBucketLifecycleConfigurationRequest = configCaptor.getValue();
        assertEquals("existing-id", putBucketLifecycleConfigurationRequest.lifecycleConfiguration().rules().get(0).id(),
                "Existing rule is untouched");
        assertEquals("sys2_type2_40_50", putBucketLifecycleConfigurationRequest.lifecycleConfiguration().rules().get(1).id(),
                "Existing rule with matching lifecycle policy is untouched");
        assertEquals("sys_type_20_30", putBucketLifecycleConfigurationRequest.lifecycleConfiguration().rules().get(2).id(),
                "New rule is added");
    }

    @Test
    void ensureLifecyclePolicyPresent_policyExists() {
        S3LifecycleConfigurationFactory configurationFactory = new S3LifecycleConfigurationFactory(lifecyclePolicyService);
        S3LifecycleConfigurationInitializer initializer = new S3LifecycleConfigurationInitializer(configurationFactory, s3client);
        GetBucketLifecycleConfigurationResponse existingConfig = GetBucketLifecycleConfigurationResponse.builder()
                .rules(LifecycleRule.builder().id("sys_type_20_30").build())
                .build();
        doReturn(existingConfig).when(s3client).getBucketLifecycleConfiguration(GetBucketLifecycleConfigurationRequest.builder().bucket(BUCKET).build());
        initializer.ensureLifecyclePolicyPresent(BUCKET, POLICY);
        verifyNoMoreInteractions(s3client);
    }

    @Test
    void ensureLifecyclePolicyPresent_twoHits_cacheAvoidsSecondUpdate() {
        initializer.ensureLifecyclePolicyPresent(BUCKET, POLICY);
        initializer.ensureLifecyclePolicyPresent(BUCKET, POLICY);

        verify(s3client).getBucketLifecycleConfiguration(GetBucketLifecycleConfigurationRequest.builder().bucket(BUCKET).build());
        verify(s3client).putBucketLifecycleConfiguration(configCaptor.capture());
        verifyNoMoreInteractions(s3client);
    }

    @Test
    void ruleId() {
        assertEquals("sys_type_20_30", S3LifecycleConfigurationInitializer.ruleId(POLICY));
    }

    @Test
    void lifecyclePolicyTag() {
        Tag tag = S3LifecycleConfigurationInitializer.lifecyclePolicyTag(POLICY);
        assertEquals("ArchiveTypeLifecyclePolicy", tag.key());
        assertEquals("sys_type_20_30", tag.value());
    }
}

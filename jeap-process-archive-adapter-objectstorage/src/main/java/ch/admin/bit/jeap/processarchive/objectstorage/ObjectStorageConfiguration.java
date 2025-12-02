package ch.admin.bit.jeap.processarchive.objectstorage;

import ch.admin.bit.jeap.processarchive.crypto.ArchiveCryptoService;
import ch.admin.bit.jeap.processarchive.domain.archive.lifecycle.LifecyclePolicyService;
import ch.admin.bit.jeap.processarchive.objectstorage.lifecycle.S3LifecycleConfigurationFactory;
import ch.admin.bit.jeap.processarchive.objectstorage.lifecycle.S3LifecycleConfigurationInitializer;
import ch.admin.bit.jeap.processarchive.plugin.api.storage.HashProvider;
import ch.admin.bit.jeap.processarchive.plugin.api.storage.ObjectStorageStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.PropertySource;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;

@Slf4j
@AutoConfiguration
@PropertySource("classpath:pas-object-storage-defaults.properties")
@EnableConfigurationProperties({ObjectStorageProperties.class, DefaultObjectStorageStrategyProperties.class, S3ObjectStorageConnectionProperties.class})
public class ObjectStorageConfiguration {

    public static final String JEAP_PAS_TEST_INMEMORY_PROFILE = "jeap-pas-test-objectstorage-inmemory";
    public static final String JEAP_PAS_S3_STORAGE_PROFILE = "!" + JEAP_PAS_TEST_INMEMORY_PROFILE;

    @Bean
    @ConditionalOnMissingBean
    public ObjectStorageStrategy objectStorageStrategy(DefaultObjectStorageStrategyProperties defaultObjectStorageStrategyProperties,
                                                       HashProvider hashProvider) {
        return new DefaultObjectStorageStrategy(defaultObjectStorageStrategyProperties, hashProvider);
    }

    @Profile(JEAP_PAS_S3_STORAGE_PROFILE)
    @Bean
    public ObjectStorageRepository objectStorageRepository(ObjectStorageProperties objectStorageProperties,
                                                           S3LifecycleConfigurationInitializer lifecycleConfigurationInitializer,
                                                           ArchiveCryptoService archiveCryptoService,
                                                           TimedS3Client s3Client) {
        log.info("Setting object storage up with S3 object storage repository.");
        return new S3ObjectStorageRepository(objectStorageProperties, lifecycleConfigurationInitializer, archiveCryptoService, s3Client);
    }

    @Profile(JEAP_PAS_TEST_INMEMORY_PROFILE)
    @Bean
    public ObjectStorageRepository testObjectStorageRepository() {
        log.info("Objectstorage test profile active: Setting object storage up with an in-memory repository.");
        return new InMemoryObjectStorageRepository();
    }

    @Bean
    public ArchiveDataObjectStoreAdapter archiveDataObjectStoreAdapter(ObjectStorageRepository objectStorageRepository,
                                                                       ObjectStorageStrategy objectStorageStrategy,
                                                                       LifecyclePolicyService lifecyclePolicyService,
                                                                       ObjectStorageProperties props,
                                                                       HashProvider hashProvider) {
        return new ArchiveDataObjectStoreAdapter(objectStorageRepository, objectStorageStrategy, lifecyclePolicyService, props, hashProvider);
    }

    @Profile(JEAP_PAS_S3_STORAGE_PROFILE)
    @Bean
    public S3LifecycleConfigurationFactory lifecycleConfigurationFactory(LifecyclePolicyService lifecyclePolicyService) {
        return new S3LifecycleConfigurationFactory(lifecyclePolicyService);
    }

    @Profile(JEAP_PAS_S3_STORAGE_PROFILE)
    @Bean
    public S3LifecycleConfigurationInitializer s3LifecycleConfigurationInitializer(S3LifecycleConfigurationFactory s3LifecycleConfigurationFactory, TimedS3Client s3Client) {
        return new S3LifecycleConfigurationInitializer(s3LifecycleConfigurationFactory, s3Client);
    }

    @Profile(JEAP_PAS_S3_STORAGE_PROFILE)
    @Bean
    public TimedS3Client timedS3Client(S3ObjectStorageConnectionProperties connectionProperties, AwsCredentialsProvider awsCredentialsProvider) {
        return new TimedS3Client(connectionProperties, awsCredentialsProvider);
    }

    @Profile(JEAP_PAS_S3_STORAGE_PROFILE)
    @Bean
    @ConditionalOnMissingBean(AwsCredentialsProvider.class)
    DefaultCredentialsProvider awsCredentialsProvider() {
        return DefaultCredentialsProvider.create();
    }
}

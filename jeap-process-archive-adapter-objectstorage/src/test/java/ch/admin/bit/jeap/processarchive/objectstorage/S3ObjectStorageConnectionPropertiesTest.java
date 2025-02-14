package ch.admin.bit.jeap.processarchive.objectstorage;

import ch.admin.bit.jeap.processarchive.domain.archive.lifecycle.LifecyclePolicyService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import software.amazon.awssdk.regions.Region;

import static ch.admin.bit.jeap.processarchive.objectstorage.ObjectStorageConfiguration.JEAP_PAS_TEST_INMEMORY_PROFILE;
import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles(JEAP_PAS_TEST_INMEMORY_PROFILE)
@SpringBootTest(
        classes = {ObjectStorageConfiguration.class, HashProviderTestConfig.class},
        properties = {
                "jeap.processarchive.objectstorage.storage.bucket=testbucket",
                "spring.cloud.vault.enabled=false",
                "AWS_REGION=eu-central-2"
        })
class S3ObjectStorageConnectionPropertiesTest {

    @MockitoBean
    LifecyclePolicyService lifecyclePolicyService;

    @Autowired
    S3ObjectStorageConnectionProperties s3ObjectStorageConnectionProperties;

    @Test
    void testRegion() {
        assertThat(s3ObjectStorageConnectionProperties.region())
                .isEqualTo(Region.EU_CENTRAL_2);
    }
}

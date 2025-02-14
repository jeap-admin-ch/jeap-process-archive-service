package ch.admin.bit.jeap.processarchive.objectstorage;

import ch.admin.bit.jeap.processarchive.crypto.ArchiveCryptoService;
import ch.admin.bit.jeap.processarchive.domain.archive.lifecycle.LifecyclePolicy;
import ch.admin.bit.jeap.processarchive.objectstorage.lifecycle.S3LifecycleConfigurationInitializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.s3.model.ObjectLockMode;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class S3ObjectStorageRepositoryTest {

    private S3ObjectStorageRepository s3ObjectStorageRepository;

    private ObjectStorageProperties objectStorageProperties;

    @Mock
    private TimedS3Client s3Client;

    @Mock
    private LifecyclePolicy lifecyclePolicy;

    @BeforeEach
    void setUp() {
        objectStorageProperties = new ObjectStorageProperties();
        s3ObjectStorageRepository = new S3ObjectStorageRepository(objectStorageProperties, mock(S3LifecycleConfigurationInitializer.class), mock(ArchiveCryptoService.class), s3Client);
        doReturn(PutObjectResponse.builder().build()).when(s3Client).putObject(any(), any());
    }

    @Test
    void putObjectWithDisabledObjectLockMode() {
        objectStorageProperties.setObjectLockEnabled(false);
        s3ObjectStorageRepository.putObjectWithLifecyclePolicy("bucket", "key", "test".getBytes(), Collections.emptyMap(), lifecyclePolicy, null);

        ArgumentCaptor<PutObjectRequest> argumentCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client).putObject(argumentCaptor.capture(), any());
        PutObjectRequest putObjectRequest = argumentCaptor.getValue();
        assertNull(putObjectRequest.objectLockMode());
        assertNull(putObjectRequest.objectLockRetainUntilDate());
    }

    @Test
    void putObjectWithDefaultObjectLockMode() {
        doReturn(10).when(lifecyclePolicy).getRetainDays();
        s3ObjectStorageRepository.putObjectWithLifecyclePolicy("bucket", "key", "test".getBytes(), Collections.emptyMap(), lifecyclePolicy, null);

        ArgumentCaptor<PutObjectRequest> argumentCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client).putObject(argumentCaptor.capture(), any());
        PutObjectRequest putObjectRequest = argumentCaptor.getValue();
        assertEquals(ObjectLockMode.COMPLIANCE, putObjectRequest.objectLockMode());
        assertNotNull(putObjectRequest.objectLockRetainUntilDate());
    }

    @Test
    void putObjectWithCustomizedObjectLockMode() {
        doReturn(10).when(lifecyclePolicy).getRetainDays();
        objectStorageProperties.setObjectLockMode(ObjectLockMode.GOVERNANCE.toString());
        s3ObjectStorageRepository.putObjectWithLifecyclePolicy("bucket", "key", "test".getBytes(), Collections.emptyMap(), lifecyclePolicy, null);

        ArgumentCaptor<PutObjectRequest> argumentCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client).putObject(argumentCaptor.capture(), any());
        PutObjectRequest putObjectRequest = argumentCaptor.getValue();
        assertEquals(ObjectLockMode.GOVERNANCE, putObjectRequest.objectLockMode());
        assertNotNull(putObjectRequest.objectLockRetainUntilDate());
    }
}

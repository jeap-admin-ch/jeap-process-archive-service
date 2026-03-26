package ch.admin.bit.jeap.processarchive.objectstorage;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import software.amazon.awssdk.services.s3.model.ObjectLockMode;


@Getter
@ToString
@ConfigurationProperties("jeap.processarchive.objectstorage")
public class ObjectStorageProperties {

    @Setter
    private boolean objectLockEnabled = true;

    /**
     * Controls whether existing schema files for an archive data schema in object storage are allowed to be overwritten
     * if their content does not match the schema that the PAS is using at the moment.
     */
    @Setter
    private boolean schemaOverwriteAllowed = false;

    private ObjectLockMode objectLockMode = ObjectLockMode.COMPLIANCE;

    public void setObjectLockMode(String objectLockMode) {
        this.objectLockMode = ObjectLockMode.fromValue(objectLockMode);
    }
}

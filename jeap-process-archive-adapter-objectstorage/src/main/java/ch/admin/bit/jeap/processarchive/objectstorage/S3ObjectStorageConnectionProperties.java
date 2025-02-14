package ch.admin.bit.jeap.processarchive.objectstorage;

import lombok.Data;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import software.amazon.awssdk.regions.Region;


@Data
@ToString
@ConfigurationProperties("jeap.processarchive.objectstorage.connection")

public class S3ObjectStorageConnectionProperties {

    private String accessUrl;

    private String region;

    // excluded from toString for security reasons
    @ToString.Exclude
    private String accessKey;

    // excluded from toString for security reasons
    @ToString.Exclude
    private String secretKey;

    public Region region() {
        return Region.of(region);
    }
}

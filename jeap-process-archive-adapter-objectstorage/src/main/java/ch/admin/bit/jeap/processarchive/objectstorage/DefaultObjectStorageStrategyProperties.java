package ch.admin.bit.jeap.processarchive.objectstorage;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ToString
@ConfigurationProperties("jeap.processarchive.objectstorage.storage")
public class DefaultObjectStorageStrategyProperties {

    private String bucket;
    private PrefixMode prefixMode = PrefixMode.DAY;

}

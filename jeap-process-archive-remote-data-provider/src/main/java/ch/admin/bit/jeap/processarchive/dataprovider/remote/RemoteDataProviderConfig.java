package ch.admin.bit.jeap.processarchive.dataprovider.remote;

import lombok.Data;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;

import java.time.Duration;

@AutoConfiguration
@ConfigurationProperties(prefix = "jeap.processarchive.http")
@ComponentScan
@Data
class RemoteDataProviderConfig {
    private Duration timeout = Duration.ofSeconds(30);
}

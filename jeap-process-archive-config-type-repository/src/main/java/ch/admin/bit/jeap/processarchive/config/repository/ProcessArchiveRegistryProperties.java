package ch.admin.bit.jeap.processarchive.config.repository;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "jeap.processarchive.registry")
@Getter
@Setter
public class ProcessArchiveRegistryProperties {
    private List<ArchiveTypeDefinition> types = new ArrayList<>();
}

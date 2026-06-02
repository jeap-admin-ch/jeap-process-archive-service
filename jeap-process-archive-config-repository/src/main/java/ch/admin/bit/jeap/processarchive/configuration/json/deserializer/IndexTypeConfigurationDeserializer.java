package ch.admin.bit.jeap.processarchive.configuration.json.deserializer;

import ch.admin.bit.jeap.processarchive.domain.configuration.IndexTypeConfiguration;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

import static org.springframework.util.StringUtils.hasText;

@Component
@RequiredArgsConstructor
public class IndexTypeConfigurationDeserializer {

    public List<IndexTypeConfiguration> toConfiguration(List<ch.admin.bit.jeap.processarchive.configuration.json.model.IndexTypeConfiguration> configs) {
        return configs.stream().map(this::toIndexTypeConfiguration).toList();
    }

    private IndexTypeConfiguration toIndexTypeConfiguration(ch.admin.bit.jeap.processarchive.configuration.json.model.IndexTypeConfiguration config) {
        String indexType = config.getIndexType();
        String archiveType = config.getArchiveType();
        String archiveTypeToSearchItemConverter = config.getArchiveTypeToSearchItemConverter();

        if (!hasText(indexType)) {
            throw IndexTypeConfigurationException.emptyIndexType();
        }

        if (!hasText(archiveType)) {
            throw IndexTypeConfigurationException.emptyArchiveType(indexType);
        }

        if (!hasText(archiveTypeToSearchItemConverter)) {
            throw IndexTypeConfigurationException.emptyArchiveTypeToSearchItemConverter(indexType);
        }

        return new IndexTypeConfiguration(
                indexType,
                Instances.toClass(archiveType),
                Instances.newInstance(archiveTypeToSearchItemConverter)
        );
    }

}

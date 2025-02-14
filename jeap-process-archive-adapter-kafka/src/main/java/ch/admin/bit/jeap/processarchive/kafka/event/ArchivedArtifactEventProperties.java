package ch.admin.bit.jeap.processarchive.kafka.event;

import jakarta.annotation.PostConstruct;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Set;
import java.util.stream.Collectors;

@ConfigurationProperties(prefix = "jeap.processarchive.archivedartifact")
@Data
@Slf4j
@RequiredArgsConstructor
public class ArchivedArtifactEventProperties {

    private boolean enabled = true;
    @NotEmpty
    private String systemId;
    @NotEmpty
    private String eventTopic;
    @NotEmpty
    private String systemName;
    @NotEmpty
    private String serviceName;

    @PostConstruct
    void validateConfiguration() {
        if (!enabled) {
            log.info("Publishing SharedArchivedArtifactVersionCreatedEvent is not enabled");
            return;
        }

        validateProperties();

        log.info(this.toString());
    }

    private void validateProperties() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            Validator validator = factory.getValidator();
            Set<ConstraintViolation<ArchivedArtifactEventProperties>> violations = validator.validate(this);
            if (!violations.isEmpty()) {
                String errorMessage = violations.stream()
                        .map(ConstraintViolation::toString)
                        .collect(Collectors.joining(", "));
                throw new IllegalStateException(errorMessage);
            }
        }
    }
}

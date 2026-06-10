package ch.admin.bit.jeap.processarchive.kafka.backfill;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Set;
import java.util.stream.Collectors;

@ConfigurationProperties(prefix = "jeap.processarchive.backfill.command")
@Data
@Slf4j
public class BackfillCommandProperties {

    @NotEmpty
    private String topic;

    @NotEmpty
    private String systemName;

    @NotEmpty
    private String serviceName;

    public void validateConfiguration() {
        validateProperties();
        log.info(this.toString());
    }

    private void validateProperties() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            Validator validator = factory.getValidator();
            Set<ConstraintViolation<BackfillCommandProperties>> violations = validator.validate(this);
            if (!violations.isEmpty()) {
                String errorMessage = violations.stream()
                        .map(ConstraintViolation::toString)
                        .collect(Collectors.joining(", "));
                throw new IllegalStateException(errorMessage);
            }
        }
    }
}

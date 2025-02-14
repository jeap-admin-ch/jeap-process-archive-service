package ch.admin.bit.jeap.processarchive.dataprovider.remote;


import lombok.*;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Builder
class TemplateUriParametersValidator {

    @NonNull
    final String template;

    @NonNull
    @Singular("expectedParameter")
    final Set<String> expectedParameters;

    ValidationResult validate() {
        Set<String> missingParameters = new HashSet<>(expectedParameters);
        Set<String> additionalParameters = new HashSet<>();

        Pattern pattern = Pattern.compile("\\{([^}]+)}");
        Matcher matcher = pattern.matcher(template);
        while (matcher.find()) {
            String foundParam = matcher.group(1);
            if (!missingParameters.remove(foundParam)) {
                additionalParameters.add(foundParam);
            }
        }

        return new ValidationResult(missingParameters, additionalParameters);
    }

    @Getter
    @RequiredArgsConstructor
    static class ValidationResult {

        private final Set<String> missingParameters;
        private final Set<String> additionalParameters;

        boolean isValid() {
            return missingParameters.isEmpty() && additionalParameters.isEmpty();
        }
    }

}

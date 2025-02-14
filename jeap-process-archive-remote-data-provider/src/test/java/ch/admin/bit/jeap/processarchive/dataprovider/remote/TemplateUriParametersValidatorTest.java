package ch.admin.bit.jeap.processarchive.dataprovider.remote;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TemplateUriParametersValidatorTest {

    @Test
    void testValidate_WhenParametersMatch_thenIsValid() {
        TemplateUriParametersValidator.ValidationResult result = TemplateUriParametersValidator.builder()
                .template("test-data/{id}?{version}")
                .expectedParameter("id")
                .expectedParameter("version")
                .build()
                .validate();

        assertThat(result.isValid()).isTrue();
        assertThat(result.getMissingParameters()).isEmpty();
        assertThat(result.getAdditionalParameters()).isEmpty();
    }

    @Test
    void testValidate_WhenAdditionalTemplateParameter_thenIsNotValidWithAdditionalParameter() {
        TemplateUriParametersValidator.ValidationResult result = TemplateUriParametersValidator.builder()
                .template( "test-data/{id}?{version}")
                .expectedParameter("id")
                .build()
                .validate();

        assertThat(result.isValid()).isFalse();
        assertThat(result.getMissingParameters()).isEmpty();
        assertThat(result.getAdditionalParameters()).containsOnly("version");
    }

    @Test
    void testValidate_WhenMissingTemplateParameter_thenIsNotValidWithMissingParameter() {
        TemplateUriParametersValidator.ValidationResult result = TemplateUriParametersValidator.builder()
                .template( "test-data/{id}")
                .expectedParameter("id")
                .expectedParameter("version")
                .build()
                .validate();

        assertThat(result.isValid()).isFalse();
        assertThat(result.getMissingParameters()).containsOnly("version");
        assertThat(result.getAdditionalParameters()).isEmpty();
    }

}

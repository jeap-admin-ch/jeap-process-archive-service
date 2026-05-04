package ch.admin.bit.jeap.processarchive.registry.verifier.archivetype;

import ch.admin.bit.jeap.processarchive.registry.verifier.ValidationContext;
import ch.admin.bit.jeap.processarchive.registry.verifier.common.ValidationResult;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.File;
import java.io.IOException;
import java.util.stream.Stream;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArchiveTypeDescriptorSchemaValidatorTest {
    @ParameterizedTest(name = "{0}")
    @MethodSource("schemaValidationScenarios")
    void schemaValidationCases(String name, String descriptorContent, boolean expectedValid, @TempDir File tmpDir) throws IOException {
        ValidationResult result = validateDescriptor(tmpDir, descriptorContent);

        if (expectedValid) {
            assertTrue(result.isValid(), String.join(",", result.getErrors()));
        } else {
            assertFalse(result.isValid());
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("mutualExclusionErrorScenarios")
    void mutualExclusionErrorCases(String name, String descriptorContent, @TempDir File tmpDir) throws IOException {
        ValidationResult result = validateDescriptor(tmpDir, descriptorContent);

        assertFalse(result.isValid());
        assertThat(result.getErrors())
                .anyMatch(str -> str.contains("must not be valid to the schema")
                        && str.contains("\"required\":[\"encryption\",\"encryptionKey\"]"));
    }

    private ValidationResult validateDescriptor(File tmpDir, String descriptorContent) throws IOException {
        File file = new File(tmpDir, "test");
        FileUtils.write(file, descriptorContent, UTF_8);
        ValidationContext validationContext = ValidationContext.builder()
                .descriptor(file)
                .build();
        return ArchiveTypeDescriptorSchemaValidator.validate(validationContext);
    }

    private static Stream<Arguments> schemaValidationScenarios() {
        return Stream.of(
                Arguments.of("valid schema", """
                        {
                          "archiveType": "Decree",
                          "system": "JME",
                          "referenceIdType": "ch.admin.bit.jeap.audit.type.JmeDecreeArchive",
                          "description": "archive type example for a decree",
                          "documentationUrl": "https://foo/bar",
                          "expirationDays": 1,
                          "versions": [
                            {
                              "version": 1,
                              "schema": "decree_v1.avdl"
                            },
                            {
                              "version": 2,
                              "schema": "decree_v2.avdl",
                              "compatibilityMode": "BACKWARD"
                            }
                          ]
                        }
                        """, true),
                Arguments.of("invalid schema", "{\"archive-type\": \"Decree\"}", false),
                Arguments.of("not json", "Something", false),
                Arguments.of("valid schema with encryption", """
                        {
                          "archiveType": "Decree",
                          "system": "JME",
                          "referenceIdType": "ch.admin.bit.jeap.audit.type.JmeDecreeArchive",
                          "description": "archive type example for a decree",
                          "documentationUrl": "https://foo/bar",
                          "expirationDays": 1,
                          "encryption": {
                            "secretEnginePath": "transit/jme",
                            "keyName": "jme-process-archive-example-s3-key"
                          },
                          "versions": [
                            {
                              "version": 1,
                              "schema": "decree_v1.avdl"
                            },
                            {
                              "version": 2,
                              "schema": "decree_v2.avdl",
                              "compatibilityMode": "BACKWARD"
                            }
                          ]
                        }
                        """, true),
                Arguments.of("invalid schema with encryption", """
                        {
                          "archiveType": "Decree",
                          "system": "JME",
                          "referenceIdType": "ch.admin.bit.jeap.audit.type.JmeDecreeArchive",
                          "description": "archive type example for a decree",
                          "documentationUrl": "https://foo/bar",
                          "expirationDays": 1,
                          "encryption": {
                            "secretEnginePath": "transit/jme"
                          },
                          "versions": [
                            {
                              "version": 1,
                              "schema": "decree_v1.avdl"
                            },
                            {
                              "version": 2,
                              "schema": "decree_v2.avdl",
                              "compatibilityMode": "BACKWARD"
                            }
                          ]
                        }
                        """, false),
                Arguments.of("valid schema with encryption key id", """
                        {
                          "archiveType": "Decree",
                          "system": "JME",
                          "referenceIdType": "ch.admin.bit.jeap.audit.type.JmeDecreeArchive",
                          "description": "archive type example for a decree",
                          "documentationUrl": "https://foo/bar",
                          "expirationDays": 1,
                          "encryptionKey": {
                            "keyId": "someKey"
                          },
                          "versions": [
                            {
                              "version": 1,
                              "schema": "decree_v1.avdl"
                            },
                            {
                              "version": 2,
                              "schema": "decree_v2.avdl",
                              "compatibilityMode": "BACKWARD"
                            }
                          ]
                        }
                        """, true)
        );
    }

    private static Stream<Arguments> mutualExclusionErrorScenarios() {
        return Stream.of(
                Arguments.of("invalid schema with both encryption key reference and id", """
                        {
                          "archiveType": "Decree",
                          "system": "JME",
                          "referenceIdType": "ch.admin.bit.jeap.audit.type.JmeDecreeArchive",
                          "description": "archive type example for a decree",
                          "documentationUrl": "https://foo/bar",
                          "expirationDays": 1,
                          "encryptionKey": {
                            "keyId": "someKey"
                          },
                          "encryption": {
                            "secretEnginePath": "transit/jme",
                            "keyName": "jme-process-archive-example-s3-key"
                          },
                          "versions": [
                            {
                              "version": 1,
                              "schema": "decree_v1.avdl"
                            },
                            {
                              "version": 2,
                              "schema": "decree_v2.avdl",
                              "compatibilityMode": "BACKWARD"
                            }
                          ]
                        }
                        """));
    }
}

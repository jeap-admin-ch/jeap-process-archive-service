package ch.admin.bit.jeap.processarchive.registry.verifier.archivetype;

import ch.admin.bit.jeap.processarchive.registry.verifier.ValidationContext;
import ch.admin.bit.jeap.processarchive.registry.verifier.common.ValidationResult;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArchiveTypeDescriptorSchemaValidatorTest {
    @Test
    void validSchema(@TempDir File tmpDir) throws IOException {
        File file = new File(tmpDir, "test");
        FileUtils.write(file, """
                {\
                  "archiveType": "Decree",\
                  "system": "JME",\
                 "referenceIdType": "ch.admin.bit.jeap.audit.type.JmeDecreeArchive",\
                  "description": "archive type example for a decree",\
                  "documentationUrl": "https://foo/bar",\
                  "expirationDays": 1,\
                  "versions": [\
                    {\
                      "version": 1,\
                      "schema": "decree_v1.avdl"\
                    },\
                    {\
                      "version": 2,\
                      "schema": "decree_v2.avdl",\
                      "compatibilityMode": "BACKWARD"
                    }\
                  ]\
                }\
                """, UTF_8);
        ValidationContext validationContext = ValidationContext.builder()
                .descriptor(file)
                .build();

        ValidationResult result = ArchiveTypeDescriptorSchemaValidator.validate(validationContext);

        assertTrue(result.isValid(), String.join(",", result.getErrors()));
    }

    @Test
    void invalidSchema(@TempDir File tmpDir) throws IOException {
        File file = new File(tmpDir, "test");
        FileUtils.write(file, "{\"archive-type\": \"Decree\"}", UTF_8);
        ValidationContext validationContext = ValidationContext.builder()
                .descriptor(file)
                .build();

        ValidationResult result = ArchiveTypeDescriptorSchemaValidator.validate(validationContext);

        assertFalse(result.isValid(), "Archive type descriptor does not confirm to schema");
    }

    @Test
    void notJson(@TempDir File tmpDir) throws IOException {
        File file = new File(tmpDir, "test");
        FileUtils.write(file, "Something", UTF_8);
        ValidationContext validationContext = ValidationContext.builder()
                .descriptor(file)
                .build();

        ValidationResult result = ArchiveTypeDescriptorSchemaValidator.validate(validationContext);

        assertFalse(result.isValid(), "Archive type descriptor is not valid json");
    }

    @Test
    void validSchemaWithEncryption(@TempDir File tmpDir) throws IOException {
        File file = new File(tmpDir, "test");
        FileUtils.write(file, """
                {\
                  "archiveType": "Decree",\
                  "system": "JME",\
                 "referenceIdType": "ch.admin.bit.jeap.audit.type.JmeDecreeArchive",\
                  "description": "archive type example for a decree",\
                  "documentationUrl": "https://foo/bar",\
                  "expirationDays": 1,\
                  "encryption" : {\
                  "secretEnginePath": "transit/jme",\
                  "keyName": "jme-process-archive-example-s3-key"\
                  },\
                  "versions": [\
                    {\
                      "version": 1,\
                      "schema": "decree_v1.avdl"\
                    },\
                    {\
                      "version": 2,\
                      "schema": "decree_v2.avdl",\
                      "compatibilityMode": "BACKWARD"
                    }\
                  ]\
                }\
                """, UTF_8);
        ValidationContext validationContext = ValidationContext.builder()
                .descriptor(file)
                .build();

        ValidationResult result = ArchiveTypeDescriptorSchemaValidator.validate(validationContext);

        assertTrue(result.isValid(), String.join(",", result.getErrors()));
    }

    @Test
    void invalidSchemaWithEncryption(@TempDir File tmpDir) throws IOException {
        File file = new File(tmpDir, "test");
        FileUtils.write(file, """
                {\
                  "archiveType": "Decree",\
                  "system": "JME",\
                 "referenceIdType": "ch.admin.bit.jeap.audit.type.JmeDecreeArchive",\
                  "description": "archive type example for a decree",\
                  "documentationUrl": "https://foo/bar",\
                  "expirationDays": 1,\
                  "encryption" : {\
                  "secretEnginePath": "transit/jme",\
                  },\
                  "versions": [\
                    {\
                      "version": 1,\
                      "schema": "decree_v1.avdl"\
                    },\
                    {\
                      "version": 2,\
                      "schema": "decree_v2.avdl",\
                      "compatibilityMode": "BACKWARD"
                    }\
                  ]\
                }\
                """, UTF_8);
        ValidationContext validationContext = ValidationContext.builder()
                .descriptor(file)
                .build();

        ValidationResult result = ArchiveTypeDescriptorSchemaValidator.validate(validationContext);

        assertFalse(result.isValid(), "Archive type descriptor is not valid json");
    }

    @Test
    void validSchemaWithEncryptionKeyId(@TempDir File tmpDir) throws IOException {
        File file = new File(tmpDir, "test");
        FileUtils.write(file, """
                {\
                  "archiveType": "Decree",\
                  "system": "JME",\
                 "referenceIdType": "ch.admin.bit.jeap.audit.type.JmeDecreeArchive",\
                  "description": "archive type example for a decree",\
                  "documentationUrl": "https://foo/bar",\
                  "expirationDays": 1,\
                  "encryptionKey" : {\
                  "keyId": "someKey"\
                  },\
                  "versions": [\
                    {\
                      "version": 1,\
                      "schema": "decree_v1.avdl"\
                    },\
                    {\
                      "version": 2,\
                      "schema": "decree_v2.avdl",\
                      "compatibilityMode": "BACKWARD"
                    }\
                  ]\
                }\
                """, UTF_8);
        ValidationContext validationContext = ValidationContext.builder()
                .descriptor(file)
                .build();

        ValidationResult result = ArchiveTypeDescriptorSchemaValidator.validate(validationContext);

        assertTrue(result.isValid(), String.join(",", result.getErrors()));
    }

    @Test
    void invalidSchemaWithBothEncryptionKeyReferenceAndId(@TempDir File tmpDir) throws IOException {
        File file = new File(tmpDir, "test");
        FileUtils.write(file, """
                {\
                  "archiveType": "Decree",\
                  "system": "JME",\
                 "referenceIdType": "ch.admin.bit.jeap.audit.type.JmeDecreeArchive",\
                  "description": "archive type example for a decree",\
                  "documentationUrl": "https://foo/bar",\
                  "expirationDays": 1,\
                  "encryptionKey" : {\
                  "keyId": "someKey"\
                  },\
                  "encryption" : {\
                  "secretEnginePath": "transit/jme",\
                  "keyName": "jme-process-archive-example-s3-key"\
                  },\
                  "versions": [\
                    {\
                      "version": 1,\
                      "schema": "decree_v1.avdl"\
                    },\
                    {\
                      "version": 2,\
                      "schema": "decree_v2.avdl",\
                      "compatibilityMode": "BACKWARD"
                    }\
                  ]\
                }\
                """, UTF_8);
        ValidationContext validationContext = ValidationContext.builder()
                .descriptor(file)
                .build();

        ValidationResult result = ArchiveTypeDescriptorSchemaValidator.validate(validationContext);

        assertFalse(result.isValid());
        assertThat(result.getErrors())
                .anyMatch(str -> str.contains("""
                        not" : {"anyOf":[{"required":["encryption","encryptionKey"]}]}"""));
    }
}

package ch.admin.bit.jeap.processarchive.registry.verifier.system;

import ch.admin.bit.jeap.processarchive.avro.plugin.compiler.ImportClassLoader;
import ch.admin.bit.jeap.processarchive.avro.plugin.registry.connector.ArchiveTypeRegistryConstants;
import ch.admin.bit.jeap.processarchive.registry.verifier.ValidationContext;
import ch.admin.bit.jeap.processarchive.registry.verifier.common.ValidationResult;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SystemCommonDirValidatorTest {
    private static ValidationContext validationContext(File systemDir) {
        return ValidationContext.builder()
                .importClassLoader(new ImportClassLoader())
                .systemDir(systemDir)
                .build();
    }

    @Test
    void noIdlFile(@TempDir File tmpDir) throws IOException {
        File commonSystemDir = new File(tmpDir, ArchiveTypeRegistryConstants.COMMON_DIR_NAME);
        FileUtils.forceMkdir(commonSystemDir);
        FileUtils.write(new File(commonSystemDir, "test.Test.avdl"), "Invalid Data", UTF_8);

        ValidationResult result = SystemCommonDirValidator.validate(validationContext(tmpDir));

        assertFalse(result.isValid(), "Common dir contained no IDL file, this is not allowed");
    }

    @Test
    void valid(@TempDir File tmpDir) throws IOException {
        File commonSystemDir = new File(tmpDir, ArchiveTypeRegistryConstants.COMMON_DIR_NAME);
        FileUtils.forceMkdir(commonSystemDir);
        FileUtils.write(new File(commonSystemDir, "test.Test.avdl"), """
                @namespace("test")
                protocol TestProtocol {
                  record Test {
                    string id;
                  }
                }
                """, UTF_8);

        ValidationResult result = SystemCommonDirValidator.validate(validationContext(tmpDir));

        assertTrue(result.isValid(), String.join(",", result.getErrors()));
    }

    @Test
    void wrongRecordName(@TempDir File tmpDir) throws IOException {
        File commonSystemDir = new File(tmpDir, ArchiveTypeRegistryConstants.COMMON_DIR_NAME);
        FileUtils.forceMkdir(commonSystemDir);
        FileUtils.write(new File(commonSystemDir, "test.Test.avdl"), """
                @namespace("test")
                protocol TestProtocol {
                  record TestWrong {
                    string id;
                  }
                }
                """, UTF_8);

        ValidationResult result = SystemCommonDirValidator.validate(validationContext(tmpDir));

        assertFalse(result.isValid(), "Common dir contained a file with a record with a wrong name, this is not allowed");
    }

    @Test
    void wrongProtocolName(@TempDir File tmpDir) throws IOException {
        File commonSystemDir = new File(tmpDir, ArchiveTypeRegistryConstants.COMMON_DIR_NAME);
        FileUtils.forceMkdir(commonSystemDir);
        FileUtils.write(new File(commonSystemDir, "test.Test.avdl"), """
                @namespace("test")
                protocol TestWrongProtocol {
                  record Test {
                    string id;
                  }
                }
                """, UTF_8);

        ValidationResult result = SystemCommonDirValidator.validate(validationContext(tmpDir));

        assertFalse(result.isValid(), "Common dir contained a file with a protocol with a wrong name, this is not allowed");
    }

    @Test
    void multipleRecordsPerFile(@TempDir File tmpDir) throws IOException {
        File commonSystemDir = new File(tmpDir, ArchiveTypeRegistryConstants.COMMON_DIR_NAME);
        FileUtils.forceMkdir(commonSystemDir);
        FileUtils.write(new File(commonSystemDir, "test.Test.avdl"), """
                @namespace("test")
                protocol TestProtocol {
                  record Test1 {
                    string id;
                  }
                  record Test2 {
                    string id;
                  }
                }
                """, UTF_8);

        ValidationResult result = SystemCommonDirValidator.validate(validationContext(tmpDir));

        assertFalse(result.isValid(), "Common dir contained a file with multiple records, this is not allowed");
    }
}

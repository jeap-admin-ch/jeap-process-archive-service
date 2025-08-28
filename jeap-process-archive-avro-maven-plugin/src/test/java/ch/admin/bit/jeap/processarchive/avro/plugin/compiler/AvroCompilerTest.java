package ch.admin.bit.jeap.processarchive.avro.plugin.compiler;

import org.apache.avro.Protocol;
import org.apache.avro.Schema;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class AvroCompilerTest {

    private Path tempOutputDir;

    private static AvroCompiler generateCompiler(Path outputDirectory) throws IOException {
        return AvroCompiler.builder()
                .sourceEncoding(Charset.defaultCharset().name())
                .outputDirectory(outputDirectory.toFile())
                .build();
    }

    @BeforeEach
    void init() throws IOException {
        tempOutputDir = Files.createTempDirectory("AvroCompilerTestOutputDirectory");
    }

    @AfterEach
    void cleanUp() throws IOException {
        FileUtils.deleteDirectory(tempOutputDir.toFile());
    }

    @Test
    void schema() throws IOException {
        File src = new File("src/test/resources/unittest/validSchema.avsc");
        Schema schema = new Schema.Parser().parse(src);
        AvroCompiler target = generateCompiler(tempOutputDir);

        target.compileSchema(schema, src);

        assertTrue(isNotEmpty(tempOutputDir.resolve("ch/admin/bit/jeap/processarchive/avro/test/schema")));
    }

    @Test
    void protocol() throws IOException {
        File src = new File("src/test/resources/unittest/validProtocol.avpr");
        Protocol protocol = Protocol.parse(src);
        AvroCompiler target = generateCompiler(tempOutputDir);

        target.compileProtocol(protocol, src);

        assertTrue(isNotEmpty(tempOutputDir.resolve("ch/admin/bit/jeap/processarchive/avro/test/protocol")));
    }

    private boolean isNotEmpty(Path directory) throws IOException {
        return Files.list(directory).count() > 0;
    }
}

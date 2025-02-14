package ch.admin.bit.jeap.processarchive.avro.plugin.compiler;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.Singular;
import org.apache.avro.Protocol;
import org.apache.avro.Schema;
import org.apache.avro.compiler.specific.SpecificCompiler;
import org.apache.avro.generic.GenericData;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Compiler for AVRO files. Basically a wrapper for {@link SpecificCompiler}, use to preconfigure this compiler
 * with some share settings
 */
@Builder
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class AvroCompiler {
    private static final String TEMPLATE_DIRECTORY = "/velocity-templates/";
    private final String sourceEncoding;
    private final File outputDirectory;
    @Singular
    private final List<Object> additionalTools;
    private final boolean enableDecimalLogicalType;

    /**
     * Compile an Avro-Schema. Generates .class files for the schema and writes them into the outputDirectory
     *
     * @param schema            The schema to compile
     * @param onlyIfFileChanged Recompile only if this file has changed. Can be null, in this case recompile always.
     * @throws IOException If onlyIfFileChanged cannot be read
     */
    public void compileSchema(Schema schema, @Nullable File onlyIfFileChanged) throws IOException {
        SpecificCompiler compiler = new SpecificCompiler(schema);
        configureCompiler(compiler);
        compiler.compileToDestination(onlyIfFileChanged, outputDirectory);
    }

    /**
     * Compile an Avro-Schema. Generates .class files for the schema and writes them into the outputDirectory
     *
     * @param protocol          The protocol to compile
     * @param onlyIfFileChanged Recompile only if this file has changed. Can be null, in this case recompile always.
     * @throws IOException If onlyIfFileChanged cannot be read
     */
    public void compileProtocol(Protocol protocol, @Nullable File onlyIfFileChanged) throws IOException {
        SpecificCompiler compiler = new SpecificCompiler(protocol);
        configureCompiler(compiler);
        compiler.compileToDestination(onlyIfFileChanged, outputDirectory);
    }

    private void configureCompiler(SpecificCompiler compiler) {
        compiler.setStringType(GenericData.StringType.String);
        compiler.setTemplateDir(TEMPLATE_DIRECTORY);
        compiler.setFieldVisibility(SpecificCompiler.FieldVisibility.PRIVATE);
        compiler.setCreateOptionalGetters(true);
        compiler.setGettersReturnOptional(false);
        compiler.setCreateSetters(true);
        compiler.setAdditionalVelocityTools(additionalTools);
        compiler.setEnableDecimalLogicalType(enableDecimalLogicalType);
        compiler.setOutputCharacterEncoding(sourceEncoding);
    }
}

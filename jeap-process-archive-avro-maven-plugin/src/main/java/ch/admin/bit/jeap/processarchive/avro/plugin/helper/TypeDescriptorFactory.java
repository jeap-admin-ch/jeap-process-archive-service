package ch.admin.bit.jeap.processarchive.avro.plugin.helper;

import ch.admin.bit.jeap.processarchive.avro.plugin.registry.descriptor.ArchiveTypeDescriptor;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import lombok.experimental.UtilityClass;
import org.apache.maven.plugin.MojoExecutionException;
import tools.jackson.core.JacksonException;

import java.nio.file.Path;

@UtilityClass
public class TypeDescriptorFactory {

    private static final ObjectMapper OBJECT_MAPPER;

    static {
        OBJECT_MAPPER = JsonMapper.builder()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .build();
    }

    public static ArchiveTypeDescriptor getTypeDescriptor(Path descriptor) throws MojoExecutionException {
        try {
            return OBJECT_MAPPER.readValue(descriptor.toFile(), ArchiveTypeDescriptor.class);
        } catch (JacksonException e) {
            throw new MojoExecutionException("Cannot read value from json: " + e.getMessage(), e);
        }
    }

    public static ArchiveTypeDescriptor readTypeDescriptor(String jsonContent, String descriptorPath) throws MojoExecutionException {
        if (jsonContent == null) {
            return null;
        }

        try {
            return OBJECT_MAPPER.readValue(jsonContent, ArchiveTypeDescriptor.class);
        } catch (JacksonException e) {
            throw new MojoExecutionException("Cannot read value from json at " + descriptorPath + ": " + e.getMessage(), e);
        }
    }
}

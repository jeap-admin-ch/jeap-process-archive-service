package ch.admin.bit.jeap.processarchive.avro.plugin.helper;

import ch.admin.bit.jeap.processarchive.avro.plugin.registry.connector.ArchiveTypeDescriptor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.experimental.UtilityClass;
import org.apache.maven.plugin.MojoExecutionException;

import java.io.IOException;
import java.nio.file.Path;

@UtilityClass
public class TypeDescriptorFactory {

    private static final ObjectMapper OBJECT_MAPPER;

    static {
        OBJECT_MAPPER = new ObjectMapper();
        OBJECT_MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public static ArchiveTypeDescriptor getTypeDescriptor(Path descriptor) throws MojoExecutionException {
        try {
            return OBJECT_MAPPER.readValue(descriptor.toFile(), ArchiveTypeDescriptor.class);
        } catch (IOException e) {
            throw new MojoExecutionException("Cannot read value from json: " + e.getMessage(), e);
        }
    }

    public static ArchiveTypeDescriptor readTypeDescriptor(String jsonContent, String descriptorPath) throws MojoExecutionException {
        if (jsonContent == null) {
            return null;
        }

        try {
            return OBJECT_MAPPER.readValue(jsonContent, ArchiveTypeDescriptor.class);
        } catch (IOException e) {
            throw new MojoExecutionException("Cannot read value from json at " + descriptorPath + ": " + e.getMessage(), e);
        }
    }
}

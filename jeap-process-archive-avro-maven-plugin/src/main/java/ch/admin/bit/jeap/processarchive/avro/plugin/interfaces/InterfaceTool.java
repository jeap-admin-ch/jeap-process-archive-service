package ch.admin.bit.jeap.processarchive.avro.plugin.interfaces;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.apache.avro.Schema;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

@RequiredArgsConstructor
public class InterfaceTool {
    private final static ObjectMapper objectMapper = new ObjectMapper();
    private final static JsonFactory jsonFactory = new JsonFactory();
    private final List<AdditionalInterface> additionalInterfaces;

    public InterfaceTool(File interfacesFile) throws IOException {
        if (!interfacesFile.exists()) {
            additionalInterfaces = Collections.emptyList();
            return;
        }
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        JsonParser jsonParser = jsonFactory.createParser(interfacesFile);
        additionalInterfaces = objectMapper.readValue(jsonParser, new TypeReference<>() {
        });
    }

    //This method is used in the velocity template
    @SuppressWarnings("unused")
    public String getInterfaces(Schema schema) {
        return InterfaceList.builder()
                .additionalInterfaces(additionalInterfaces)
                .schema(schema)
                .build();
    }
}

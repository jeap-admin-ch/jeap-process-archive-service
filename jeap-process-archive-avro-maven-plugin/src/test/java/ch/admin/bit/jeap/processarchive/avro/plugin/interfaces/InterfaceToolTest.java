package ch.admin.bit.jeap.processarchive.avro.plugin.interfaces;

import org.apache.avro.Schema;
import org.apache.avro.specific.SpecificRecord;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InterfaceToolTest {
    private void assertInterfaceListEquals(String actual, Object... expectedInterfaces) {
        String expected = Arrays.stream(expectedInterfaces)
                .map(o -> o instanceof Class c ? c.getCanonicalName() : o.toString())
                .collect(Collectors.joining(", "));
        assertEquals(expected, actual);
    }

    @Test
    void fileNotExisting() throws IOException {
        File src = new File("src/test/resources/unittest/validSchema.avsc");
        Schema schema = new Schema.Parser().parse(src);
        File interfacesFile = new File("src/test/resources/unittest/notExist.json");
        InterfaceTool target = new InterfaceTool(interfacesFile);
        assertInterfaceListEquals(target.getInterfaces(schema), SpecificRecord.class);
    }

    @Test
    void fileInvalid() {
        File interfacesFile = new File("src/test/resources/unittest/invalid.json");
        assertThrows(IOException.class, () -> new InterfaceTool(interfacesFile));
    }

    @Test
    void additionalInterface() throws IOException {
        File src = new File("src/test/resources/unittest/validSchema.avsc");
        Schema schema = new Schema.Parser().parse(src);
        File interfacesFile = new File("src/test/resources/unittest/interfaces.json");
        InterfaceTool target = new InterfaceTool(interfacesFile);
        assertInterfaceListEquals(target.getInterfaces(schema), SpecificRecord.class, FatClass.class);
    }

    private interface FatClass {

    }
}

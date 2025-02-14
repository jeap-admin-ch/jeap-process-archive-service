package ch.admin.bit.jeap.processarchive.avro.plugin.interfaces;

import lombok.*;
import org.apache.avro.Schema;
import org.apache.avro.specific.SpecificRecord;

import java.util.LinkedList;
import java.util.List;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
class InterfaceList {
    private final List<String> interfaceNames = new LinkedList<>();

    @Builder
    private static String create(@NonNull Schema schema, @Singular List<AdditionalInterface> additionalInterfaces) {
        InterfaceList interfaceList = new InterfaceList();
        interfaceList.addClass(SpecificRecord.class);
        interfaceList.addAdditionalInterfaces(schema, additionalInterfaces);
        return interfaceList.toString();
    }

    private void addAdditionalInterfaces(Schema schema, List<AdditionalInterface> additionalInterfaces) {
        additionalInterfaces.stream()
                .filter(i -> i.getSchemas().contains(schema.getFullName()))
                .map(AdditionalInterface::getJavaInterface)
                .forEach(this::addName);
    }

    private void addClass(Class<?> klass) {
        addName(klass.getCanonicalName());
    }

    private void addName(String name) {
        interfaceNames.add(name);
    }

    @Override
    public String toString() {
        return String.join(", ", interfaceNames);
    }
}

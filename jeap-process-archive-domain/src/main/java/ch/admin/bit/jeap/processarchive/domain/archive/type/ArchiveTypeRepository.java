package ch.admin.bit.jeap.processarchive.domain.archive.type;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class ArchiveTypeRepository {

    private final Map<TypeId, ArchiveTypeInfo> types = new ConcurrentHashMap<>();

    public ArchiveTypeRepository(List<ArchiveTypeInfoProvider> providers) {
        for (ArchiveTypeInfoProvider provider : providers) {
            for (ArchiveTypeInfo typeInfo : provider.getArchiveTypes()) {
                TypeId id = new TypeId(typeInfo.getSystem(), typeInfo.getName(), typeInfo.getVersion());
                ArchiveTypeInfo existing = types.putIfAbsent(id, typeInfo);
                if (existing != null) {
                    log.warn("Duplicate archive type definition for {}, keeping first", id);
                }
            }
        }
        log.info("Loaded {} archive types: {}", types.size(), types.keySet());
    }

    public ArchiveTypeInfo requireType(String system, String name, int version) {
        TypeId id = new TypeId(system, name, version);
        ArchiveTypeInfo typeInfo = types.get(id);
        if (typeInfo == null) {
            throw ArchiveTypeNotFoundException.forType(system, name, version);
        }
        return typeInfo;
    }

    public List<ArchiveTypeInfo> getAllTypes() {
        List<ArchiveTypeInfo> all = new ArrayList<>(types.values());
        all.sort(Comparator.comparing(ArchiveTypeInfo::getName).thenComparingInt(ArchiveTypeInfo::getVersion));
        return all;
    }

    private record TypeId(String system, String name, int version) {
    }
}

package ch.admin.bit.jeap.processarchive.domain.archive.type;

public class ArchiveTypeNotFoundException extends RuntimeException {

    private ArchiveTypeNotFoundException(String message) {
        super(message);
    }

    public static ArchiveTypeNotFoundException forType(String system, String name, int version) {
        return new ArchiveTypeNotFoundException(
                "No archive type found for system '%s', name '%s', version %d".formatted(system, name, version));
    }
}

package ch.admin.bit.jeap.processarchive.registry.verifier;

import ch.admin.bit.jeap.processarchive.avro.plugin.compiler.ImportClassLoader;
import lombok.Builder;
import lombok.Value;

import java.io.File;

@Value
@Builder(toBuilder = true)
public class ValidationContext {
    ImportClassLoader importClassLoader;
    File descriptorDir;
    File oldDescriptorDir;
    String systemName;
    File systemDir;
    File descriptor;
    String archiveTypeName;
    File archiveTypeDir;
}

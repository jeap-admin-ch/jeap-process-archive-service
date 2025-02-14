package ch.admin.bit.jeap.processarchive.avro.plugin.registry.service;

import lombok.NonNull;
import lombok.Value;

import java.io.File;
import java.util.Map;

@Value
public class DownloadedSchema {
    @NonNull
    File schema;
    @NonNull
    Map<String, File> importPath;
}

package ch.admin.bit.jeap.processarchive.avro.plugin.registry.connector;

import java.io.IOException;
import java.io.InputStream;

@FunctionalInterface
interface DownloadedStreamConverter<Output> {
    Output convert(InputStream inputStream) throws RegistryConnectorException, IOException;
}

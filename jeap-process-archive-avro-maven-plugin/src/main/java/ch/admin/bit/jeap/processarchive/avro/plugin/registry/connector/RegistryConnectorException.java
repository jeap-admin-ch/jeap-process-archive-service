package ch.admin.bit.jeap.processarchive.avro.plugin.registry.connector;

import java.io.IOException;
import java.net.URL;

public class RegistryConnectorException extends Exception {
    private RegistryConnectorException(String message) {
        super(message);
    }

    private RegistryConnectorException(String message, Throwable cause) {
        super(message, cause);
    }

    static RegistryConnectorException cannotReadDescriptor(String filename, IOException cause) {
        String message = String.format("Cannot read downloaded archive type descriptor from file '%s'", filename);
        return new RegistryConnectorException(message, cause);
    }

    static RegistryConnectorException notPlainText(String contentType, URL url) {
        String message = String.format("Did not get raw text but '%s' from Bitbucket at '%s'", contentType, url.toString());
        return new RegistryConnectorException(message);
    }

    static RegistryConnectorException cannotDownload(URL url, Throwable cause) {
        String message = String.format("Cannot download from '%s'", url.toString());
        return new RegistryConnectorException(message, cause);
    }

    static RegistryConnectorException cannotCreateUrl(String repoUrl, String branch, String path, IOException cause) {
        String message = String.format("Cannot create valid URL from repository '%s', git reference '%s'and path '%s'", repoUrl, branch, path);
        return new RegistryConnectorException(message, cause);
    }

    static RegistryConnectorException cannotWriteTemporaryFile(String filename, IOException cause) {
        String message = String.format("Cannot write temporary output file with name '%s'", filename);
        return new RegistryConnectorException(message, cause);
    }

    static RegistryConnectorException statusCode(int responseCode, URL url) {
        if (responseCode == 302) {
            String message = String.format("Got a redirect, '%s' is probably not a public repo", url);
            return new RegistryConnectorException(message);
        }
        if (responseCode == 404) {
            String message = String.format("URL '%s' does not exist, got a 404", url);
            return new RegistryConnectorException(message);
        }
        if (responseCode == 500) {
            String message = String.format("Got a 500 Error from Bitbucket at URL '%s'", url);
            return new RegistryConnectorException(message);
        }
        String message = String.format("Got unexpected status code %s from Bitbucket at URL '%s'", responseCode, url);
        return new RegistryConnectorException(message);
    }
}

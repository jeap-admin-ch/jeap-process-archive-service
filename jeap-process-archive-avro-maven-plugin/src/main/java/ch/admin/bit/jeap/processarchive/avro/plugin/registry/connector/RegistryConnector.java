package ch.admin.bit.jeap.processarchive.avro.plugin.registry.connector;

import ch.admin.bit.jeap.processarchive.avro.plugin.registry.service.ArchiveTypeReference;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.FileUtils;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Downloader to get files from the Event-Registry
 */
public class RegistryConnector {
    private final static JsonFactory jsonFactory = new JsonFactory();
    private final ObjectMapper objectMapper;
    private final String repoUrl;
    private final GitReference gitReference;

    public RegistryConnector(String repoUrl, GitReference gitReference) {
        objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        this.repoUrl = repoUrl;
        this.gitReference = gitReference;
    }

    public ArchiveTypeDescriptor downloadDescriptorFromRegistry(ArchiveTypeReference archiveTypeReference) throws RegistryConnectorException {
        String filename = format("%s.json", archiveTypeReference.getName());
        String path = format("/archive-types/%s/%s/%s",
                archiveTypeReference.getSystem().toLowerCase(),
                archiveTypeReference.getName().toLowerCase(),
                filename);
        File tmpFile = downloadFileFromGit(path, inputStream -> toTempFileConverter(inputStream, filename));
        try {
            JsonParser jsonParser = jsonFactory.createParser(tmpFile);
            return objectMapper.readValue(jsonParser, ArchiveTypeDescriptor.class);
        } catch (IOException e) {
            throw RegistryConnectorException.cannotReadDescriptor(filename, e);
        } finally {
            // File no longer needed -> delete it
            // If this should be unsuccessful, the file has also been marked to be deleted on exit.
            if (!tmpFile.delete()) {
                tmpFile.deleteOnExit();
            }
        }
    }

    public File downloadSchemaFromRegistry(ArchiveTypeReference archiveTypeReference, String filename) throws RegistryConnectorException {
        String path = format("/archive-types/%s/%s/%s",
                archiveTypeReference.getSystem().toLowerCase(),
                archiveTypeReference.getName().toLowerCase(),
                filename);
        return downloadFileFromGit(path, inputStream -> toTempFileConverter(inputStream, filename));
    }

    public Map<String, File> downloadCommonFilesFromRegistry(String systemName) {
        try {
            String path = systemName == null ? "/archive-types/" + RegistryConstants.COMMON_DIR_NAME
                    : format("/archive-types/%s/" + RegistryConstants.COMMON_DIR_NAME, systemName.toLowerCase());
            Map<String, File> ret = new HashMap<>();
            for (String filename : downloadFileFromGit(path, this::toDirConverter)) {
                File file = downloadFileFromGit(path + "/" + filename, inputStream -> toTempFileConverter(inputStream, filename));
                ret.put(filename, file);
            }
            return ret;
        } catch (RegistryConnectorException e) {
            return Map.of();
        }
    }

    private <Output> Output downloadFileFromGit(String path, DownloadedStreamConverter<Output> converter) throws RegistryConnectorException {
        HttpURLConnection connection = null;
        URL url = convertToUrl(path);
        try {
            connection = (HttpURLConnection) url.openConnection();
            if (connection.getResponseCode() >= 300) {
                throw RegistryConnectorException.statusCode(connection.getResponseCode(), url);
            }
            if (!connection.getContentType().startsWith("text/plain")) {
                throw RegistryConnectorException.notPlainText(connection.getContentType(), url);
            }
            return converter.convert(connection.getInputStream());
        } catch (IOException e) {
            throw RegistryConnectorException.cannotDownload(url, e);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private URL convertToUrl(String path) throws RegistryConnectorException {
        try {
            String at = atCommitOrBranch();
            String urlAsString = format("%sraw%s?at=%s", ensureTrailingSlash(repoUrl), path, at);
            return new URL(urlAsString);
        } catch (MalformedURLException e) {
            throw RegistryConnectorException.cannotCreateUrl(repoUrl, gitReference.toString(), path, e);
        }
    }

    private String atCommitOrBranch() {
        if (gitReference.isCommit()) {
            return URLEncoder.encode(gitReference.getCommit(), UTF_8);
        }
        return format("refs%%2Fheads%%2F%s", URLEncoder.encode(gitReference.getBranch(), UTF_8));
    }

    private File toTempFileConverter(InputStream inputStream, String filename) throws RegistryConnectorException {
        try {
            File tmpFile = File.createTempFile("archivetype", filename);
            tmpFile.deleteOnExit();
            FileUtils.copyInputStreamToFile(inputStream, tmpFile);
            return tmpFile;
        } catch (IOException e) {
            throw RegistryConnectorException.cannotWriteTemporaryFile(filename, e);
        }
    }

    private List<String> toDirConverter(InputStream inputStream) throws IOException {
        List<String> ret = new LinkedList<>();
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        while (reader.ready()) {
            String line = reader.readLine();
            String[] split = line.split("\t");
            String filename = split[1];
            ret.add(filename);
        }
        return ret;
    }

    private String ensureTrailingSlash(String input) {
        if (input.endsWith("/")) {
            return input;
        }
        return input + "/";
    }
}

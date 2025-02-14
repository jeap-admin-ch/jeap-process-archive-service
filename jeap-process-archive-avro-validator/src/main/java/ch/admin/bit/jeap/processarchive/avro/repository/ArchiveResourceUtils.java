package ch.admin.bit.jeap.processarchive.avro.repository;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Notice: This class is based on code from arden-file
 * and available at <a href="https://github.com/ardenliu/common/tree/master/arden-file"></a>.
 */
@Slf4j
public final class ArchiveResourceUtils {

    private ArchiveResourceUtils() {
    }

    public static void copyFromClassPath(final String resourcePath, final Path targetRoot) {
        try {
            ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

            Resource[] resources = resolver.getResources(ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX + resourcePath + "/**");

            for (Resource resource : resources) {
                if (resource.exists()) {
                    String pathOfResource = resource.getURL().getPath();
                    int lastIndex = pathOfResource.lastIndexOf(resourcePath);
                    if (lastIndex == -1) {
                        log.error("cannot find[{}] from pathOfResource[{}]", resourcePath, pathOfResource);
                        continue;
                    }
                    String relativePath = pathOfResource.substring(lastIndex, pathOfResource.length());
                    Path targetPath = Paths.get(targetRoot.toString(), relativePath);
                    if (resource.isReadable()) {
                        log.debug("creating file {}", targetPath);
                        File targetFile = targetPath.toFile();
                        FileUtils.copyURLToFile(resource.getURL(), targetFile);

                    } else {
                        log.debug("creating directory {}", targetPath);
                        Files.createDirectories(targetPath);
                    }
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }
}

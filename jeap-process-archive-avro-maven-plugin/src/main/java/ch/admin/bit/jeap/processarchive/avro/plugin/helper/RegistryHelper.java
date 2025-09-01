package ch.admin.bit.jeap.processarchive.avro.plugin.helper;

import lombok.experimental.UtilityClass;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@UtilityClass
public class RegistryHelper {

    private static final Pattern PATTERN_VERSION_COMMON_DEFINITION = Pattern.compile("\\.v(\\d*?)\\.");

    public static Integer retrieveVersionFromCommonDefinition(String filepath) {
        Matcher matcher = PATTERN_VERSION_COMMON_DEFINITION.matcher(filepath);
        if (matcher.find()) {
            return Integer.valueOf(matcher.group(1));
        }

        return 1;
    }

    public static String convertFileNameOfAVDLToFilePathOfJava(String filename) {
        return filename.replace(".", "/").replace("/avdl", ".java");
    }

}

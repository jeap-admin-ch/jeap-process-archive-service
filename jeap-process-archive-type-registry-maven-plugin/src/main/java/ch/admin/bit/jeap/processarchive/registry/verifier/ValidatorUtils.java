package ch.admin.bit.jeap.processarchive.registry.verifier;

import ch.admin.bit.jeap.processarchive.registry.verifier.common.ValidationResult;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.lang3.StringUtils;

import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class ValidatorUtils {
    /**
     * System name in descriptor for shared messages
     */
    private static final String SHARED_SYSTEM = "Shared";
    private static final String SHARED_WITH_PREFIX = "_" + SHARED_SYSTEM;

    public static String getSystemNamePrefix(String systemName) {
        String systemNameCamelCase;
        if (SHARED_WITH_PREFIX.equalsIgnoreCase(systemName)) {
            systemNameCamelCase = SHARED_SYSTEM;
        } else {
            systemNameCamelCase = StringUtils.capitalize(systemName);
        }
        return systemNameCamelCase;
    }

    public static ValidationResult validateVersions(JsonNode contracts, JsonNode descriptorJson, String archiveTypeDescriptorPath, Function<JsonNode, JsonNode> getVersions) {
        return ValidatorUtils.streamElements(contracts)
                .map(getVersions)
                .flatMap(ValidatorUtils::streamElements)
                .map(JsonNode::asText)
                .map(version -> validateVersion(version, descriptorJson, archiveTypeDescriptorPath))
                .reduce(ValidationResult.ok(), ValidationResult::merge);
    }

    private static ValidationResult validateVersion(String version, JsonNode descriptorJson, String archiveTypeDescriptorPath) {
        JsonNode versionsNode = descriptorJson.get("versions");
        if (versionsNode == null) {
            String message = String.format("Missing version %s is referenced in a contract in archive type descriptor '%s'",
                    version, archiveTypeDescriptorPath);
            return ValidationResult.fail(message);
        }

        boolean versionValid = StreamSupport.stream(
                        Spliterators.spliteratorUnknownSize(versionsNode.elements(), Spliterator.ORDERED), false)
                .map(n -> n.get("version"))
                .map(JsonNode::asText)
                .anyMatch(version::equals);
        if (!versionValid) {
            String message = String.format("Missing version %s is referenced in a contract in archive type descriptor '%s'",
                    version, archiveTypeDescriptorPath);
            return ValidationResult.fail(message);
        }
        return ValidationResult.ok();
    }

    public static ValidationResult validateTopicOnContracts(String path, JsonNode contracts, JsonNode descriptor) {
        boolean isTopicDefinedOnDescriptor = descriptor.has("topic");

        String message;
        if (isTopicDefinedOnDescriptor) {
            message = "%s: Contract %s has a topic defined, but the topic is already defined on the descriptor";
        } else {
            message = """
                    %s: Contract %s has no topic defined, and no topic is defined on the descriptor. Either set the \
                    topic globally on the descriptor, or define a topic per contract.\
                    """;
        }

        return streamElements(contracts)
                .filter(node -> node.has("topic") == isTopicDefinedOnDescriptor)
                .map(node -> String.format(message, path, node))
                .map(ValidationResult::fail)
                .reduce(ValidationResult.ok(), ValidationResult::merge);
    }

    public static Stream<JsonNode> streamElements(JsonNode node) {
        if (node == null) {
            return Stream.of();
        }
        return StreamSupport.stream(node.spliterator(), false);
    }
}

package ch.admin.bit.jeap.processarchive.objectstorage.lifecycle;

import ch.admin.bit.jeap.processarchive.domain.archive.lifecycle.LifecyclePolicy;
import ch.admin.bit.jeap.processarchive.domain.archive.lifecycle.LifecyclePolicyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.s3.model.BucketLifecycleConfiguration;
import software.amazon.awssdk.services.s3.model.ExpirationStatus;
import software.amazon.awssdk.services.s3.model.GetBucketLifecycleConfigurationResponse;
import software.amazon.awssdk.services.s3.model.LifecycleRule;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static java.util.stream.Collectors.toSet;

@RequiredArgsConstructor
@Slf4j
public class S3LifecycleConfigurationFactory {

    private final LifecyclePolicyService lifecyclePolicyService;

    /**
     * Creates or updates a {@link BucketLifecycleConfiguration} such that a lifecycle policy for all configured
     * archive type expiration settings is available. The lifecycle rule ID consists of system, archive type name
     * and expiration days to ensure that 1) a policy is scoped to a single archive type, and 2) changing lifecycle
     * expiration settings do not influence existing, already persisted data.
     */
    BucketLifecycleConfiguration createOrUpdateBucketLifecycleConfiguration(GetBucketLifecycleConfigurationResponse getBucketLifecycleConfigurationResponse) {
        BucketLifecycleConfiguration configuration = createOrReuseConfiguration(getBucketLifecycleConfigurationResponse);
        return addMissingRulesForLifecyclePolicies(configuration);
    }

    private BucketLifecycleConfiguration createOrReuseConfiguration(GetBucketLifecycleConfigurationResponse getBucketLifecycleConfigurationResponse) {
        BucketLifecycleConfiguration.Builder builder = BucketLifecycleConfiguration.builder();
        if (getBucketLifecycleConfigurationResponse != null && getBucketLifecycleConfigurationResponse.hasRules()) {
            builder.rules(getBucketLifecycleConfigurationResponse.rules());
        }
        return builder.build();
    }

    private BucketLifecycleConfiguration addMissingRulesForLifecyclePolicies(BucketLifecycleConfiguration configuration) {
        List<LifecycleRule> rules = new ArrayList<>(Objects.requireNonNullElseGet(configuration.rules(), List::of));
        Set<String> existingRuleIds = getRuleIds(rules);

        List<LifecyclePolicy> policies = lifecyclePolicyService.getAllLifecyclePolicies();
        log.info("Lifecycle policy configuration: {}", policies);
        policies.stream()
                .filter(policy -> !existingRuleIds.contains(S3LifecycleConfigurationInitializer.ruleId(policy)))
                .map(this::toRule)
                .forEach(rules::add);
        return BucketLifecycleConfiguration.builder().rules(rules).build();
    }

    private LifecycleRule toRule(LifecyclePolicy lifecyclePolicy) {
        return LifecycleRule.builder()
                .status(ExpirationStatus.ENABLED)
                .id(S3LifecycleConfigurationInitializer.ruleId(lifecyclePolicy))
                .expiration(le -> le.days(lifecyclePolicy.getCurrentVersionExpirationDays()))
                .noncurrentVersionExpiration(nve -> nve.noncurrentDays(lifecyclePolicy.getPreviousVersionExpirationDays()))
                .filter(lrf -> lrf.tag(S3LifecycleConfigurationInitializer.lifecyclePolicyTag(lifecyclePolicy)))
                .build();
    }

    private static Set<String> getRuleIds(List<LifecycleRule> rules) {
        return rules.stream()
                .map(LifecycleRule::id)
                .filter(Objects::nonNull)
                .collect(toSet());
    }
}

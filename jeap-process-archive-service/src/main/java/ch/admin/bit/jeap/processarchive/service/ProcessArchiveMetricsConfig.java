package ch.admin.bit.jeap.processarchive.service;

import io.micrometer.core.aop.TimedAspect;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;

@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
@ConditionalOnClass(MeterRegistry.class)
@AutoConfiguration
public class ProcessArchiveMetricsConfig {

    @Bean
    public TimedAspect timedAspect(MeterRegistry meterRegistry) {
        return new TimedAspect(meterRegistry);
    }
}

package ch.admin.bit.jeap.test.processarchive;

import ch.admin.bit.jeap.processarchive.domain.event.DomainEventReceiver;
import ch.admin.bit.jeap.processarchive.kafka.TestConsumer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class TestApp {

    @Bean
    TestConsumer testConsumer() {
        return new TestConsumer();
    }

    @Bean
    SimpleMeterRegistry meterRegistry() {
        return new SimpleMeterRegistry();
    }

    @Bean
    DomainEventReceiver domainEventReceiverStub() {
        return new DomainEventReceiver(null, null);
    }
}

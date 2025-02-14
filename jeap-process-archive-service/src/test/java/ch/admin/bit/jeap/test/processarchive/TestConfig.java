package ch.admin.bit.jeap.test.processarchive;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration
public class TestConfig {

    @Bean
    public TestConsumer testConsumer() {
        return new TestConsumer();
    }

}

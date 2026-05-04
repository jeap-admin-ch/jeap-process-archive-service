package ch.admin.bit.jeap.processarchive.service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;
import org.springframework.context.annotation.PropertySource;

@PropertySource("classpath:pas-default-config.properties")
@SpringBootApplication(exclude = {UserDetailsServiceAutoConfiguration.class})
public class ProcessArchiveApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProcessArchiveApplication.class, args);
    }
}

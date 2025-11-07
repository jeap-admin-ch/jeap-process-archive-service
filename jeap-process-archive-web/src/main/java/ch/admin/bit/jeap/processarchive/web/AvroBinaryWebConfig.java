package ch.admin.bit.jeap.processarchive.web;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

@AutoConfiguration
@ComponentScan
public class AvroBinaryWebConfig {

    @Bean
    public AvroBinaryHttpMessageConverter avroBinaryHttpMessageConverter() {
        return new AvroBinaryHttpMessageConverter();
    }

}

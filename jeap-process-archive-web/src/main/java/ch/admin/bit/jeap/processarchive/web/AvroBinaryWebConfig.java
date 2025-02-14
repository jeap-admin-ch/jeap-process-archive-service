package ch.admin.bit.jeap.processarchive.web;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.web.codec.CodecCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

@AutoConfiguration
@ComponentScan
public class AvroBinaryWebConfig {

    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    @Bean
    public AvroBinaryHttpMessageConverter avroBinaryHttpMessageConverter() {
        return new AvroBinaryHttpMessageConverter();
    }

    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
    @Bean
    public CodecCustomizer avroCodeCustomizer() {
        return (configurer) ->
                configurer.customCodecs().register(new AvroBinaryEncoder());
    }
}

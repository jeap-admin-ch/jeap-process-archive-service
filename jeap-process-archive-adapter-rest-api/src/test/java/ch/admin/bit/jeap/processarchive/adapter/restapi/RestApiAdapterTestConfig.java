package ch.admin.bit.jeap.processarchive.adapter.restapi;

import ch.admin.bit.jeap.security.test.resource.configuration.ServletJeapAuthorizationConfig;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

@Configuration
@ComponentScan
@EnableWebSecurity
class RestApiAdapterTestConfig extends ServletJeapAuthorizationConfig {

    // You have to provide the system name and the application context to the test support base class.
    RestApiAdapterTestConfig(ApplicationContext applicationContext) {
        super("jme", applicationContext);
    }

}

package ch.admin.bit.jeap.processarchive.configuration.json.deserializer;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.config.EmbeddedValueResolver;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SpringExpressionEvaluator {

    private final ConfigurableBeanFactory configurableBeanFactory;

    String evaluateExpression(String stringExpression) {
        if (stringExpression == null) {
            return null;
        }
        EmbeddedValueResolver embeddedValueResolver = new EmbeddedValueResolver(configurableBeanFactory);
        return embeddedValueResolver.resolveStringValue(stringExpression);
    }
}

package ch.admin.bit.jeap.processarchive.configuration.json.deserializer;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@SpringBootTest(classes = SpringExpressionEvaluator.class, properties = "target.host.name=http://localhost")
class SpringExpressionEvaluatorTest {

    @Autowired
    private SpringExpressionEvaluator springExpressionEvaluator;

    @Test
    void evaluateExpression() {
        assertNull(springExpressionEvaluator.evaluateExpression(null));
        assertEquals("", springExpressionEvaluator.evaluateExpression(""));
        assertEquals("literal", springExpressionEvaluator.evaluateExpression("literal"));
        assertEquals("http://localhost", springExpressionEvaluator.evaluateExpression("${target.host.name}"));
        assertEquals("http://localhost/api/archive/mix/match", springExpressionEvaluator.evaluateExpression("${target.host.name}/api/archive/mix/match"));
    }
}

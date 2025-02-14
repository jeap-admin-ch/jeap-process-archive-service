package ch.admin.bit.jeap.processarchive.web;

import ch.admin.bit.jeap.processarchive.test.avro.TestRecord;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.io.ByteArrayInputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@WebFluxTest(controllers = TestWebFluxController.class)
@ContextConfiguration(classes = AvroBinaryWebConfig.class)
class AvroBinaryWebFluxTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void testControllerReturnsBinaryAvroObjectThatCanBeSuccesfullyDeserialized() throws Exception {
        byte[] body = webTestClient.get()
                .uri("/test-webflux-api/avro")
                .exchange()
                .expectStatus().is2xxSuccessful()
                .returnResult(byte[].class)
                .getResponseBodyContent();

        assertNotNull(body);
        TestRecord testRecord = new AvroBinaryDeserializer().deserialize(TestRecord.class, new ByteArrayInputStream(body));
        assertEquals(42, testRecord.getId());
    }
}

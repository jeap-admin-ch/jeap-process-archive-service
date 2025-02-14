package ch.admin.bit.jeap.processarchive.web;

import ch.admin.bit.jeap.processarchive.test.avro.TestRecord;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import java.io.ByteArrayInputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = TestWebMvcController.class)
@ContextConfiguration(classes = AvroBinaryWebConfig.class)
class AvroBinaryWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testControllerReturnsBinaryAvroObjectThatCanBeSuccesfullyDeserialized() throws Exception {
        byte[] body = mockMvc.perform(get("/test-api/avro"))
                .andExpectAll(
                        status().is(200),
                        content().contentType("avro/binary"))
                .andReturn().getResponse().getContentAsByteArray();

        TestRecord testRecord = new AvroBinaryDeserializer().deserialize(TestRecord.class, new ByteArrayInputStream(body));
        assertEquals(42, testRecord.getId());
    }
}
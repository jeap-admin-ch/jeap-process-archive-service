package ch.admin.bit.jeap.processarchive.web;

import ch.admin.bit.jeap.processarchive.test.avro.TestRecord;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/test-api")
@RequiredArgsConstructor
class TestWebMvcController {

    @GetMapping(value = "/avro", produces = AvroWebConstants.AVRO_BINARY)
    public TestRecord getTestRecord() {
        return TestRecord.newBuilder()
                .setId(42)
                .setPayload("the answer to life, the universe and everything")
                .build();
    }
}
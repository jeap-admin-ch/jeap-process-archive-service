package ch.admin.bit.jeap.processarchive.configuration.json.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ProcessArchiveMessageConfiguration {

    @JsonAlias("events")
    private List<MessageArchiveConfiguration> messages = new ArrayList<>();

}

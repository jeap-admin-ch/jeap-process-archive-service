package ch.admin.bit.jeap.processarchive.configuration.json.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ProcessArchiveEventConfiguration {

    private List<DomainEventArchiveConfiguration> events = new ArrayList<>();

}

package ch.admin.bit.jeap.processarchive.configuration.json.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class IndexTypesConfiguration {

    private List<IndexTypeConfiguration> indexTypes = new ArrayList<>();

}

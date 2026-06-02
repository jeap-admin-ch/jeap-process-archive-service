package ch.admin.bit.jeap.processarchive.configuration.json.test;

import ch.admin.bit.jeap.processarchive.plugin.api.indextype.ArchiveTypeToSearchItemConverter;
import ch.admin.bit.jeap.processarchive.plugin.api.indextype.SearchItemContainer;

import java.util.Map;

public class TestIndexTypeConverter implements ArchiveTypeToSearchItemConverter<String> {

    @Override
    public SearchItemContainer convert(String archivePayload, String archiveId, String version,  Map<String, String> metadata) {
        return null;
    }
}

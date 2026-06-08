package ch.admin.bit.jeap.processarchive.plugin.api.indextype;

import ch.admin.bit.jeap.opensearch.searchitem.model.SearchItemContainer;

import java.util.Map;

public interface ArchiveTypeToSearchItemConverter<T> {

    SearchItemContainer convert(T archivePayload, String archiveId, String version, Map<String, String> metadata);
}

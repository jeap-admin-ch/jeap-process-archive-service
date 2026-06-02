package ch.admin.bit.jeap.processarchive.plugin.api.indextype;

import ch.admin.bit.jeap.opensearch.indextype.SearchItem;

public record SearchItemContainer(
        int indexMajorVersion,
        int indexMinorVersion,
        SearchItem<?> searchItem) {
}

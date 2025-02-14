package ch.admin.bit.jeap.processarchive.domain.archive;

import ch.admin.bit.jeap.processarchive.plugin.api.archivedata.ArchiveData;
import ch.admin.bit.jeap.processarchive.plugin.api.archivedata.ArchiveDataReference;

public interface RemoteArchiveDataProvider {

    ArchiveData readArchiveData(String endpointTemplate, String oauthClientId, ArchiveDataReference reference);
}

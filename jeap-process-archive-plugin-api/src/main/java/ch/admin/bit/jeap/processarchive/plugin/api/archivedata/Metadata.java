package ch.admin.bit.jeap.processarchive.plugin.api.archivedata;

import lombok.Value;

@Value(staticConstructor = "of")
public class Metadata {
    /**
     * Note that metadata names are transported using HTTP headers and thus always lower-case (case insensitive /
     * HTTP 2.0 only uses lowercase headers)
     */
    String name;
    String value;
}

package ch.admin.bit.jeap.processarchive.dataprovider.remote;

import ch.admin.bit.jeap.processarchive.plugin.api.archivedata.ArchiveDataReference;
import org.springframework.http.HttpStatusCode;

import java.util.Set;

import static java.lang.String.format;

class RemoteDataProviderException extends RuntimeException {

    private RemoteDataProviderException(String message) {
        super(message);
    }

    static RemoteDataProviderException badHttpResponseStatus(ArchiveDataReference reference, HttpStatusCode statusCode) {
        return new RemoteDataProviderException(format(
                "Unable to read archive data for reference '%s', HTTP status code '%s'",
                reference, statusCode.toString()));
    }

    static RemoteDataProviderException missingHeader(ArchiveDataReference reference, String missingHeader) {
        return new RemoteDataProviderException(format(
                "Unable to read archive data for reference '%s', missing mandatory header '%s'",
                reference, missingHeader));
    }

    static RemoteDataProviderException invalidVersion(ArchiveDataReference reference, String headerName, String parsingError) {
        return new RemoteDataProviderException(format(
                "Unable to read archive data for reference '%s', version header '%s' is invalid: %s",
                reference, headerName, parsingError));
    }

    static RemoteDataProviderException templateUriParametersMismatch(ArchiveDataReference reference, String endpointTemplate, Set<String> missingParameters, Set<String> additionalParameters) {
        return new RemoteDataProviderException(format(
                "Unable to read archive data for reference '%s', endpoint template '%s' does not match actual fetch parameters, missing template uri parameters '%s', additional template uri parameters '%s'.",
                reference, endpointTemplate, parametersToString(missingParameters), parametersToString(additionalParameters)));
    }

    private static String parametersToString(Set<String> parameters) {
        return String.join(", ", parameters);
    }
}

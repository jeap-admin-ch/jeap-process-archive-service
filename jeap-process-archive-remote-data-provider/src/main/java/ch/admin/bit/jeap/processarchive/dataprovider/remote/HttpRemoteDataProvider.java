package ch.admin.bit.jeap.processarchive.dataprovider.remote;

import ch.admin.bit.jeap.processarchive.domain.archive.RemoteArchiveDataProvider;
import ch.admin.bit.jeap.processarchive.plugin.api.archivedata.ArchiveData;
import ch.admin.bit.jeap.processarchive.plugin.api.archivedata.ArchiveDataReference;
import ch.admin.bit.jeap.security.restclient.JeapOAuth2RestClientBuilderFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
class HttpRemoteDataProvider implements RemoteArchiveDataProvider {

    private static final String ID_PARAMETER_NAME = "id";
    private static final String VERSION_PARAMETER_NAME = "version";

    private final JeapOAuth2RestClientBuilderFactory jeapOAuth2RestClientBuilderFactory;
    private final RestClient noAuthRestClient;
    private final Map<String, RestClient> oauthRestClientsByClientId = new ConcurrentHashMap<>();
    private final Duration timeout;

    HttpRemoteDataProvider(RemoteDataProviderConfig config,
                           JeapOAuth2RestClientBuilderFactory jeapOAuth2RestClientBuilderFactory,
                           RestClient.Builder noAuthRestClientBuilder) {
        this.timeout = config.getTimeout();
        this.jeapOAuth2RestClientBuilderFactory = jeapOAuth2RestClientBuilderFactory;
        this.noAuthRestClient = noAuthRestClientBuilder.build();
    }

    @Override
    public ArchiveData readArchiveData(String endpointTemplate, String oauthClientId, ArchiveDataReference reference) {
        log.info("Reading remote data from '{}' using reference '{}'.", endpointTemplate, reference);
        Map<String, String> uriParameters = getUriParameters(reference);
        checkEndpointTemplateParameters(reference, endpointTemplate, uriParameters.keySet());
        return getRestClient(oauthClientId)
                .get()
                .uri(endpointTemplate, uriParameters)
                .exchange( (clientRequest, clientResponse) ->
                    ArchiveDataHttpResponseMapper.mapResponseToArchiveData(reference, clientResponse));
    }

    private Map<String, String> getUriParameters(ArchiveDataReference reference) {
        Map<String, String> params = new HashMap<>();
        params.put(ID_PARAMETER_NAME, reference.getId());
        Optional.ofNullable(reference.getVersion())
                .map(Object::toString)
                .ifPresent(version -> params.put(VERSION_PARAMETER_NAME, version));
        return params;
    }

    private void checkEndpointTemplateParameters(ArchiveDataReference reference, String endpointTemplate, Set<String> parameters) {
        TemplateUriParametersValidator.ValidationResult result = TemplateUriParametersValidator.builder()
                .template(endpointTemplate)
                .expectedParameters(parameters)
                .build()
                .validate();
        if (!result.isValid()) {
            throw RemoteDataProviderException.templateUriParametersMismatch(reference, endpointTemplate, result.getMissingParameters(), result.getAdditionalParameters());
        }
    }

    private RestClient getRestClient(String oauthClientId) {
        if (oauthClientId == null) {
            return noAuthRestClient;
        }
        return oauthRestClientsByClientId.computeIfAbsent(oauthClientId, this::createRestClient);
    }

    private RestClient createRestClient(String oauthClientId) {
        ClientHttpRequestFactory requestFactory = ClientHttpRequestFactories.get(ClientHttpRequestFactorySettings.DEFAULTS
                .withReadTimeout(timeout));
        RestClient.Builder oAuth2RestClientBuilder = jeapOAuth2RestClientBuilderFactory.createForClientRegistryId(oauthClientId);
        return oAuth2RestClientBuilder.requestFactory(requestFactory).build();
    }

}

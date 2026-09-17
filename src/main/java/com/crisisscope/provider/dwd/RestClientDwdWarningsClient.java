package com.crisisscope.provider.dwd;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
class RestClientDwdWarningsClient implements DwdWarningsClient {

    private final RestClient restClient;
    private final DwdWarningsProperties properties;

    RestClientDwdWarningsClient(RestClient.Builder restClientBuilder, DwdWarningsProperties properties) {
        this.restClient = restClientBuilder.build();
        this.properties = properties;
    }

    @Override
    public String fetchRawWarningsPayload() {
        return restClient.get()
                .uri(properties.getWarningsUrl())
                .retrieve()
                .body(String.class);
    }
}

package com.crisisscope.provider.bbk;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
class RestClientBbkWarningsClient implements BbkWarningsClient {

    private final RestClient restClient;
    private final BbkWarningsProperties properties;

    RestClientBbkWarningsClient(RestClient.Builder restClientBuilder, BbkWarningsProperties properties) {
        this.restClient = restClientBuilder.build();
        this.properties = properties;
    }

    @Override
    public List<BbkMapDataEntry> fetchMapData(String channel) {
        BbkMapDataEntry[] entries = restClient.get()
                .uri(properties.getBaseUrl() + "/{channel}/mapData.json", channel)
                .retrieve()
                .body(BbkMapDataEntry[].class);
        return entries != null ? List.of(entries) : List.of();
    }

    @Override
    public BbkAlert fetchAlertDetail(String id) {
        return restClient.get()
                .uri(properties.getBaseUrl() + "/warnings/{id}.json", id)
                .retrieve()
                .body(BbkAlert.class);
    }
}

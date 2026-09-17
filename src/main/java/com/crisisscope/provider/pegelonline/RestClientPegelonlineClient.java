package com.crisisscope.provider.pegelonline;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
class RestClientPegelonlineClient implements PegelonlineClient {

    private final RestClient restClient;
    private final PegelonlineProperties properties;

    RestClientPegelonlineClient(RestClient.Builder restClientBuilder, PegelonlineProperties properties) {
        this.restClient = restClientBuilder.build();
        this.properties = properties;
    }

    @Override
    public List<PegelonlineStation> fetchStations() {
        PegelonlineStation[] stations = restClient.get()
                .uri(properties.getStationsUrl())
                .retrieve()
                .body(PegelonlineStation[].class);
        return stations != null ? List.of(stations) : List.of();
    }
}

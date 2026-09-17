package com.crisisscope.provider.pegelonline;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Configuration for the PEGELONLINE flood/river level provider (the official
 * REST API of the WSV, Germany's federal waterways and shipping
 * administration), overridable via {@code crisisscope.pegelonline.*}
 * properties.
 */
@ConfigurationProperties(prefix = "crisisscope.pegelonline")
public class PegelonlineProperties {

    private String stationsUrl =
            "https://www.pegelonline.wsv.de/webservices/rest-api/v2/stations.json"
                    + "?includeTimeseries=true&includeCurrentMeasurement=true";
    private Duration cacheTtl = Duration.ofMinutes(5);

    public String getStationsUrl() {
        return stationsUrl;
    }

    public void setStationsUrl(String stationsUrl) {
        this.stationsUrl = stationsUrl;
    }

    public Duration getCacheTtl() {
        return cacheTtl;
    }

    public void setCacheTtl(Duration cacheTtl) {
        this.cacheTtl = cacheTtl;
    }
}

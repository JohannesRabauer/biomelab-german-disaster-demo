package com.crisisscope.provider.bbk;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;

/**
 * Configuration for the BBK civil protection warnings provider (the official
 * public feed behind the NINA app and MoWaS), overridable via
 * {@code crisisscope.bbk.*} properties.
 * <p>
 * {@code channels} defaults to the civil-protection channels of the feed
 * (MoWaS, KATWARN, BIWAPP). {@code police} is deliberately excluded (not a
 * disaster/crisis category) and {@code dwd} is excluded because weather
 * warnings are covered by a dedicated DWD provider elsewhere.
 */
@ConfigurationProperties(prefix = "crisisscope.bbk")
public class BbkWarningsProperties {

    private String baseUrl = "https://warnung.bund.de/api31";
    private List<String> channels = List.of("mowas", "katwarn", "biwapp");
    private Duration cacheTtl = Duration.ofMinutes(5);

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public List<String> getChannels() {
        return channels;
    }

    public void setChannels(List<String> channels) {
        this.channels = channels;
    }

    public Duration getCacheTtl() {
        return cacheTtl;
    }

    public void setCacheTtl(Duration cacheTtl) {
        this.cacheTtl = cacheTtl;
    }
}

package com.crisisscope.provider.dwd;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Configuration for the DWD (Deutscher Wetterdienst) weather warnings provider,
 * overridable via {@code crisisscope.dwd.*} properties.
 */
@ConfigurationProperties(prefix = "crisisscope.dwd")
public class DwdWarningsProperties {

    private String warningsUrl = "https://www.dwd.de/DE/wetter/warnungen_gemeinden/json/warnungen_gemeinde_map.json";
    private Duration cacheTtl = Duration.ofMinutes(5);

    public String getWarningsUrl() {
        return warningsUrl;
    }

    public void setWarningsUrl(String warningsUrl) {
        this.warningsUrl = warningsUrl;
    }

    public Duration getCacheTtl() {
        return cacheTtl;
    }

    public void setCacheTtl(Duration cacheTtl) {
        this.cacheTtl = cacheTtl;
    }
}

package com.crisisscope.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Default map viewport for the dashboard, overridable via
 * {@code crisisscope.map.*} properties.
 */
@ConfigurationProperties(prefix = "crisisscope.map")
public class MapProperties {

    private double centerLatitude = 51.1657;
    private double centerLongitude = 10.4515;
    private double zoom = 5.3;

    public double getCenterLatitude() {
        return centerLatitude;
    }

    public void setCenterLatitude(double centerLatitude) {
        this.centerLatitude = centerLatitude;
    }

    public double getCenterLongitude() {
        return centerLongitude;
    }

    public void setCenterLongitude(double centerLongitude) {
        this.centerLongitude = centerLongitude;
    }

    public double getZoom() {
        return zoom;
    }

    public void setZoom(double zoom) {
        this.zoom = zoom;
    }
}

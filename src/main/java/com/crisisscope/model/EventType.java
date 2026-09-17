package com.crisisscope.model;

/**
 * Category of a crisis event. {@code label} and {@code icon} are for display only;
 * {@code icon} is a Unicode glyph so the UI needs no icon font/build step.
 */
public enum EventType {
    FLOOD("Flood", "🌊"),
    WILDFIRE("Wildfire", "🔥"),
    STORM("Storm", "🌪️"),
    EARTHQUAKE("Earthquake", "🌍"),
    POWER_OUTAGE("Power Outage", "⚡"),
    INDUSTRIAL_ACCIDENT("Industrial Accident", "🏭"),
    TRAFFIC_ACCIDENT("Traffic Accident", "🚧"),
    PUBLIC_HEALTH("Public Health", "🩺"),
    SECURITY_INCIDENT("Security Incident", "🚨"),
    INFRASTRUCTURE_FAILURE("Infrastructure Failure", "🏗️"),
    OTHER("Other", "❔");

    private final String label;
    private final String icon;

    EventType(String label, String icon) {
        this.label = label;
        this.icon = icon;
    }

    public String label() {
        return label;
    }

    public String icon() {
        return icon;
    }
}

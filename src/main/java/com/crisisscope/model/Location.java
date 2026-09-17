package com.crisisscope.model;

/**
 * A physical place an event is associated with. {@code latitude}/{@code longitude}
 * drive map placement; {@code region} is the German federal state (Bundesland)
 * used for filtering and the regional details panel.
 */
public record Location(
        String name,
        String region,
        double latitude,
        double longitude
) {
}

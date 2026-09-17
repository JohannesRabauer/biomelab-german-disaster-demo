package com.crisisscope.service.dto;

/**
 * Aggregate counters rendered as the top-of-dashboard KPI cards.
 */
public record KpiSummary(
        long totalEvents,
        long criticalEvents,
        long activeRegions,
        long activeProviders
) {
}

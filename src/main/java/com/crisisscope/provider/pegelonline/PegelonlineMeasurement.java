package com.crisisscope.provider.pegelonline;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * A timeseries' current reading as published by PEGELONLINE.
 * <p>
 * {@code stateMnwMhw} classifies {@code value} against the station's mean
 * low/high water statistic ({@code niedrig}/{@code normal}/{@code hoch});
 * {@code stateNswHsw} classifies it against the station's recorded
 * lowest/highest water level instead, so {@code hoch} there means the
 * reading exceeds even the historical high-water mark.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PegelonlineMeasurement(
        String timestamp,
        Double value,
        String stateMnwMhw,
        String stateNswHsw
) {
}

package com.crisisscope.provider.pegelonline;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * One measured quantity of a PEGELONLINE station (e.g. water level {@code "W"}
 * or discharge {@code "Q"}). Only the water level series is used here.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PegelonlineTimeseries(
        String shortname,
        String longname,
        String unit,
        PegelonlineMeasurement currentMeasurement
) {
}

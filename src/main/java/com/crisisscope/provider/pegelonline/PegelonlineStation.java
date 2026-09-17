package com.crisisscope.provider.pegelonline;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * A single gauge station as returned by PEGELONLINE's
 * {@code /stations.json?includeTimeseries=true&includeCurrentMeasurement=true}
 * endpoint (the official REST API of the WSV, Germany's federal waterways and
 * shipping administration).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PegelonlineStation(
        String uuid,
        String number,
        String shortname,
        String longname,
        Double km,
        String agency,
        Double longitude,
        Double latitude,
        PegelonlineWater water,
        List<PegelonlineTimeseries> timeseries
) {
}

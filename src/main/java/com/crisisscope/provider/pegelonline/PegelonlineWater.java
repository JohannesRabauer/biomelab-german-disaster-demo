package com.crisisscope.provider.pegelonline;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** The river/waterway (e.g. {@code "ELBE"}) a PEGELONLINE station reports on. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PegelonlineWater(
        String shortname,
        String longname
) {
}

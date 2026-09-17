package com.crisisscope.provider.dwd;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * A single warning entry as published by DWD's community-level warnings feed.
 * <p>
 * The upstream feed carries more fields than this (urgency, certainty, altitude
 * bounds, etc.); {@code ignoreUnknown} keeps deserialization resilient to fields
 * we don't need or to upstream additions.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record DwdWarning(
        Long id,
        Integer level,
        Long start,
        Long end,
        String regionName,
        String state,
        String stateShort,
        String event,
        String headline,
        String description,
        String instruction
) {
}

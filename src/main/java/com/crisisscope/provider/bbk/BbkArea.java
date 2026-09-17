package com.crisisscope.provider.bbk;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * A CAP {@code area} block. BBK's feed carries only the free-text description
 * in practice (no polygon/circle geometry and no usable geocode), so that's
 * all this models.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record BbkArea(
        String areaDesc
) {
}

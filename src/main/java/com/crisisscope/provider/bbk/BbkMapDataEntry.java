package com.crisisscope.provider.bbk;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * One entry of a channel's {@code mapData.json} listing (e.g.
 * {@code https://warnung.bund.de/api31/mowas/mapData.json}) — a lightweight
 * summary used to decide which alerts are worth fetching in full via
 * {@link BbkWarningsClient#fetchAlertDetail(String)}.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record BbkMapDataEntry(
        String id,
        Integer version,
        String severity,
        String type
) {
}

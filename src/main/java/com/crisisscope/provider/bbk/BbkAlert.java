package com.crisisscope.provider.bbk;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * A full CAP (Common Alerting Protocol) alert as returned by
 * {@code https://warnung.bund.de/api31/warnings/{id}.json}, BBK's official
 * public feed backing the NINA app and MoWaS.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record BbkAlert(
        String identifier,
        String sender,
        String sent,
        String msgType,
        List<BbkAlertInfo> info
) {
}

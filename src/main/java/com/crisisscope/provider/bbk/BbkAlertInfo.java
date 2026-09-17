package com.crisisscope.provider.bbk;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * One {@code info} block of a CAP alert. A single alert carries one block per
 * language (e.g. {@code de}, {@code de-LS} for simple language, {@code en}, …),
 * each repeating the full headline/description/instruction in that language.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record BbkAlertInfo(
        String language,
        List<String> category,
        String event,
        String severity,
        String effective,
        String onset,
        String expires,
        String headline,
        String description,
        String instruction,
        String web,
        List<BbkArea> area
) {
}

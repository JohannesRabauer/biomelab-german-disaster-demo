package com.crisisscope.provider.dwd;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;
import java.util.Map;

/**
 * Top-level payload of DWD's community-level warnings feed, keyed by warncell
 * (administrative area) ID. The same warning typically appears under every
 * warncell it covers, so callers must deduplicate by {@link DwdWarning#id()}.
 * <p>
 * The upstream feed also carries a {@code vorabInformation} map for
 * preliminary/unofficial warnings; this is intentionally not modeled here, so
 * only confirmed warnings are surfaced.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record DwdWarningsResponse(
        Map<String, List<DwdWarning>> warnings,
        Long time
) {
}

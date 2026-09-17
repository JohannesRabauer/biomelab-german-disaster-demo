package com.crisisscope.provider.dwd;

import com.crisisscope.model.CrisisEvent;
import com.crisisscope.model.EventType;
import com.crisisscope.model.Location;
import com.crisisscope.model.Severity;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Converts DWD's raw warning payload into {@link CrisisEvent}s.
 * <p>
 * {@link CrisisEvent} has no dedicated field for a warning's expiry, so the
 * validity window (start and end) is folded into {@link CrisisEvent#description()}
 * instead of being dropped.
 */
final class DwdWarningMapper {

    private static final String DWD_WARNINGS_URL = "https://www.dwd.de/DE/wetter/warnungen_aktuell/warnlagebericht_node.html";

    private static final DateTimeFormatter VALIDITY_FORMAT =
            DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm", Locale.ENGLISH).withZone(ZoneId.of("Europe/Berlin"));

    private DwdWarningMapper() {
    }

    /**
     * Flattens and deduplicates {@code response.warnings()} into {@link CrisisEvent}s.
     * A single warning is broadcast once per warncell (administrative area) it
     * covers, so the same {@link DwdWarning#id()} recurs across multiple map
     * entries; the first occurrence encountered is kept as the representative area.
     */
    static List<CrisisEvent> toEvents(String providerId, DwdWarningsResponse response) {
        if (response == null || response.warnings() == null) {
            return List.of();
        }
        Instant publishedAt = response.time() != null ? Instant.ofEpochMilli(response.time()) : null;

        Map<Long, DwdWarning> deduplicated = new LinkedHashMap<>();
        for (List<DwdWarning> warnings : response.warnings().values()) {
            if (warnings == null) {
                continue;
            }
            for (DwdWarning warning : warnings) {
                if (warning.id() != null && warning.start() != null) {
                    deduplicated.putIfAbsent(warning.id(), warning);
                }
            }
        }

        return deduplicated.values().stream()
                .map(warning -> toEvent(providerId, warning, publishedAt))
                .toList();
    }

    static CrisisEvent toEvent(String providerId, DwdWarning warning, Instant publishedAt) {
        Instant occurredAt = Instant.ofEpochMilli(warning.start());
        Instant reportedAt = publishedAt != null ? publishedAt : occurredAt;

        return new CrisisEvent(
                "dwd-" + warning.id(),
                providerId,
                resolveTitle(warning),
                composeDescription(warning),
                classifyType(warning),
                classifySeverity(warning.level()),
                resolveLocation(warning),
                occurredAt,
                reportedAt,
                DWD_WARNINGS_URL
        );
    }

    static String resolveTitle(DwdWarning warning) {
        if (warning.headline() != null && !warning.headline().isBlank()) {
            return warning.headline().strip();
        }
        if (warning.event() != null && !warning.event().isBlank()) {
            return toTitleCase(warning.event().strip());
        }
        return "DWD weather warning";
    }

    static String composeDescription(DwdWarning warning) {
        StringBuilder text = new StringBuilder();
        if (warning.description() != null && !warning.description().isBlank()) {
            text.append(warning.description().strip());
        } else if (warning.event() != null) {
            text.append(toTitleCase(warning.event().strip()));
        }

        String validity = formatValidity(warning);
        if (!validity.isEmpty()) {
            appendParagraph(text, validity);
        }

        if (warning.instruction() != null && !warning.instruction().isBlank()) {
            appendParagraph(text, warning.instruction().strip());
        }

        return text.toString();
    }

    private static void appendParagraph(StringBuilder text, String paragraph) {
        if (text.length() > 0) {
            text.append("\n\n");
        }
        text.append(paragraph);
    }

    static String formatValidity(DwdWarning warning) {
        if (warning.start() == null) {
            return "";
        }
        String startText = VALIDITY_FORMAT.format(Instant.ofEpochMilli(warning.start()));
        if (warning.end() == null) {
            return "Valid: " + startText + " until further notice (Europe/Berlin).";
        }
        String endText = VALIDITY_FORMAT.format(Instant.ofEpochMilli(warning.end()));
        return "Valid: " + startText + " – " + endText + " (Europe/Berlin).";
    }

    static EventType classifyType(DwdWarning warning) {
        String text = ((warning.event() == null ? "" : warning.event()) + " "
                + (warning.headline() == null ? "" : warning.headline())).toUpperCase(Locale.GERMANY);

        if (text.contains("HOCHWASSER") || text.contains("FLUT")) {
            return EventType.FLOOD;
        }
        if (text.contains("GEWITTER") || text.contains("STURM") || text.contains("ORKAN")
                || text.contains("WIND") || text.contains("BÖEN") || text.contains("SCHNEE")
                || text.contains("GLÄTTE") || text.contains("FROST") || text.contains("HITZE")
                || text.contains("NEBEL") || text.contains("REGEN")) {
            return EventType.STORM;
        }
        return EventType.OTHER;
    }

    static Severity classifySeverity(Integer level) {
        if (level == null) {
            return Severity.MODERATE;
        }
        return switch (level) {
            case 1 -> Severity.LOW;
            case 2 -> Severity.MODERATE;
            case 3 -> Severity.HIGH;
            case 4 -> Severity.CRITICAL;
            default -> Severity.MODERATE;
        };
    }

    static Location resolveLocation(DwdWarning warning) {
        String name = warning.regionName() != null && !warning.regionName().isBlank()
                ? warning.regionName().strip() : "Germany";
        String state = warning.state() != null && !warning.state().isBlank()
                ? warning.state().strip() : "Unknown";
        double[] coordinates = BundeslandCoordinates.representativeCoordinates(warning.state());
        return new Location(name, state, coordinates[0], coordinates[1]);
    }

    private static String toTitleCase(String allCaps) {
        String lower = allCaps.toLowerCase(Locale.GERMANY);
        StringBuilder result = new StringBuilder(lower.length());
        boolean capitalizeNext = true;
        for (char c : lower.toCharArray()) {
            if (capitalizeNext && Character.isLetter(c)) {
                result.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else {
                result.append(c);
                if (c == ' ') {
                    capitalizeNext = true;
                }
            }
        }
        return result.toString();
    }
}

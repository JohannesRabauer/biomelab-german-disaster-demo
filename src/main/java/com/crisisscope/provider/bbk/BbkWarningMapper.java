package com.crisisscope.provider.bbk;

import com.crisisscope.model.CrisisEvent;
import com.crisisscope.model.EventType;
import com.crisisscope.model.Location;
import com.crisisscope.model.Severity;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Converts a raw {@link BbkAlert} (CAP format) into a {@link CrisisEvent}.
 * <p>
 * A {@code Cancel} message just retracts an earlier warning rather than
 * describing an active one, and an alert without a usable {@code info} block
 * or timestamp can't be turned into a meaningful event, so both map to
 * {@link Optional#empty()} and are skipped by the caller.
 */
final class BbkWarningMapper {

    private static final Pattern BR_TAG = Pattern.compile("(?i)<br\\s*/?>");
    private static final Pattern HTML_TAG = Pattern.compile("<[^>]+>");

    private BbkWarningMapper() {
    }

    static Optional<CrisisEvent> toEvent(String providerId, BbkAlert alert) {
        if (alert == null || "Cancel".equalsIgnoreCase(alert.msgType())) {
            return Optional.empty();
        }
        BbkAlertInfo info = pickPrimaryInfo(alert.info());
        if (info == null) {
            return Optional.empty();
        }
        Instant occurredAt = firstParsable(info.effective(), info.onset(), alert.sent());
        if (occurredAt == null) {
            return Optional.empty();
        }
        Instant reportedAt = firstParsable(alert.sent());
        if (reportedAt == null) {
            reportedAt = occurredAt;
        }

        return Optional.of(new CrisisEvent(
                "bbk-" + alert.identifier(),
                providerId,
                resolveTitle(info),
                composeDescription(info),
                classifyType(info),
                classifySeverity(info.severity()),
                resolveLocation(alert, info),
                occurredAt,
                reportedAt,
                info.web() != null && !info.web().isBlank() ? info.web() : null
        ));
    }

    static BbkAlertInfo pickPrimaryInfo(List<BbkAlertInfo> infoBlocks) {
        if (infoBlocks == null || infoBlocks.isEmpty()) {
            return null;
        }
        return infoBlocks.stream()
                .filter(i -> "de".equalsIgnoreCase(i.language()))
                .findFirst()
                .orElse(infoBlocks.get(0));
    }

    static String resolveTitle(BbkAlertInfo info) {
        if (info.headline() != null && !info.headline().isBlank()) {
            return info.headline().strip();
        }
        return info.event() != null ? info.event().strip() : "Civil protection warning";
    }

    static String composeDescription(BbkAlertInfo info) {
        StringBuilder text = new StringBuilder();
        if (info.description() != null && !info.description().isBlank()) {
            text.append(htmlToPlainText(info.description()));
        }
        if (info.instruction() != null && !info.instruction().isBlank()) {
            if (!text.isEmpty()) {
                text.append("\n\n");
            }
            text.append(htmlToPlainText(info.instruction()));
        }
        return text.toString();
    }

    static String htmlToPlainText(String html) {
        String withNewlines = BR_TAG.matcher(html).replaceAll("\n");
        return HTML_TAG.matcher(withNewlines).replaceAll("").strip();
    }

    static EventType classifyType(BbkAlertInfo info) {
        String category = info.category() != null && !info.category().isEmpty()
                ? info.category().get(0).toUpperCase(Locale.ROOT) : "";
        String text = ((info.event() == null ? "" : info.event()) + " "
                + (info.headline() == null ? "" : info.headline())).toUpperCase(Locale.GERMANY);

        return switch (category) {
            case "MET" -> text.contains("HOCHWASSER") || text.contains("STURMFLUT") || text.contains("ÜBERSCHWEMMUNG")
                    ? EventType.FLOOD : EventType.STORM;
            case "GEO" -> EventType.EARTHQUAKE;
            case "FIRE" -> EventType.WILDFIRE;
            case "HEALTH" -> EventType.PUBLIC_HEALTH;
            case "SAFETY", "SECURITY" -> EventType.SECURITY_INCIDENT;
            case "TRANSPORT" -> EventType.TRAFFIC_ACCIDENT;
            case "INFRA" -> EventType.INFRASTRUCTURE_FAILURE;
            case "CBRNE", "ENV" -> EventType.INDUSTRIAL_ACCIDENT;
            default -> EventType.OTHER;
        };
    }

    static Severity classifySeverity(String severity) {
        if (severity == null) {
            return Severity.MODERATE;
        }
        return switch (severity) {
            case "Extreme" -> Severity.CRITICAL;
            case "Severe" -> Severity.HIGH;
            case "Moderate" -> Severity.MODERATE;
            case "Minor" -> Severity.LOW;
            default -> Severity.MODERATE;
        };
    }

    static Location resolveLocation(BbkAlert alert, BbkAlertInfo info) {
        String name = firstAreaDesc(info).orElseGet(() -> resolveTitle(info));
        String region = GermanStates.regionFor(alert.sender());
        double[] coordinates = GermanStates.coordinatesFor(alert.sender());
        return new Location(name, region, coordinates[0], coordinates[1]);
    }

    private static Optional<String> firstAreaDesc(BbkAlertInfo info) {
        if (info.area() == null) {
            return Optional.empty();
        }
        return info.area().stream()
                .map(BbkArea::areaDesc)
                .filter(desc -> desc != null && !desc.isBlank())
                .findFirst();
    }

    private static Instant firstParsable(String... candidates) {
        for (String candidate : candidates) {
            Instant parsed = parseInstant(candidate);
            if (parsed != null) {
                return parsed;
            }
        }
        return null;
    }

    private static Instant parseInstant(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return OffsetDateTime.parse(value).toInstant();
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}

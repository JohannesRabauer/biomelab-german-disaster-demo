package com.crisisscope.service.dto;

import com.crisisscope.model.CrisisEvent;
import com.crisisscope.model.EventType;
import com.crisisscope.model.Severity;

/**
 * Query-string driven filter for the event feed. Any {@code null} field means
 * "no constraint" on that dimension. Blank/unparsable request parameters are
 * normalized to {@code null} in {@link #of} rather than rejected, so a stale or
 * malformed filter never breaks the feed.
 */
public record EventFilter(EventType type, Severity severity, String region) {

    public static EventFilter of(String type, String severity, String region) {
        return new EventFilter(
                parseEnum(EventType.class, type),
                parseEnum(Severity.class, severity),
                (region == null || region.isBlank()) ? null : region
        );
    }

    public boolean matches(CrisisEvent event) {
        if (type != null && event.type() != type) {
            return false;
        }
        if (severity != null && event.severity() != severity) {
            return false;
        }
        if (region != null && !region.equalsIgnoreCase(event.location().region())) {
            return false;
        }
        return true;
    }

    private static <E extends Enum<E>> E parseEnum(Class<E> enumType, String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Enum.valueOf(enumType, raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}

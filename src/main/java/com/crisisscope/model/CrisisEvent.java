package com.crisisscope.model;

import java.time.Instant;
import java.util.Optional;

/**
 * A single reported crisis/disaster event, normalized across all
 * {@link com.crisisscope.provider.CrisisDataProvider} implementations.
 * <p>
 * {@code sourceId} identifies which provider produced the event (see
 * {@code CrisisDataProvider#id()}), {@code occurredAt} is when the event itself
 * happened and {@code reportedAt} is when the source published/updated it — the
 * two may differ, e.g. delayed disclosure. {@code url} is optional because not
 * every source links back to a public report.
 */
public record CrisisEvent(
        String id,
        String sourceId,
        String title,
        String description,
        EventType type,
        Severity severity,
        Location location,
        Instant occurredAt,
        Instant reportedAt,
        String url
) {

    public Optional<String> urlOptional() {
        return Optional.ofNullable(url);
    }
}

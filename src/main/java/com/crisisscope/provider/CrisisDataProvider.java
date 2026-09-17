package com.crisisscope.provider;

import com.crisisscope.model.CrisisEvent;

import java.util.List;

/**
 * Shared contract for anything that can supply {@link CrisisEvent}s to CrisisScope.
 * <p>
 * Register an implementation as a Spring bean (e.g. {@code @Component}) and it is
 * automatically picked up by {@link com.crisisscope.service.CrisisDataRegistry} —
 * no wiring changes needed elsewhere.
 */
public interface CrisisDataProvider {

    /**
     * Stable, unique identifier for this provider (e.g. {@code "dwd-weather-warnings"}).
     * Used as {@link CrisisEvent#sourceId()} and for attribution in the UI.
     */
    String id();

    /**
     * Fetches the current set of events known to this provider. Implementations
     * may call out to external systems and should handle their own error/retry
     * concerns; a failing provider should not prevent others from contributing data.
     */
    List<CrisisEvent> fetchEvents();
}

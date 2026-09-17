package com.crisisscope.provider.pegelonline;

import com.crisisscope.model.CrisisEvent;
import com.crisisscope.provider.CrisisDataProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Supplies {@link CrisisEvent}s from PEGELONLINE, the official REST API of
 * the WSV (Wasserstraßen- und Schifffahrtsverwaltung des Bundes, Germany's
 * federal waterways and shipping administration).
 * <p>
 * Every station reports its current water level regardless of severity, so
 * {@link FloodRiverLevelMapper} keeps only those classified as elevated
 * against the station's mean high water statistic. Results are cached
 * briefly ({@link PegelonlineProperties#getCacheTtl()}) since the dashboard
 * polls far more often than gauge readings change. A failed fetch/parse falls
 * back to the last known-good result rather than propagating, per
 * {@link CrisisDataProvider}'s contract that one provider's failure should not
 * affect others.
 */
@Component
public class FloodRiverLevelCrisisDataProvider implements CrisisDataProvider {

    private static final String PROVIDER_ID = "pegelonline-river-levels";

    private static final Logger log = LoggerFactory.getLogger(FloodRiverLevelCrisisDataProvider.class);

    private final PegelonlineClient client;
    private final Clock clock;
    private final PegelonlineProperties properties;

    private CachedReadings cache;

    public FloodRiverLevelCrisisDataProvider(PegelonlineClient client, Clock clock, PegelonlineProperties properties) {
        this.client = client;
        this.clock = clock;
        this.properties = properties;
    }

    @Override
    public String id() {
        return PROVIDER_ID;
    }

    @Override
    public synchronized List<CrisisEvent> fetchEvents() {
        Instant now = clock.instant();
        if (cache != null && Duration.between(cache.fetchedAt(), now).compareTo(properties.getCacheTtl()) < 0) {
            return cache.events();
        }

        try {
            List<PegelonlineStation> stations = client.fetchStations();
            List<CrisisEvent> events = FloodRiverLevelMapper.toEvents(PROVIDER_ID, stations);
            cache = new CachedReadings(events, now);
            return events;
        } catch (Exception e) {
            if (cache != null) {
                log.warn("Failed to refresh PEGELONLINE river levels; serving cached data from {}", cache.fetchedAt(), e);
                return cache.events();
            }
            log.warn("Failed to fetch PEGELONLINE river levels and no cached data is available", e);
            return List.of();
        }
    }

    private record CachedReadings(List<CrisisEvent> events, Instant fetchedAt) {
    }
}

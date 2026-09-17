package com.crisisscope.provider.dwd;

import com.crisisscope.model.CrisisEvent;
import com.crisisscope.provider.CrisisDataProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Supplies {@link CrisisEvent}s from DWD's (Deutscher Wetterdienst) official
 * community-level weather warnings feed.
 * <p>
 * The feed responds as JSONP (e.g. {@code warnWetter.loadWarnings({...});}); the
 * wrapper is stripped before parsing. Results are cached briefly
 * ({@link DwdWarningsProperties#getCacheTtl()}) since the dashboard polls far
 * more often than DWD updates warnings. A failed fetch/parse falls back to the
 * last known-good result rather than propagating, per {@link CrisisDataProvider}'s
 * contract that one provider's failure should not affect others.
 */
@Component
public class DwdWeatherWarningCrisisDataProvider implements CrisisDataProvider {

    private static final String PROVIDER_ID = "dwd-weather-warnings";

    private static final Logger log = LoggerFactory.getLogger(DwdWeatherWarningCrisisDataProvider.class);

    private final DwdWarningsClient client;
    private final ObjectMapper objectMapper;
    private final Clock clock;
    private final DwdWarningsProperties properties;

    private CachedWarnings cache;

    public DwdWeatherWarningCrisisDataProvider(DwdWarningsClient client, ObjectMapper objectMapper,
                                                Clock clock, DwdWarningsProperties properties) {
        this.client = client;
        this.objectMapper = objectMapper;
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
            String rawPayload = client.fetchRawWarningsPayload();
            DwdWarningsResponse response = objectMapper.readValue(stripJsonpWrapper(rawPayload), DwdWarningsResponse.class);
            List<CrisisEvent> events = DwdWarningMapper.toEvents(PROVIDER_ID, response);
            cache = new CachedWarnings(events, now);
            return events;
        } catch (Exception e) {
            if (cache != null) {
                log.warn("Failed to refresh DWD weather warnings; serving cached data from {}", cache.fetchedAt(), e);
                return cache.events();
            }
            log.warn("Failed to fetch DWD weather warnings and no cached data is available", e);
            return List.of();
        }
    }

    private static String stripJsonpWrapper(String rawPayload) {
        int start = rawPayload.indexOf('{');
        int end = rawPayload.lastIndexOf('}');
        if (start < 0 || end < start) {
            throw new IllegalArgumentException("Unexpected DWD warnings payload format");
        }
        return rawPayload.substring(start, end + 1);
    }

    private record CachedWarnings(List<CrisisEvent> events, Instant fetchedAt) {
    }
}

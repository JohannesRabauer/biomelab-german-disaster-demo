package com.crisisscope.provider.bbk;

import com.crisisscope.model.CrisisEvent;
import com.crisisscope.provider.CrisisDataProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Supplies {@link CrisisEvent}s from BBK's official public warnings feed —
 * the same backend that powers the NINA app and aggregates MoWaS, KATWARN and
 * BIWAPP civil protection alerts.
 * <p>
 * Each configured channel is fetched independently, and each alert's detail is
 * fetched independently: a failure fetching one channel or one alert is
 * logged and skipped rather than aborting the whole refresh, per
 * {@link CrisisDataProvider}'s contract that one provider's failure should
 * not affect others (and, here, that one bad warning shouldn't hide the rest).
 * Results are cached briefly ({@link BbkWarningsProperties#getCacheTtl()})
 * since the dashboard polls far more often than BBK publishes updates. If
 * every channel fails on a refresh, the last known-good result is served
 * instead of an empty list.
 */
@Component
public class BbkCivilProtectionWarningProvider implements CrisisDataProvider {

    private static final String PROVIDER_ID = "bbk-warnings";

    private static final Logger log = LoggerFactory.getLogger(BbkCivilProtectionWarningProvider.class);

    private final BbkWarningsClient client;
    private final Clock clock;
    private final BbkWarningsProperties properties;

    private CachedWarnings cache;

    public BbkCivilProtectionWarningProvider(BbkWarningsClient client, Clock clock, BbkWarningsProperties properties) {
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

        List<CrisisEvent> events = new ArrayList<>();
        boolean anyChannelSucceeded = false;
        for (String channel : properties.getChannels()) {
            try {
                List<BbkMapDataEntry> entries = client.fetchMapData(channel);
                anyChannelSucceeded = true;
                events.addAll(fetchEventsForChannel(channel, entries));
            } catch (Exception e) {
                log.warn("Failed to fetch BBK '{}' warning listing", channel, e);
            }
        }

        if (!anyChannelSucceeded) {
            if (cache != null) {
                log.warn("Failed to refresh BBK warnings from any channel; serving cached data from {}", cache.fetchedAt());
                return cache.events();
            }
            log.warn("Failed to fetch BBK warnings and no cached data is available");
            return List.of();
        }

        cache = new CachedWarnings(List.copyOf(events), now);
        return cache.events();
    }

    private List<CrisisEvent> fetchEventsForChannel(String channel, List<BbkMapDataEntry> entries) {
        List<CrisisEvent> channelEvents = new ArrayList<>();
        for (BbkMapDataEntry entry : entries) {
            if ("Cancel".equalsIgnoreCase(entry.type())) {
                continue;
            }
            try {
                BbkAlert alert = client.fetchAlertDetail(entry.id());
                BbkWarningMapper.toEvent(PROVIDER_ID, alert).ifPresent(channelEvents::add);
            } catch (Exception e) {
                log.warn("Failed to fetch BBK '{}' warning detail for id {}", channel, entry.id(), e);
            }
        }
        return channelEvents;
    }

    private record CachedWarnings(List<CrisisEvent> events, Instant fetchedAt) {
    }
}

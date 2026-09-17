package com.crisisscope.provider.bbk;

import com.crisisscope.model.CrisisEvent;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class BbkCivilProtectionWarningProviderTest {

    @Test
    void fetchesAndAggregatesEventsAcrossChannelsSkippingCancelledListings() {
        StubClient client = StubClient.happyPath();
        BbkCivilProtectionWarningProvider provider = provider(client, Clock.fixed(Instant.EPOCH, ZoneId.of("UTC")),
                Duration.ofMinutes(5));

        List<CrisisEvent> events = provider.fetchEvents();

        assertThat(provider.id()).isEqualTo("bbk-warnings");
        assertThat(events).hasSize(2);
        assertThat(events).extracting(CrisisEvent::id)
                .containsExactlyInAnyOrder("bbk-mow.DE-SN-DD-W100-20260916-000", "bbk-kat.abc123_public_topics");
        // The cancelled mowas listing entry must never trigger a detail fetch.
        assertThat(client.detailCallCount("mow.DE-SN-DD-W100-20260916-001")).isZero();
    }

    @Test
    void oneFailingChannelDoesNotPreventOthersFromContributingEvents() {
        StubClient client = StubClient.happyPath();
        client.failChannel("mowas");
        BbkCivilProtectionWarningProvider provider = provider(client, Clock.fixed(Instant.EPOCH, ZoneId.of("UTC")),
                Duration.ofMinutes(5));

        List<CrisisEvent> events = provider.fetchEvents();

        assertThat(events).hasSize(1);
        assertThat(events).extracting(CrisisEvent::id).containsExactly("bbk-kat.abc123_public_topics");
    }

    @Test
    void oneFailingDetailFetchDoesNotPreventOtherAlertsInSameChannel() {
        StubClient client = StubClient.happyPath();
        client.failDetail("mow.DE-SN-DD-W100-20260916-000");
        BbkCivilProtectionWarningProvider provider = provider(client, Clock.fixed(Instant.EPOCH, ZoneId.of("UTC")),
                Duration.ofMinutes(5));

        List<CrisisEvent> events = provider.fetchEvents();

        assertThat(events).extracting(CrisisEvent::id).containsExactly("bbk-kat.abc123_public_topics");
    }

    @Test
    void returnsEmptyListWhenEveryChannelFailsAndNoCacheExists() {
        StubClient client = StubClient.failingEverywhere();
        BbkCivilProtectionWarningProvider provider = provider(client, Clock.fixed(Instant.EPOCH, ZoneId.of("UTC")),
                Duration.ofMinutes(5));

        assertThat(provider.fetchEvents()).isEmpty();
    }

    @Test
    void servesCachedResultWithinTtl() {
        StubClient client = StubClient.happyPath();
        MutableClock clock = new MutableClock(Instant.EPOCH);
        BbkCivilProtectionWarningProvider provider = provider(client, clock, Duration.ofMinutes(5));

        provider.fetchEvents();
        int callsAfterFirstFetch = client.mapDataCallCount("mowas");
        clock.advance(Duration.ofMinutes(4));
        List<CrisisEvent> secondCall = provider.fetchEvents();

        assertThat(client.mapDataCallCount("mowas")).isEqualTo(callsAfterFirstFetch);
        assertThat(secondCall).hasSize(2);
    }

    @Test
    void refetchesAfterCacheTtlExpires() {
        StubClient client = StubClient.happyPath();
        MutableClock clock = new MutableClock(Instant.EPOCH);
        BbkCivilProtectionWarningProvider provider = provider(client, clock, Duration.ofMinutes(5));

        provider.fetchEvents();
        clock.advance(Duration.ofMinutes(6));
        provider.fetchEvents();

        assertThat(client.mapDataCallCount("mowas")).isEqualTo(2);
    }

    @Test
    void fallsBackToCachedResultWhenEveryChannelFailsOnRefresh() {
        StubClient client = StubClient.happyPath();
        MutableClock clock = new MutableClock(Instant.EPOCH);
        BbkCivilProtectionWarningProvider provider = provider(client, clock, Duration.ofMinutes(5));

        List<CrisisEvent> firstCall = provider.fetchEvents();
        clock.advance(Duration.ofMinutes(6));
        client.failChannel("mowas");
        client.failChannel("katwarn");
        client.failChannel("biwapp");
        List<CrisisEvent> secondCall = provider.fetchEvents();

        assertThat(secondCall).isEqualTo(firstCall);
    }

    private static BbkCivilProtectionWarningProvider provider(StubClient client, Clock clock, Duration cacheTtl) {
        BbkWarningsProperties properties = new BbkWarningsProperties();
        properties.setCacheTtl(cacheTtl);
        properties.setChannels(List.of("mowas", "katwarn", "biwapp"));
        return new BbkCivilProtectionWarningProvider(client, clock, properties);
    }

    private static class StubClient implements BbkWarningsClient {
        private final Map<String, List<BbkMapDataEntry>> mapDataByChannel;
        private final Map<String, BbkAlert> alertsById;
        private final Map<String, AtomicInteger> mapDataCalls = new HashMap<>();
        private final Map<String, AtomicInteger> detailCalls = new HashMap<>();
        private final java.util.Set<String> failingChannels = new java.util.HashSet<>();
        private final java.util.Set<String> failingDetailIds = new java.util.HashSet<>();
        private boolean failEverything;

        private StubClient(Map<String, List<BbkMapDataEntry>> mapDataByChannel, Map<String, BbkAlert> alertsById) {
            this.mapDataByChannel = mapDataByChannel;
            this.alertsById = alertsById;
        }

        static StubClient happyPath() {
            Map<String, List<BbkMapDataEntry>> mapData = new HashMap<>();
            mapData.put("mowas", List.of(
                    new BbkMapDataEntry("mow.DE-SN-DD-W100-20260916-000", 3, "Severe", "Alert"),
                    new BbkMapDataEntry("mow.DE-SN-DD-W100-20260916-001", 4, "Severe", "Cancel")));
            mapData.put("katwarn", List.of(
                    new BbkMapDataEntry("kat.abc123_public_topics", 20, "Moderate", "Alert")));
            mapData.put("biwapp", List.of());

            Map<String, BbkAlert> alerts = new HashMap<>();
            alerts.put("mow.DE-SN-DD-W100-20260916-000", floodAlert());
            alerts.put("kat.abc123_public_topics", healthAlert());

            return new StubClient(mapData, alerts);
        }

        static StubClient failingEverywhere() {
            StubClient client = new StubClient(Map.of(), Map.of());
            client.failEverything = true;
            return client;
        }

        void failChannel(String channel) {
            failingChannels.add(channel);
        }

        void failDetail(String id) {
            failingDetailIds.add(id);
        }

        int mapDataCallCount(String channel) {
            return mapDataCalls.getOrDefault(channel, new AtomicInteger()).get();
        }

        int detailCallCount(String id) {
            return detailCalls.getOrDefault(id, new AtomicInteger()).get();
        }

        @Override
        public List<BbkMapDataEntry> fetchMapData(String channel) {
            mapDataCalls.computeIfAbsent(channel, c -> new AtomicInteger()).incrementAndGet();
            if (failEverything || failingChannels.contains(channel)) {
                throw new RuntimeException("simulated failure for channel " + channel);
            }
            return mapDataByChannel.getOrDefault(channel, List.of());
        }

        @Override
        public BbkAlert fetchAlertDetail(String id) {
            detailCalls.computeIfAbsent(id, i -> new AtomicInteger()).incrementAndGet();
            if (failEverything || failingDetailIds.contains(id)) {
                throw new RuntimeException("simulated failure for id " + id);
            }
            return alertsById.get(id);
        }

        private static BbkAlert floodAlert() {
            BbkAlertInfo info = new BbkAlertInfo("de", List.of("Met"), "Hochwasser", "Severe",
                    "2026-09-16T08:00:00+02:00", "2026-09-16T08:00:00+02:00", "2026-09-17T08:00:00+02:00",
                    "Hochwasserwarnung für die Elbe bei Dresden",
                    "Der Pegel der Elbe hat die Meldestufe 2 überschritten.", null,
                    "https://www.pegelonline.wsv.de/", List.of(new BbkArea("Dresden, Elbtal")));
            return new BbkAlert("mow.DE-SN-DD-W100-20260916-000", "DE-SN-DD-W100",
                    "2026-09-16T08:15:00+02:00", "Alert", List.of(info));
        }

        private static BbkAlert healthAlert() {
            BbkAlertInfo info = new BbkAlertInfo("de", List.of("Health"), "DrinkingWater", "Moderate",
                    "2026-09-14T15:29:00+02:00", null, null,
                    "Vogelsbergkreis meldet: Warnung Trinkwasserunfall",
                    "Im Trinkwasser wurde eine Trübung festgestellt.", null, null,
                    List.of(new BbkArea("Teile von Lauterbach")));
            return new BbkAlert("kat.abc123_public_topics", "CAP@katwarn.de",
                    "2026-09-14T15:29:25+02:00", "Alert", List.of(info));
        }
    }

    private static class MutableClock extends Clock {
        private Instant instant;

        MutableClock(Instant instant) {
            this.instant = instant;
        }

        void advance(Duration duration) {
            instant = instant.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}

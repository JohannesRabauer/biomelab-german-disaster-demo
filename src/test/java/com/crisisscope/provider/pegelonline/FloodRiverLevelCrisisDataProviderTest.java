package com.crisisscope.provider.pegelonline;

import com.crisisscope.model.CrisisEvent;
import com.crisisscope.model.Severity;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class FloodRiverLevelCrisisDataProviderTest {

    @Test
    void fetchesAndParsesOnlyElevatedStations() {
        StubClient client = StubClient.withFixtureStations();
        FloodRiverLevelCrisisDataProvider provider = provider(client, Clock.fixed(Instant.EPOCH, ZoneId.of("UTC")),
                Duration.ofMinutes(5));

        List<CrisisEvent> events = provider.fetchEvents();

        assertThat(provider.id()).isEqualTo("pegelonline-river-levels");
        assertThat(client.callCount.get()).isEqualTo(1);
        assertThat(events).hasSize(2);
        assertThat(events).extracting(CrisisEvent::id)
                .containsExactlyInAnyOrder("pegelonline-501060", "pegelonline-48900237");
        assertThat(events).extracting(CrisisEvent::severity)
                .containsExactlyInAnyOrder(Severity.CRITICAL, Severity.HIGH);
    }

    @Test
    void servesCachedResultWithinTtl() {
        StubClient client = StubClient.withFixtureStations();
        MutableClock clock = new MutableClock(Instant.EPOCH);
        FloodRiverLevelCrisisDataProvider provider = provider(client, clock, Duration.ofMinutes(5));

        provider.fetchEvents();
        clock.advance(Duration.ofMinutes(4));
        List<CrisisEvent> secondCall = provider.fetchEvents();

        assertThat(client.callCount.get()).isEqualTo(1);
        assertThat(secondCall).hasSize(2);
    }

    @Test
    void refetchesAfterCacheTtlExpires() {
        StubClient client = StubClient.withFixtureStations();
        MutableClock clock = new MutableClock(Instant.EPOCH);
        FloodRiverLevelCrisisDataProvider provider = provider(client, clock, Duration.ofMinutes(5));

        provider.fetchEvents();
        clock.advance(Duration.ofMinutes(6));
        provider.fetchEvents();

        assertThat(client.callCount.get()).isEqualTo(2);
    }

    @Test
    void fallsBackToCachedResultWhenRefetchFails() {
        StubClient client = StubClient.withFixtureStations();
        MutableClock clock = new MutableClock(Instant.EPOCH);
        FloodRiverLevelCrisisDataProvider provider = provider(client, clock, Duration.ofMinutes(5));

        List<CrisisEvent> firstCall = provider.fetchEvents();
        clock.advance(Duration.ofMinutes(6));
        client.failNextCall = true;
        List<CrisisEvent> secondCall = provider.fetchEvents();

        assertThat(secondCall).isEqualTo(firstCall);
    }

    @Test
    void returnsEmptyListWhenFirstFetchFailsAndNoCacheExists() {
        StubClient client = StubClient.failingClient();
        FloodRiverLevelCrisisDataProvider provider = provider(client, Clock.fixed(Instant.EPOCH, ZoneId.of("UTC")),
                Duration.ofMinutes(5));

        assertThat(provider.fetchEvents()).isEmpty();
    }

    private static FloodRiverLevelCrisisDataProvider provider(StubClient client, Clock clock, Duration cacheTtl) {
        PegelonlineProperties properties = new PegelonlineProperties();
        properties.setCacheTtl(cacheTtl);
        return new FloodRiverLevelCrisisDataProvider(client, clock, properties);
    }

    private static class StubClient implements PegelonlineClient {
        private final List<PegelonlineStation> stations;
        private final AtomicInteger callCount = new AtomicInteger();
        private boolean failNextCall = false;

        private StubClient(List<PegelonlineStation> stations) {
            this.stations = stations;
        }

        static StubClient withFixtureStations() {
            PegelonlineMeasurement critical = new PegelonlineMeasurement("2026-09-17T08:00:00+02:00", 750.0, "hoch", "hoch");
            PegelonlineMeasurement high = new PegelonlineMeasurement("2026-09-17T08:15:00+02:00", 620.5, "hoch", "normal");
            PegelonlineMeasurement normal = new PegelonlineMeasurement("2026-09-17T08:10:00+02:00", 210.0, "normal", "normal");

            PegelonlineStation dresden = new PegelonlineStation("uuid-1", "501060", "DRESDEN", "DRESDEN",
                    55.63, "WSA ELBE", 13.738831, 51.055988, new PegelonlineWater("ELBE", "ELBE"),
                    List.of(new PegelonlineTimeseries("W", "Wasserstand", "cm", critical)));
            PegelonlineStation koeln = new PegelonlineStation("uuid-2", "48900237", "KOELN", "KOELN",
                    688.15, "WSA RHEIN", 6.964, 50.9382, new PegelonlineWater("RHEIN", "RHEIN"),
                    List.of(new PegelonlineTimeseries("W", "Wasserstand", "cm", high)));
            PegelonlineStation hamburg = new PegelonlineStation("uuid-3", "10010000", "HAMBURG", "HAMBURG",
                    623.60, "WSA ELBE-NORDSEE", 9.9698, 53.5461, new PegelonlineWater("ELBE", "ELBE"),
                    List.of(new PegelonlineTimeseries("W", "Wasserstand", "cm", normal)));

            return new StubClient(List.of(dresden, koeln, hamburg));
        }

        static StubClient failingClient() {
            StubClient client = new StubClient(List.of());
            client.failNextCall = true;
            return client;
        }

        @Override
        public List<PegelonlineStation> fetchStations() {
            callCount.incrementAndGet();
            if (failNextCall) {
                failNextCall = false;
                throw new RuntimeException("simulated network failure");
            }
            return stations;
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

package com.crisisscope.provider.dwd;

import com.crisisscope.model.CrisisEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class DwdWeatherWarningCrisisDataProviderTest {

    @Test
    void fetchesAndParsesEventsFromFixturePayload() {
        StubClient client = StubClient.withFixturePayload();
        DwdWeatherWarningCrisisDataProvider provider = provider(client, Clock.fixed(Instant.EPOCH, ZoneId.of("UTC")),
                Duration.ofMinutes(5));

        List<CrisisEvent> events = provider.fetchEvents();

        assertThat(provider.id()).isEqualTo("dwd-weather-warnings");
        assertThat(client.callCount.get()).isEqualTo(1);
        assertThat(events).hasSize(2);
        assertThat(events).extracting(CrisisEvent::id).containsExactlyInAnyOrder("dwd-22334455", "dwd-99887766");
    }

    @Test
    void servesCachedResultWithinTtl() {
        StubClient client = StubClient.withFixturePayload();
        MutableClock clock = new MutableClock(Instant.EPOCH);
        DwdWeatherWarningCrisisDataProvider provider = provider(client, clock, Duration.ofMinutes(5));

        provider.fetchEvents();
        clock.advance(Duration.ofMinutes(4));
        List<CrisisEvent> secondCall = provider.fetchEvents();

        assertThat(client.callCount.get()).isEqualTo(1);
        assertThat(secondCall).hasSize(2);
    }

    @Test
    void refetchesAfterCacheTtlExpires() {
        StubClient client = StubClient.withFixturePayload();
        MutableClock clock = new MutableClock(Instant.EPOCH);
        DwdWeatherWarningCrisisDataProvider provider = provider(client, clock, Duration.ofMinutes(5));

        provider.fetchEvents();
        clock.advance(Duration.ofMinutes(6));
        provider.fetchEvents();

        assertThat(client.callCount.get()).isEqualTo(2);
    }

    @Test
    void fallsBackToCachedResultWhenRefetchFails() {
        StubClient client = StubClient.withFixturePayload();
        MutableClock clock = new MutableClock(Instant.EPOCH);
        DwdWeatherWarningCrisisDataProvider provider = provider(client, clock, Duration.ofMinutes(5));

        List<CrisisEvent> firstCall = provider.fetchEvents();
        clock.advance(Duration.ofMinutes(6));
        client.failNextCall = true;
        List<CrisisEvent> secondCall = provider.fetchEvents();

        assertThat(secondCall).isEqualTo(firstCall);
    }

    @Test
    void returnsEmptyListWhenFirstFetchFailsAndNoCacheExists() {
        StubClient client = StubClient.failingClient();
        DwdWeatherWarningCrisisDataProvider provider = provider(client, Clock.fixed(Instant.EPOCH, ZoneId.of("UTC")),
                Duration.ofMinutes(5));

        assertThat(provider.fetchEvents()).isEmpty();
    }

    private static DwdWeatherWarningCrisisDataProvider provider(StubClient client, Clock clock, Duration cacheTtl) {
        DwdWarningsProperties properties = new DwdWarningsProperties();
        properties.setCacheTtl(cacheTtl);
        return new DwdWeatherWarningCrisisDataProvider(client, new ObjectMapper(), clock, properties);
    }

    private static class StubClient implements DwdWarningsClient {
        private final String payload;
        private final AtomicInteger callCount = new AtomicInteger();
        private boolean failNextCall = false;

        private StubClient(String payload) {
            this.payload = payload;
        }

        static StubClient withFixturePayload() {
            return new StubClient(readFixture());
        }

        static StubClient failingClient() {
            StubClient client = new StubClient(null);
            client.failNextCall = true;
            return client;
        }

        @Override
        public String fetchRawWarningsPayload() {
            callCount.incrementAndGet();
            if (failNextCall) {
                failNextCall = false;
                throw new RuntimeException("simulated network failure");
            }
            return payload;
        }

        private static String readFixture() {
            try (InputStream in = StubClient.class.getResourceAsStream("/fixtures/dwd/warnungen_gemeinde_map.json")) {
                return new String(in.readAllBytes(), StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
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

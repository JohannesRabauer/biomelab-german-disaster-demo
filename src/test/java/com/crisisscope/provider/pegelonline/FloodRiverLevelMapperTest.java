package com.crisisscope.provider.pegelonline;

import com.crisisscope.model.CrisisEvent;
import com.crisisscope.model.EventType;
import com.crisisscope.model.Location;
import com.crisisscope.model.Severity;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FloodRiverLevelMapperTest {

    @Test
    void mapsRealisticFixturePayloadKeepingOnlyElevatedWaterLevelStations() throws IOException {
        PegelonlineStation[] stations;
        try (InputStream in = getClass().getResourceAsStream("/fixtures/pegelonline/stations.json")) {
            stations = new ObjectMapper().readValue(in, PegelonlineStation[].class);
        }

        List<CrisisEvent> events = FloodRiverLevelMapper.toEvents("pegelonline-river-levels", List.of(stations));

        assertThat(events).hasSize(2);
        assertThat(events).extracting(CrisisEvent::id)
                .containsExactlyInAnyOrder("pegelonline-501060", "pegelonline-48900237");
        assertThat(events).extracting(CrisisEvent::severity)
                .containsExactlyInAnyOrder(Severity.CRITICAL, Severity.HIGH);
    }

    @Test
    void classifiesReadingsAboveBothMeanAndHistoricalHighWaterAsCritical() {
        PegelonlineMeasurement measurement = new PegelonlineMeasurement("2026-09-17T08:00:00+02:00", 750.0, "hoch", "hoch");
        assertThat(FloodRiverLevelMapper.isElevated(measurement)).isTrue();
        assertThat(FloodRiverLevelMapper.classifySeverity(measurement)).isEqualTo(Severity.CRITICAL);
    }

    @Test
    void classifiesReadingsAboveMeanHighWaterOnlyAsHigh() {
        PegelonlineMeasurement measurement = new PegelonlineMeasurement("2026-09-17T08:00:00+02:00", 620.5, "hoch", "normal");
        assertThat(FloodRiverLevelMapper.isElevated(measurement)).isTrue();
        assertThat(FloodRiverLevelMapper.classifySeverity(measurement)).isEqualTo(Severity.HIGH);
    }

    @Test
    void doesNotClassifyNormalOrLowReadingsAsElevated() {
        assertThat(FloodRiverLevelMapper.isElevated(new PegelonlineMeasurement("t", 210.0, "normal", "normal"))).isFalse();
        assertThat(FloodRiverLevelMapper.isElevated(new PegelonlineMeasurement("t", 50.0, "niedrig", "niedrig"))).isFalse();
        assertThat(FloodRiverLevelMapper.isElevated(null)).isFalse();
    }

    @Test
    void resolvesLocationFromStationCoordinatesAndNearestState() {
        PegelonlineStation dresden = station("DRESDEN", "ELBE", 51.055988, 13.738831,
                new PegelonlineMeasurement("t", 750.0, "hoch", "hoch"));

        Location location = FloodRiverLevelMapper.resolveLocation(dresden);

        assertThat(location.name()).isEqualTo("DRESDEN");
        assertThat(location.region()).isEqualTo("Sachsen");
        assertThat(location.latitude()).isEqualTo(51.055988);
        assertThat(location.longitude()).isEqualTo(13.738831);
    }

    @Test
    void titleEscalatesToFloodWarningWhenCritical() {
        assertThat(FloodRiverLevelMapper.resolveTitle(
                station("DRESDEN", "ELBE", 51.0, 13.7, null), Severity.CRITICAL))
                .isEqualTo("Flood warning: ELBE at DRESDEN");
        assertThat(FloodRiverLevelMapper.resolveTitle(
                station("KOELN", "RHEIN", 50.9, 6.9, null), Severity.HIGH))
                .isEqualTo("High water level: RHEIN at KOELN");
    }

    @Test
    void descriptionMentionsLevelUnitAndHistoricalMarkOnlyWhenCritical() {
        PegelonlineMeasurement measurement = new PegelonlineMeasurement("2026-09-17T08:00:00+02:00", 750.0, "hoch", "hoch");
        PegelonlineTimeseries series = new PegelonlineTimeseries("W", "Wasserstand", "cm", measurement);
        PegelonlineStation dresden = station("DRESDEN", "ELBE", 51.055988, 13.738831, measurement);

        String description = FloodRiverLevelMapper.composeDescription(dresden, series, measurement, Severity.CRITICAL);

        assertThat(description).contains("750 cm");
        assertThat(description).contains("historical high-water mark");
        assertThat(description).contains("Measured:");
    }

    @Test
    void mapsOnlyElevatedWaterLevelStationsAndSkipsNonWaterLevelSeries() {
        PegelonlineStation elevatedCritical = station("DRESDEN", "ELBE", 51.055988, 13.738831,
                new PegelonlineMeasurement("2026-09-17T08:00:00+02:00", 750.0, "hoch", "hoch"));
        PegelonlineStation elevatedHigh = station("KOELN", "RHEIN", 50.9382, 6.964,
                new PegelonlineMeasurement("2026-09-17T08:15:00+02:00", 620.5, "hoch", "normal"));
        PegelonlineStation normal = station("HAMBURG", "ELBE", 53.5461, 9.9698,
                new PegelonlineMeasurement("2026-09-17T08:10:00+02:00", 210.0, "normal", "normal"));
        PegelonlineStation dischargeOnly = new PegelonlineStation("uuid-4", "48610030", "MAINZ", "MAINZ",
                498.66, "WSA RHEIN", 8.2712, 50.0007,
                new PegelonlineWater("RHEIN", "RHEIN"),
                List.of(new PegelonlineTimeseries("Q", "Durchfluss", "m3/s",
                        new PegelonlineMeasurement("t", 1500.0, "hoch", "hoch"))));

        List<CrisisEvent> events = FloodRiverLevelMapper.toEvents("pegelonline-river-levels",
                List.of(elevatedCritical, elevatedHigh, normal, dischargeOnly));

        assertThat(events).hasSize(2);
        assertThat(events).extracting(CrisisEvent::id)
                .containsExactlyInAnyOrder("pegelonline-num-DRESDEN", "pegelonline-num-KOELN");
        assertThat(events).allMatch(e -> e.type() == EventType.FLOOD);
        assertThat(events).allMatch(e -> e.sourceId().equals("pegelonline-river-levels"));
        CrisisEvent critical = events.stream().filter(e -> e.id().equals("pegelonline-num-DRESDEN")).findFirst().orElseThrow();
        assertThat(critical.severity()).isEqualTo(Severity.CRITICAL);
        assertThat(critical.occurredAt()).isEqualTo(Instant.parse("2026-09-17T06:00:00Z"));
    }

    private static PegelonlineStation station(String longname, String water, double lat, double lon,
                                               PegelonlineMeasurement measurement) {
        List<PegelonlineTimeseries> timeseries = measurement != null
                ? List.of(new PegelonlineTimeseries("W", "Wasserstand", "cm", measurement))
                : List.of();
        return new PegelonlineStation("uuid-" + longname, "num-" + longname, longname, longname,
                12.3, "AGENCY", lon, lat, new PegelonlineWater(water, water), timeseries);
    }
}

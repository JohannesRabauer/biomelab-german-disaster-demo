package com.crisisscope.provider.pegelonline;

import com.crisisscope.model.CrisisEvent;
import com.crisisscope.model.EventType;
import com.crisisscope.model.Location;
import com.crisisscope.model.Severity;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Converts PEGELONLINE stations into {@link CrisisEvent}s.
 * <p>
 * PEGELONLINE reports the current water level for every station regardless of
 * whether it is unremarkable, so only stations whose reading is classified as
 * {@code hoch} ("high") against the station's mean high water statistic are
 * turned into events — otherwise every one of the several hundred stations
 * would produce a "flood" marker. A reading that additionally exceeds the
 * station's recorded historical high-water mark is escalated to
 * {@link Severity#CRITICAL}.
 */
final class FloodRiverLevelMapper {

    private static final String ATTRIBUTION_URL = "https://www.pegelonline.wsv.de/gast/start";
    private static final String WATER_LEVEL_SERIES = "W";
    private static final String ABOVE_MEAN_HIGH_WATER = "hoch";

    private static final DateTimeFormatter MEASURED_AT_FORMAT =
            DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm", Locale.ENGLISH).withZone(ZoneId.of("Europe/Berlin"));

    private FloodRiverLevelMapper() {
    }

    static List<CrisisEvent> toEvents(String providerId, List<PegelonlineStation> stations) {
        if (stations == null) {
            return List.of();
        }
        return stations.stream()
                .map(FloodRiverLevelMapper::waterLevelReading)
                .flatMap(Optional::stream)
                .filter(reading -> isElevated(reading.measurement()))
                .map(reading -> toEvent(providerId, reading))
                .toList();
    }

    private record Reading(PegelonlineStation station, PegelonlineTimeseries series, PegelonlineMeasurement measurement) {
    }

    private static Optional<Reading> waterLevelReading(PegelonlineStation station) {
        if (station.timeseries() == null) {
            return Optional.empty();
        }
        return station.timeseries().stream()
                .filter(series -> WATER_LEVEL_SERIES.equalsIgnoreCase(series.shortname()))
                .filter(series -> series.currentMeasurement() != null)
                .findFirst()
                .map(series -> new Reading(station, series, series.currentMeasurement()));
    }

    static boolean isElevated(PegelonlineMeasurement measurement) {
        return measurement != null && ABOVE_MEAN_HIGH_WATER.equalsIgnoreCase(measurement.stateMnwMhw());
    }

    static Severity classifySeverity(PegelonlineMeasurement measurement) {
        return ABOVE_MEAN_HIGH_WATER.equalsIgnoreCase(measurement.stateNswHsw()) ? Severity.CRITICAL : Severity.HIGH;
    }

    private static CrisisEvent toEvent(String providerId, Reading reading) {
        PegelonlineStation station = reading.station();
        PegelonlineMeasurement measurement = reading.measurement();
        Severity severity = classifySeverity(measurement);
        Instant measuredAt = parseInstant(measurement.timestamp());

        return new CrisisEvent(
                "pegelonline-" + stationKey(station),
                providerId,
                resolveTitle(station, severity),
                composeDescription(station, reading.series(), measurement, severity),
                EventType.FLOOD,
                severity,
                resolveLocation(station),
                measuredAt != null ? measuredAt : Instant.EPOCH,
                measuredAt != null ? measuredAt : Instant.EPOCH,
                ATTRIBUTION_URL
        );
    }

    private static String stationKey(PegelonlineStation station) {
        if (station.number() != null && !station.number().isBlank()) {
            return station.number();
        }
        return station.uuid();
    }

    static String resolveTitle(PegelonlineStation station, Severity severity) {
        String waterName = waterName(station);
        String stationName = stationName(station);
        return severity == Severity.CRITICAL
                ? "Flood warning: " + waterName + " at " + stationName
                : "High water level: " + waterName + " at " + stationName;
    }

    static String composeDescription(PegelonlineStation station, PegelonlineTimeseries series,
                                      PegelonlineMeasurement measurement, Severity severity) {
        StringBuilder text = new StringBuilder();
        text.append("Gauge ").append(stationName(station))
                .append(" on the ").append(waterName(station));
        if (station.km() != null) {
            text.append(" (river km ").append(station.km()).append(")");
        }
        text.append(" is reporting ").append(formatValue(measurement.value(), series.unit()))
                .append(", above its mean high water level");
        if (severity == Severity.CRITICAL) {
            text.append(" and above its recorded historical high-water mark");
        }
        text.append(".");

        Instant measuredAt = parseInstant(measurement.timestamp());
        if (measuredAt != null) {
            text.append(" Measured: ").append(MEASURED_AT_FORMAT.format(measuredAt)).append(" (Europe/Berlin).");
        }
        return text.toString();
    }

    private static String formatValue(Double value, String unit) {
        if (value == null) {
            return "an elevated level";
        }
        String formatted = value == Math.rint(value) ? String.valueOf(value.intValue()) : String.valueOf(value);
        return formatted + (unit != null && !unit.isBlank() ? " " + unit : "");
    }

    private static String waterName(PegelonlineStation station) {
        if (station.water() == null) {
            return "Unknown waterway";
        }
        String longname = station.water().longname();
        if (longname != null && !longname.isBlank()) {
            return longname.strip();
        }
        String shortname = station.water().shortname();
        return shortname != null && !shortname.isBlank() ? shortname.strip() : "Unknown waterway";
    }

    private static String stationName(PegelonlineStation station) {
        if (station.longname() != null && !station.longname().isBlank()) {
            return station.longname().strip();
        }
        if (station.shortname() != null && !station.shortname().isBlank()) {
            return station.shortname().strip();
        }
        return "Unknown gauge";
    }

    static Location resolveLocation(PegelonlineStation station) {
        String region = GermanStateLookup.nearestState(station.latitude(), station.longitude());
        double latitude = station.latitude() != null ? station.latitude() : 51.1657;
        double longitude = station.longitude() != null ? station.longitude() : 10.4515;
        return new Location(stationName(station), region, latitude, longitude);
    }

    private static Instant parseInstant(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return OffsetDateTime.parse(value).toInstant();
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}

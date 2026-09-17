package com.crisisscope.provider.sample;

import com.crisisscope.model.CrisisEvent;
import com.crisisscope.model.EventType;
import com.crisisscope.model.Location;
import com.crisisscope.model.Severity;
import com.crisisscope.provider.CrisisDataProvider;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Static demo data so the dashboard has something to render before any real
 * provider is wired up. Not a real data source — safe to remove once actual
 * {@link CrisisDataProvider} implementations exist.
 */
@Component
public class SampleCrisisDataProvider implements CrisisDataProvider {

    private static final String PROVIDER_ID = "sample-data";

    private final Clock clock;

    public SampleCrisisDataProvider(Clock clock) {
        this.clock = clock;
    }

    @Override
    public String id() {
        return PROVIDER_ID;
    }

    @Override
    public List<CrisisEvent> fetchEvents() {
        Instant now = clock.instant();
        return List.of(
                event("evt-1", "Elbe water level exceeds warning stage",
                        "Rapid snowmelt and sustained rainfall pushed the Elbe past level 2 warning "
                                + "stage near Dresden. Riverside districts are being monitored for evacuation.",
                        EventType.FLOOD, Severity.HIGH,
                        new Location("Dresden", "Saxony", 51.0504, 13.7373),
                        now.minus(Duration.ofHours(2)), now.minus(Duration.ofMinutes(45)),
                        "https://www.pegelonline.wsv.de/"),
                event("evt-2", "Forest fire near Lübtheen contained to 40 hectares",
                        "Fire crews report the wildfire on the former military training ground is now "
                                + "60% contained. No settlements currently at risk.",
                        EventType.WILDFIRE, Severity.MODERATE,
                        new Location("Lübtheen", "Mecklenburg-Vorpommern", 53.3200, 11.1500),
                        now.minus(Duration.ofHours(9)), now.minus(Duration.ofHours(1)),
                        null),
                event("evt-3", "Severe thunderstorm warning for the Ruhr area",
                        "DWD has issued a level 3 warning for large hail and wind gusts up to 100 km/h "
                                + "through the evening across Dortmund, Essen and Bochum.",
                        EventType.STORM, Severity.HIGH,
                        new Location("Dortmund", "North Rhine-Westphalia", 51.5136, 7.4653),
                        now.minus(Duration.ofMinutes(30)), now.minus(Duration.ofMinutes(10)),
                        "https://www.dwd.de/warnungen"),
                event("evt-4", "Substation fault cuts power to 12,000 households",
                        "A transformer fault at the Marzahn substation has left large parts of eastern "
                                + "Berlin without power. Repair crews are on site.",
                        EventType.POWER_OUTAGE, Severity.MODERATE,
                        new Location("Berlin-Marzahn", "Berlin", 52.5447, 13.5843),
                        now.minus(Duration.ofMinutes(75)), now.minus(Duration.ofMinutes(60)),
                        null),
                event("evt-5", "Chemical odor reported near Leverkusen industrial park",
                        "Residents reported a strong chemical smell; fire department is investigating a "
                                + "possible release at the Chempark site. Windows advised to stay shut.",
                        EventType.INDUSTRIAL_ACCIDENT, Severity.CRITICAL,
                        new Location("Leverkusen", "North Rhine-Westphalia", 51.0459, 6.9852),
                        now.minus(Duration.ofMinutes(20)), now.minus(Duration.ofMinutes(5)),
                        "https://www.chempark.de/"),
                event("evt-6", "Multi-vehicle collision closes A9 near Nuremberg",
                        "Dense fog caused a chain collision involving 14 vehicles. The A9 is closed in "
                                + "both directions between Nuremberg and Feucht.",
                        EventType.TRAFFIC_ACCIDENT, Severity.MODERATE,
                        new Location("Nuremberg", "Bavaria", 49.4521, 11.0767),
                        now.minus(Duration.ofHours(4)), now.minus(Duration.ofHours(3)),
                        null),
                event("evt-7", "Norovirus outbreak reported at Hamburg care facility",
                        "Health authorities are monitoring a norovirus cluster affecting 30 residents and "
                                + "staff at a senior care home in Altona.",
                        EventType.PUBLIC_HEALTH, Severity.LOW,
                        new Location("Hamburg-Altona", "Hamburg", 53.5503, 9.9349),
                        now.minus(Duration.ofDays(1)), now.minus(Duration.ofHours(6)),
                        null),
                event("evt-8", "Rail bridge inspection closes line near Stuttgart",
                        "Deutsche Bahn closed the Neckar viaduct for emergency structural inspection after "
                                + "sensors detected unusual vibration readings.",
                        EventType.INFRASTRUCTURE_FAILURE, Severity.LOW,
                        new Location("Stuttgart", "Baden-Württemberg", 48.7758, 9.1829),
                        now.minus(Duration.ofHours(12)), now.minus(Duration.ofHours(11)),
                        "https://www.deutschebahn.com/"),
                event("evt-9", "Minor tremor felt across Cologne metro area",
                        "A magnitude 3.1 tremor originating near the Rurgraben fault was felt across "
                                + "Cologne. No damage reported so far.",
                        EventType.EARTHQUAKE, Severity.LOW,
                        new Location("Cologne", "North Rhine-Westphalia", 50.9375, 6.9603),
                        now.minus(Duration.ofHours(18)), now.minus(Duration.ofHours(17)),
                        null),
                event("evt-10", "Security cordon set up in Frankfurt banking district",
                        "Police established a cordon around Taunusanlage after a suspicious package was "
                                + "reported outside a bank headquarters.",
                        EventType.SECURITY_INCIDENT, Severity.HIGH,
                        new Location("Frankfurt am Main", "Hesse", 50.1109, 8.6821),
                        now.minus(Duration.ofMinutes(50)), now.minus(Duration.ofMinutes(15)),
                        "https://www.polizei.hessen.de/")
        );
    }

    private CrisisEvent event(String id, String title, String description, EventType type, Severity severity,
                               Location location, Instant occurredAt, Instant reportedAt, String url) {
        return new CrisisEvent(id, PROVIDER_ID, title, description, type, severity, location,
                occurredAt, reportedAt, url);
    }
}

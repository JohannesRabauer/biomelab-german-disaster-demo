package com.crisisscope.provider.dwd;

import com.crisisscope.model.CrisisEvent;
import com.crisisscope.model.EventType;
import com.crisisscope.model.Severity;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DwdWarningMapperTest {

    private static final Instant PUBLISHED_AT = Instant.ofEpochMilli(1_737_100_000_000L);

    @Test
    void mapsSeverityFromLevel() {
        assertThat(DwdWarningMapper.classifySeverity(1)).isEqualTo(Severity.LOW);
        assertThat(DwdWarningMapper.classifySeverity(2)).isEqualTo(Severity.MODERATE);
        assertThat(DwdWarningMapper.classifySeverity(3)).isEqualTo(Severity.HIGH);
        assertThat(DwdWarningMapper.classifySeverity(4)).isEqualTo(Severity.CRITICAL);
        assertThat(DwdWarningMapper.classifySeverity(99)).isEqualTo(Severity.MODERATE);
        assertThat(DwdWarningMapper.classifySeverity(null)).isEqualTo(Severity.MODERATE);
    }

    @Test
    void classifiesFloodWarningsByKeyword() {
        DwdWarning warning = warning("HOCHWASSER", null);
        assertThat(DwdWarningMapper.classifyType(warning)).isEqualTo(EventType.FLOOD);
    }

    @Test
    void classifiesThunderstormWarningsAsStorm() {
        DwdWarning warning = warning("STARKES GEWITTER", "Amtliche WARNUNG vor STARKEM GEWITTER");
        assertThat(DwdWarningMapper.classifyType(warning)).isEqualTo(EventType.STORM);
    }

    @Test
    void fallsBackToOtherForUnrecognizedEventText() {
        DwdWarning warning = warning("UNKNOWN EVENT KIND", null);
        assertThat(DwdWarningMapper.classifyType(warning)).isEqualTo(EventType.OTHER);
    }

    @Test
    void prefersHeadlineOverEventForTitle() {
        DwdWarning warning = warning("STURM", "Amtliche Sturmwarnung für die Küste");
        assertThat(DwdWarningMapper.resolveTitle(warning)).isEqualTo("Amtliche Sturmwarnung für die Küste");
    }

    @Test
    void titleCasesEventWhenHeadlineMissing() {
        DwdWarning warning = warning("STARKES GEWITTER", null);
        assertThat(DwdWarningMapper.resolveTitle(warning)).isEqualTo("Starkes Gewitter");
    }

    @Test
    void resolvesRepresentativeCoordinatesFromState() {
        DwdWarning warning = new DwdWarning(1L, 2, 1_737_100_800_000L, null,
                "Stadt Dresden", "Sachsen", "SN", "STURM", null, "desc", null);

        var location = DwdWarningMapper.resolveLocation(warning);

        assertThat(location.name()).isEqualTo("Stadt Dresden");
        assertThat(location.region()).isEqualTo("Sachsen");
        assertThat(location.latitude()).isEqualTo(51.0504);
        assertThat(location.longitude()).isEqualTo(13.7373);
    }

    @Test
    void fallsBackToGermanyCenterForUnknownState() {
        DwdWarning warning = new DwdWarning(1L, 2, 1_737_100_800_000L, null,
                "Somewhere", "Neverland", null, "STURM", null, "desc", null);

        var location = DwdWarningMapper.resolveLocation(warning);

        assertThat(location.latitude()).isEqualTo(51.1657);
        assertThat(location.longitude()).isEqualTo(10.4515);
    }

    @Test
    void descriptionIncludesValidityWindowAndInstruction() {
        DwdWarning warning = new DwdWarning(1L, 3, 1_737_100_800_000L, 1_737_122_400_000L,
                "Stadt Dresden", "Sachsen", "SN", "STURM", "Sturmwarnung",
                "Schwere Sturmböen erwartet.", "Fenster und Türen schließen.");

        String description = DwdWarningMapper.composeDescription(warning);

        assertThat(description).contains("Schwere Sturmböen erwartet.");
        assertThat(description).contains("Valid:");
        assertThat(description).contains("Fenster und Türen schließen.");
    }

    @Test
    void descriptionNotesOngoingWarningsWithoutEndTime() {
        DwdWarning warning = new DwdWarning(1L, 2, 1_737_060_000_000L, null,
                "Stadt Köln", "Nordrhein-Westfalen", "NW", "HOCHWASSER", null,
                "Der Rhein erreicht die Meldestufe 2.", null);

        assertThat(DwdWarningMapper.composeDescription(warning)).contains("until further notice");
    }

    @Test
    void deduplicatesWarningsRepeatedAcrossWarncellsAndKeepsFirstAreaSeen() {
        DwdWarning dresden = new DwdWarning(22334455L, 3, 1_737_100_800_000L, 1_737_122_400_000L,
                "Stadt Dresden", "Sachsen", "SN", "STARKES GEWITTER", "Gewitterwarnung", "desc", null);
        DwdWarning bautzen = new DwdWarning(22334455L, 3, 1_737_100_800_000L, 1_737_122_400_000L,
                "Landkreis Bautzen", "Sachsen", "SN", "STARKES GEWITTER", "Gewitterwarnung", "desc", null);
        DwdWarning koeln = new DwdWarning(99887766L, 2, 1_737_060_000_000L, null,
                "Stadt Köln", "Nordrhein-Westfalen", "NW", "HOCHWASSER", null, "desc2", null);

        DwdWarningsResponse response = new DwdWarningsResponse(
                Map.of(
                        "807319000", List.of(dresden),
                        "807319001", List.of(bautzen),
                        "803159022", List.of(koeln)
                ),
                PUBLISHED_AT.toEpochMilli());

        List<CrisisEvent> events = DwdWarningMapper.toEvents("dwd-weather-warnings", response);

        assertThat(events).hasSize(2);
        assertThat(events).extracting(CrisisEvent::id).containsExactlyInAnyOrder("dwd-22334455", "dwd-99887766");
        CrisisEvent gewitter = events.stream().filter(e -> e.id().equals("dwd-22334455")).findFirst().orElseThrow();
        assertThat(gewitter.location().name()).isEqualTo("Stadt Dresden");
        assertThat(gewitter.reportedAt()).isEqualTo(PUBLISHED_AT);
        assertThat(gewitter.sourceId()).isEqualTo("dwd-weather-warnings");
    }

    @Test
    void skipsWarningsMissingIdOrStart() {
        DwdWarning missingId = new DwdWarning(null, 2, 1_737_100_800_000L, null,
                "Stadt Dresden", "Sachsen", "SN", "STURM", null, "desc", null);
        DwdWarning missingStart = new DwdWarning(1L, 2, null, null,
                "Stadt Dresden", "Sachsen", "SN", "STURM", null, "desc", null);

        DwdWarningsResponse response = new DwdWarningsResponse(
                Map.of("807319000", List.of(missingId, missingStart)), PUBLISHED_AT.toEpochMilli());

        assertThat(DwdWarningMapper.toEvents("dwd-weather-warnings", response)).isEmpty();
    }

    private static DwdWarning warning(String event, String headline) {
        return new DwdWarning(1L, 2, 1_737_100_800_000L, null,
                "Stadt Dresden", "Sachsen", "SN", event, headline, "desc", null);
    }
}

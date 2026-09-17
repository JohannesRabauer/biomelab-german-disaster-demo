package com.crisisscope.provider.bbk;

import com.crisisscope.model.CrisisEvent;
import com.crisisscope.model.EventType;
import com.crisisscope.model.Severity;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class BbkWarningMapperTest {

    @Test
    void mapsFloodAlertFromFixture() {
        BbkAlert alert = readFixture("mowas-alert-flood.json");

        Optional<CrisisEvent> event = BbkWarningMapper.toEvent("bbk-warnings", alert);

        assertThat(event).isPresent();
        CrisisEvent crisisEvent = event.get();
        assertThat(crisisEvent.id()).isEqualTo("bbk-mow.DE-SN-DD-W100-20260916-000");
        assertThat(crisisEvent.sourceId()).isEqualTo("bbk-warnings");
        assertThat(crisisEvent.title()).isEqualTo("Hochwasserwarnung für die Elbe bei Dresden");
        assertThat(crisisEvent.description())
                .contains("Der Pegel der Elbe hat die Meldestufe 2 überschritten.")
                .contains("Anwohner werden gebeten, wachsam zu sein.")
                .contains("Meiden Sie den Aufenthalt in der Nähe des Ufers.")
                .doesNotContain("<br/>");
        assertThat(crisisEvent.type()).isEqualTo(EventType.FLOOD);
        assertThat(crisisEvent.severity()).isEqualTo(Severity.HIGH);
        assertThat(crisisEvent.location().name()).isEqualTo("Dresden, Elbtal");
        assertThat(crisisEvent.location().region()).isEqualTo("Sachsen");
        assertThat(crisisEvent.location().latitude()).isEqualTo(51.0504);
        assertThat(crisisEvent.location().longitude()).isEqualTo(13.7373);
        assertThat(crisisEvent.occurredAt()).isEqualTo(OffsetDateTime.parse("2026-09-16T08:00:00+02:00").toInstant());
        assertThat(crisisEvent.reportedAt()).isEqualTo(OffsetDateTime.parse("2026-09-16T08:15:00+02:00").toInstant());
        assertThat(crisisEvent.url()).isEqualTo("https://www.pegelonline.wsv.de/");
    }

    @Test
    void mapsHealthAlertFromUnmappableSenderToGermanyFallback() {
        BbkAlert alert = readFixture("katwarn-alert-health.json");

        Optional<CrisisEvent> event = BbkWarningMapper.toEvent("bbk-warnings", alert);

        assertThat(event).isPresent();
        CrisisEvent crisisEvent = event.get();
        assertThat(crisisEvent.type()).isEqualTo(EventType.PUBLIC_HEALTH);
        assertThat(crisisEvent.severity()).isEqualTo(Severity.MODERATE);
        assertThat(crisisEvent.location().name()).isEqualTo("Teile von Lauterbach");
        assertThat(crisisEvent.location().region()).isEqualTo("Germany");
        assertThat(crisisEvent.location().latitude()).isEqualTo(51.1657);
        assertThat(crisisEvent.location().longitude()).isEqualTo(10.4515);
        assertThat(crisisEvent.url()).isNull();
    }

    @Test
    void skipsCancelMessages() {
        BbkAlert alert = readFixture("mowas-alert-cancel.json");

        assertThat(BbkWarningMapper.toEvent("bbk-warnings", alert)).isEmpty();
    }

    @Test
    void skipsAlertWithNoInfoBlocks() {
        BbkAlert alert = new BbkAlert("mow.x", "DE-SN-DD-W1", "2026-09-16T08:15:00+02:00", "Alert", List.of());

        assertThat(BbkWarningMapper.toEvent("bbk-warnings", alert)).isEmpty();
    }

    @Test
    void skipsAlertWithNoParsableTimestamp() {
        BbkAlertInfo info = new BbkAlertInfo("de", List.of("Met"), "Sturm", "Severe",
                null, null, null, "Sturmwarnung", "desc", null, null, null);
        BbkAlert alert = new BbkAlert("mow.x", "DE-SN-DD-W1", "not-a-timestamp", "Alert", List.of(info));

        assertThat(BbkWarningMapper.toEvent("bbk-warnings", alert)).isEmpty();
    }

    @Test
    void prefersGermanInfoBlockOverOtherLanguages() {
        BbkAlertInfo english = new BbkAlertInfo("en", List.of("Met"), "Flood", "Severe",
                null, null, null, "Flood warning", "desc", null, null, null);
        BbkAlertInfo german = new BbkAlertInfo("de", List.of("Met"), "Hochwasser", "Severe",
                null, null, null, "Hochwasserwarnung", "desc", null, null, null);

        assertThat(BbkWarningMapper.pickPrimaryInfo(List.of(english, german))).isEqualTo(german);
    }

    @Test
    void fallsBackToFirstInfoBlockWhenNoGermanPresent() {
        BbkAlertInfo english = new BbkAlertInfo("en", List.of("Met"), "Flood", "Severe",
                null, null, null, "Flood warning", "desc", null, null, null);

        assertThat(BbkWarningMapper.pickPrimaryInfo(List.of(english))).isEqualTo(english);
    }

    @Test
    void classifiesMetCategoryAsStormWithoutFloodKeywords() {
        BbkAlertInfo info = new BbkAlertInfo("de", List.of("Met"), "Sturm", "Severe",
                null, null, null, "Sturmwarnung für die Küste", "desc", null, null, null);

        assertThat(BbkWarningMapper.classifyType(info)).isEqualTo(EventType.STORM);
    }

    @Test
    void classifiesUnknownCategoryAsOther() {
        BbkAlertInfo info = new BbkAlertInfo("de", List.of("Unmapped"), "Sonstiges", "Minor",
                null, null, null, "Sonstige Meldung", "desc", null, null, null);

        assertThat(BbkWarningMapper.classifyType(info)).isEqualTo(EventType.OTHER);
    }

    @Test
    void classifiesMissingCategoryAsOther() {
        BbkAlertInfo info = new BbkAlertInfo("de", List.of(), "Sonstiges", "Minor",
                null, null, null, "Sonstige Meldung", "desc", null, null, null);

        assertThat(BbkWarningMapper.classifyType(info)).isEqualTo(EventType.OTHER);
    }

    @Test
    void mapsAllKnownSeverityLevels() {
        assertThat(BbkWarningMapper.classifySeverity("Extreme")).isEqualTo(Severity.CRITICAL);
        assertThat(BbkWarningMapper.classifySeverity("Severe")).isEqualTo(Severity.HIGH);
        assertThat(BbkWarningMapper.classifySeverity("Moderate")).isEqualTo(Severity.MODERATE);
        assertThat(BbkWarningMapper.classifySeverity("Minor")).isEqualTo(Severity.LOW);
        assertThat(BbkWarningMapper.classifySeverity("Unknown")).isEqualTo(Severity.MODERATE);
        assertThat(BbkWarningMapper.classifySeverity(null)).isEqualTo(Severity.MODERATE);
    }

    @Test
    void stripsHtmlBreaksAndTags() {
        String plain = BbkWarningMapper.htmlToPlainText("Line one<br/>Line two<br>Line three <a href=\"x\">link</a>");

        assertThat(plain).isEqualTo("Line one\nLine two\nLine three link");
    }

    @Test
    void occurredAtFallsBackToSentWhenEffectiveAndOnsetMissing() {
        Instant sent = OffsetDateTime.parse("2026-09-16T08:15:00+02:00").toInstant();
        BbkAlertInfo info = new BbkAlertInfo("de", List.of("Met"), "Sturm", "Severe",
                null, null, null, "Sturmwarnung", "desc", null, null, null);
        BbkAlert alert = new BbkAlert("mow.x", "DE-SN-DD-W1", "2026-09-16T08:15:00+02:00", "Alert", List.of(info));

        Optional<CrisisEvent> event = BbkWarningMapper.toEvent("bbk-warnings", alert);

        assertThat(event).isPresent();
        assertThat(event.get().occurredAt()).isEqualTo(sent);
        assertThat(event.get().reportedAt()).isEqualTo(sent);
    }

    private static BbkAlert readFixture(String fileName) {
        try (InputStream in = BbkWarningMapperTest.class.getResourceAsStream("/fixtures/bbk/" + fileName)) {
            return new ObjectMapper().readValue(in, BbkAlert.class);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}

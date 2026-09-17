package com.crisisscope.service;

import com.crisisscope.model.CrisisEvent;
import com.crisisscope.model.Severity;
import com.crisisscope.provider.CrisisDataProvider;
import com.crisisscope.service.dto.EventFilter;
import com.crisisscope.service.dto.KpiSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;

/**
 * Aggregates events across every registered {@link CrisisDataProvider} and
 * exposes the read models the web layer renders (feed, filters, KPIs, map data).
 */
@Service
public class CrisisEventService {

    private static final Logger log = LoggerFactory.getLogger(CrisisEventService.class);

    private final CrisisDataRegistry registry;

    public CrisisEventService(CrisisDataRegistry registry) {
        this.registry = registry;
    }

    /** All events from all providers, newest first. A failing provider is skipped, not fatal. */
    public List<CrisisEvent> allEvents() {
        return registry.providers().stream()
                .flatMap(provider -> fetchSafely(provider).stream())
                .sorted(Comparator.comparing(CrisisEvent::occurredAt).reversed())
                .toList();
    }

    public List<CrisisEvent> filter(EventFilter filter) {
        return allEvents().stream().filter(filter::matches).toList();
    }

    public List<CrisisEvent> eventsForRegion(String region) {
        return allEvents().stream()
                .filter(event -> event.location().region().equalsIgnoreCase(region))
                .toList();
    }

    /** Distinct region names present in current events, alphabetically sorted. */
    public List<String> regions() {
        return allEvents().stream()
                .map(event -> event.location().region())
                .collect(() -> new TreeSet<>(String::compareTo), TreeSet::add, TreeSet::addAll)
                .stream()
                .toList();
    }

    public KpiSummary kpiSummary() {
        List<CrisisEvent> events = allEvents();
        long critical = events.stream().filter(e -> e.severity() == Severity.CRITICAL).count();
        long regionCount = events.stream().map(e -> e.location().region()).distinct().count();
        return new KpiSummary(events.size(), critical, regionCount, registry.providers().size());
    }

    private List<CrisisEvent> fetchSafely(CrisisDataProvider provider) {
        try {
            return provider.fetchEvents();
        } catch (RuntimeException ex) {
            log.warn("Provider '{}' failed to supply events, skipping it for this request", provider.id(), ex);
            return List.of();
        }
    }
}

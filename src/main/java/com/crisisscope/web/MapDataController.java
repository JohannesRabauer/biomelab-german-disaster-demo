package com.crisisscope.web;

import com.crisisscope.model.CrisisEvent;
import com.crisisscope.service.CrisisEventService;
import com.crisisscope.service.dto.EventFilter;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Serves the current (optionally filtered) events as GeoJSON so the MapLibre
 * layer on the dashboard can (re)draw markers without a full page reload.
 */
@RestController
public class MapDataController {

    private final CrisisEventService eventService;

    public MapDataController(CrisisEventService eventService) {
        this.eventService = eventService;
    }

    @GetMapping("/api/events")
    public Map<String, Object> events(@RequestParam(required = false) String type,
                                       @RequestParam(required = false) String severity,
                                       @RequestParam(required = false) String region) {
        List<CrisisEvent> events = eventService.filter(EventFilter.of(type, severity, region));

        List<Map<String, Object>> features = events.stream().map(this::toFeature).toList();

        Map<String, Object> featureCollection = new LinkedHashMap<>();
        featureCollection.put("type", "FeatureCollection");
        featureCollection.put("features", features);
        return featureCollection;
    }

    private Map<String, Object> toFeature(CrisisEvent event) {
        Map<String, Object> geometry = new LinkedHashMap<>();
        geometry.put("type", "Point");
        geometry.put("coordinates", List.of(event.location().longitude(), event.location().latitude()));

        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("id", event.id());
        properties.put("title", event.title());
        properties.put("description", event.description());
        properties.put("type", event.type().name());
        properties.put("typeLabel", event.type().label());
        properties.put("icon", event.type().icon());
        properties.put("severity", event.severity().name());
        properties.put("severityLabel", event.severity().label());
        properties.put("severityRank", event.severity().rank());
        properties.put("region", event.location().region());
        properties.put("locationName", event.location().name());
        properties.put("occurredAt", event.occurredAt().toString());
        properties.put("url", event.url());

        Map<String, Object> feature = new LinkedHashMap<>();
        feature.put("type", "Feature");
        feature.put("geometry", geometry);
        feature.put("properties", properties);
        return feature;
    }
}

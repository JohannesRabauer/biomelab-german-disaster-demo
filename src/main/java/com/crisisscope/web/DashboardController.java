package com.crisisscope.web;

import com.crisisscope.config.MapProperties;
import com.crisisscope.model.EventType;
import com.crisisscope.model.Severity;
import com.crisisscope.service.CrisisEventService;
import com.crisisscope.service.dto.EventFilter;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Renders the full operations dashboard shell. Filtering/refresh interactions
 * after the initial load are handled by {@link EventFeedController},
 * {@link RegionDetailController} and {@link MapDataController} via HTMX/fetch.
 */
@Controller
public class DashboardController {

    private final CrisisEventService eventService;
    private final MapProperties mapProperties;

    public DashboardController(CrisisEventService eventService, MapProperties mapProperties) {
        this.eventService = eventService;
        this.mapProperties = mapProperties;
    }

    @GetMapping("/")
    public String dashboard(Model model) {
        model.addAttribute("events", eventService.filter(EventFilter.of(null, null, null)));
        model.addAttribute("kpis", eventService.kpiSummary());
        model.addAttribute("regions", eventService.regions());
        model.addAttribute("eventTypes", EventType.values());
        model.addAttribute("severities", Severity.values());
        model.addAttribute("mapProperties", mapProperties);
        return "dashboard";
    }
}

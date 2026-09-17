package com.crisisscope.web;

import com.crisisscope.model.CrisisEvent;
import com.crisisscope.service.CrisisEventService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Comparator;
import java.util.List;

/**
 * HTMX partial endpoint for the regional details panel, populated when a map
 * marker or a feed item's region is clicked.
 */
@Controller
public class RegionDetailController {

    private final CrisisEventService eventService;

    public RegionDetailController(CrisisEventService eventService) {
        this.eventService = eventService;
    }

    @GetMapping("/fragments/region-detail")
    public String regionDetail(@RequestParam(required = false) String region, Model model) {
        if (region == null || region.isBlank()) {
            model.addAttribute("region", null);
            model.addAttribute("events", List.<CrisisEvent>of());
            return "fragments/region-detail :: panel";
        }

        List<CrisisEvent> events = eventService.eventsForRegion(region).stream()
                .sorted(Comparator.comparing(CrisisEvent::occurredAt).reversed())
                .toList();

        model.addAttribute("region", region);
        model.addAttribute("events", events);
        return "fragments/region-detail :: panel";
    }
}

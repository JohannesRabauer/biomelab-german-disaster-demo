package com.crisisscope.web;

import com.crisisscope.service.CrisisEventService;
import com.crisisscope.service.dto.EventFilter;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * HTMX partial endpoint backing the event feed. The filter bar submits here on
 * change and swaps the returned fragment into the feed's list container.
 */
@Controller
public class EventFeedController {

    private final CrisisEventService eventService;

    public EventFeedController(CrisisEventService eventService) {
        this.eventService = eventService;
    }

    @GetMapping("/fragments/events")
    public String events(@RequestParam(required = false) String type,
                          @RequestParam(required = false) String severity,
                          @RequestParam(required = false) String region,
                          Model model) {
        model.addAttribute("events", eventService.filter(EventFilter.of(type, severity, region)));
        return "fragments/event-feed :: feed";
    }
}

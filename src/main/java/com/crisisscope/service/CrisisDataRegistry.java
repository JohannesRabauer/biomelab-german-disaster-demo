package com.crisisscope.service;

import com.crisisscope.provider.CrisisDataProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Holds every {@link CrisisDataProvider} bean present in the application context.
 * Spring collects all beans of that type into this constructor's {@code List}
 * automatically, so a new provider only needs a {@code @Component} annotation
 * to be picked up here — no registry code changes required.
 */
@Component
public class CrisisDataRegistry {

    private static final Logger log = LoggerFactory.getLogger(CrisisDataRegistry.class);

    private final List<CrisisDataProvider> providers;

    public CrisisDataRegistry(List<CrisisDataProvider> providers) {
        this.providers = List.copyOf(providers);
        log.info("Registered {} crisis data provider(s): {}", this.providers.size(), providerIds());
    }

    public List<CrisisDataProvider> providers() {
        return providers;
    }

    public List<String> providerIds() {
        return providers.stream().map(CrisisDataProvider::id).toList();
    }
}

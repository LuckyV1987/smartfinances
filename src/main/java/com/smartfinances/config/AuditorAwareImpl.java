package com.smartfinances.config;

import org.springframework.data.domain.AuditorAware;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class AuditorAwareImpl implements AuditorAware<Long> {

    @Override
    public Optional<Long> getCurrentAuditor() {
        // TODO: Return current authenticated user ID from SecurityContext
        // For now, return a default system user ID
        return Optional.of(1L);
    }

}


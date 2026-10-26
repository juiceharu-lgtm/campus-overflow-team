// UserRegisteredEvent.java
package com.campusoverflow.identity.api.event;

import com.campusoverflow.shared.event.IntegrationEvent;
import com.campusoverflow.shared.security.Role;
import java.time.Instant;

public record UserRegisteredEvent(
        String eventId,
        Instant occurredAt,
        long userId,
        String displayName,
        Role role
) implements IntegrationEvent {}

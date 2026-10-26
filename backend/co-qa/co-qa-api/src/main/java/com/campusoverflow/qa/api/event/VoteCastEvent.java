package com.campusoverflow.qa.api.event;

import com.campusoverflow.shared.event.IntegrationEvent;
import java.time.Instant;

public record VoteCastEvent(
        String eventId,
        Instant occurredAt,
        long voteId,
        long voterId,
        long targetAuthorId,
        long targetId,
        String voteTargetType
) implements IntegrationEvent {}

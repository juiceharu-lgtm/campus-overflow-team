package com.campusoverflow.qa.api.event;

import com.campusoverflow.shared.event.IntegrationEvent;
import java.time.Instant;

public record AnswerAcceptedEvent(
        String eventId,
        Instant occurredAt,
        long answerId,
        long answerAuthorId,
        long questionOwnerId,
        long bountyId
) implements IntegrationEvent {}

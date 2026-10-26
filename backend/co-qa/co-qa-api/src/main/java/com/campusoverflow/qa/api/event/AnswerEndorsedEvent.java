package com.campusoverflow.qa.api.event;

import com.campusoverflow.shared.event.IntegrationEvent;
import java.time.Instant;

public record AnswerEndorsedEvent(
        String eventId,
        Instant occurredAt,
        long answerId,
        long answerAuthorId,
        long endorserId
) implements IntegrationEvent {}

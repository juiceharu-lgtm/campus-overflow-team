// AnswerSubmittedEvent.java
package com.campusoverflow.qa.api.event;


import com.campusoverflow.shared.event.IntegrationEvent;
import java.time.Instant;

public record AnswerSubmittedEvent(
        String eventId,
        Instant occurredAt,
        long answerId,
        long authorId,
        long questionId
) implements IntegrationEvent {}

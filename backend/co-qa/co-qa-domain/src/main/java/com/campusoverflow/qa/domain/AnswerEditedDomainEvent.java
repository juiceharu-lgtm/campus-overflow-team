package com.campusoverflow.qa.domain;

import java.time.Instant;

public record AnswerEditedDomainEvent(
        Long answerId,
        long questionId,
        long operatorId,
        Instant occurredAt
) implements DomainEvent {
}

package com.campusoverflow.qa.domain;

import java.time.Instant;

public record AnswerDeletedDomainEvent(
        Long answerId,
        long questionId,
        long operatorId,
        Instant occurredAt
) implements DomainEvent {
}

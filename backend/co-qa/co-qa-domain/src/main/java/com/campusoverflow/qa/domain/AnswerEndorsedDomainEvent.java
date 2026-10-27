package com.campusoverflow.qa.domain;

import java.time.Instant;

public record AnswerEndorsedDomainEvent(
        Long answerId,
        long questionId,
        long operatorId,
        Instant occurredAt
) implements DomainEvent {
}

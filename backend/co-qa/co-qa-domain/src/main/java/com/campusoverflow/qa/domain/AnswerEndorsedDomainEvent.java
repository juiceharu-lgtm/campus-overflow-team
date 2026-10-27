package com.campusoverflow.qa.domain;

import java.time.Instant;

public record AnswerEndorsedDomainEvent(
        Long answerId,
        long questionId,
        long operatorUserId,
        Instant occurredAt
) implements DomainEvent {}

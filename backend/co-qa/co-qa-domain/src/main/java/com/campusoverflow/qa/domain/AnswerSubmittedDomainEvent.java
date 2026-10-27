package com.campusoverflow.qa.domain;

import java.time.Instant;

public record AnswerSubmittedDomainEvent(
        Long answerId,
        long questionId,
        long authorId,
        Instant occurredAt
) implements DomainEvent {}

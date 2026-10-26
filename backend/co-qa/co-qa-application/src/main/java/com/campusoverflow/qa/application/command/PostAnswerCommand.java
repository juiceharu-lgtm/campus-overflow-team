package com.campusoverflow.qa.application.command;

public record PostAnswerCommand(
        long questionId,
        String body
) {
}
package com.campusoverflow.qa.application.command;

import com.campusoverflow.qa.domain.Question;
import com.campusoverflow.qa.domain.QuestionRepository;
import com.campusoverflow.shared.security.Actor;
import org.springframework.stereotype.Service;
import java.time.Instant;

@Service
public class SoftDeleteQuestionUseCase {

    private final QuestionRepository questionRepository;

    public SoftDeleteQuestionUseCase(QuestionRepository questionRepository) {
        this.questionRepository = questionRepository;
    }

    public void execute(Actor actor, long questionId, boolean actorManagesCourse) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new com.campusoverflow.shared.domain.BusinessRuleException("QA-1020", "问题不存在"));

        Instant now = Instant.now();
        question.softDelete(actor, actorManagesCourse, now);
        questionRepository.save(question);
    }
}
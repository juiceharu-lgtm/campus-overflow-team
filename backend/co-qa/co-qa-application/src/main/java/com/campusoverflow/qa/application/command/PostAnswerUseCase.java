package com.campusoverflow.qa.application.command;

import com.campusoverflow.qa.domain.Answer;
import com.campusoverflow.qa.domain.AnswerRepository;
import com.campusoverflow.qa.domain.MarkdownBody;
import com.campusoverflow.qa.domain.Question;
import com.campusoverflow.qa.domain.QuestionRepository;
import com.campusoverflow.shared.domain.BusinessRuleException;
import com.campusoverflow.shared.security.Actor;
import org.springframework.stereotype.Service;
import java.time.Instant;

@Service
public class PostAnswerUseCase {

    private final AnswerRepository answerRepository;
    private final QuestionRepository questionRepository;

    public PostAnswerUseCase(AnswerRepository answerRepository, QuestionRepository questionRepository) {
        this.answerRepository = answerRepository;
        this.questionRepository = questionRepository;
    }

    public Long execute(Actor actor, long questionId, MarkdownBody body) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new BusinessRuleException("QA-1020", "问题不存在"));
        Instant now = Instant.now();
        Answer answer = Answer.submit(question, actor.userId(), body, now);
        Answer saved = answerRepository.save(answer);
        return saved.id();
    }
}
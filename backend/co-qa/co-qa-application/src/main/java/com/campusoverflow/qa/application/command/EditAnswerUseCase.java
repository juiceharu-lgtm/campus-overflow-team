package com.campusoverflow.qa.application.command;

import com.campusoverflow.qa.domain.Answer;
import com.campusoverflow.qa.domain.AnswerRepository;
import com.campusoverflow.qa.domain.MarkdownBody;
import com.campusoverflow.qa.domain.Question;
import com.campusoverflow.qa.domain.QuestionRepository;
import com.campusoverflow.identity.api.IdentityApi;
import com.campusoverflow.shared.domain.NotFoundException;
import com.campusoverflow.shared.security.Actor;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class EditAnswerUseCase {

    private final AnswerRepository answerRepository;
    private final QuestionRepository questionRepository;
    private final IdentityApi identityApi;

    public EditAnswerUseCase(AnswerRepository answerRepository,
                             QuestionRepository questionRepository,
                             IdentityApi identityApi) {
        this.answerRepository = answerRepository;
        this.questionRepository = questionRepository;
        this.identityApi = identityApi;
    }

    public void execute(Actor actor, long answerId, MarkdownBody newBody) {
        Answer answer = answerRepository.findById(answerId)
                .orElseThrow(() -> new NotFoundException("QA‑4002", "回答不存在"));

        Question question = questionRepository.findById(answer.questionId())
                .orElseThrow(() -> new NotFoundException("QA‑4001", "所属问题不存在"));

        boolean manages = identityApi.canManageCourse(actor.userId(), question.courseId());
        Instant now = Instant.now();

        answer.edit(actor, manages, newBody, now);
        answerRepository.save(answer);
    }
}

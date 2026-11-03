package com.campusoverflow.qa.application.command;

import com.campusoverflow.qa.domain.Question;
import com.campusoverflow.qa.domain.QuestionRepository;
import com.campusoverflow.qa.domain.MarkdownBody;
import com.campusoverflow.qa.domain.Tags;
import com.campusoverflow.qa.domain.Title;
import com.campusoverflow.shared.security.Actor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class PostQuestionUseCase {

    private final QuestionRepository questionRepository;

    public PostQuestionUseCase(QuestionRepository questionRepository) {
        this.questionRepository = questionRepository;
    }

    @Transactional
    public Long execute(Actor actor, Long courseId, Title title, MarkdownBody body, Tags tags) {
        Instant now = Instant.now();
        // 调用聚合工厂方法新建问题
        Question question = Question.post(actor.userId(), courseId, title, body, tags, now);
        Question saved = questionRepository.save(question);
        return saved.id();
    }
}

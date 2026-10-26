package com.campusoverflow.qa.application.command;

import com.campusoverflow.qa.api.event.QuestionPostedEvent;
import com.campusoverflow.qa.domain.MarkdownBody;
import com.campusoverflow.qa.domain.Question;
import com.campusoverflow.qa.domain.QuestionRepository;
import com.campusoverflow.qa.domain.TagRepository;
import com.campusoverflow.qa.domain.Tags;
import com.campusoverflow.qa.domain.Title;
import com.campusoverflow.shared.event.IntegrationEventPublisher;
import com.campusoverflow.shared.security.Actor;
import com.campusoverflow.shared.security.Role;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuestionServiceTest {
    @Mock
    private QuestionRepository questionRepository;
    @Mock
    private TagRepository tagRepository;
    @Mock
    private IntegrationEventPublisher publisher;
    @Mock
    private Clock clock;

    @InjectMocks
    private QuestionService questionService;

    private final Instant now = Instant.parse("2026-10-26T10:00:00Z");
    private final Actor actor = new Actor(1001L, "测试用户", Role.STUDENT);

    @Test
    void post_success() {
        List<String> inputTags = List.of("DDD", "Java");
        PostQuestionCommand cmd = new PostQuestionCommand(5001L, "Java DDD怎么学", "请教DDD实践", inputTags);
        when(clock.instant()).thenReturn(now);

        Title title = new Title(cmd.title());
        MarkdownBody body = MarkdownBody.ofQuestion(cmd.body());
        Tags tags = Tags.of(inputTags);
        Question question = Question.post(actor.userId(), cmd.courseId(), title, body, tags, now);

        when(questionRepository.save(any(Question.class))).thenAnswer(a -> {
            Question q = a.getArgument(0);
            q.assignId(2001L);
            return q;
        });

        long qId = questionService.post(actor, cmd);
        assertEquals(2001L, qId);

        verify(tagRepository).adjustUsage(inputTags, 1);
        verify(publisher).publish(any(QuestionPostedEvent.class));
    }
}
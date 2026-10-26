package com.campusoverflow.qa.application.command;

import com.campusoverflow.qa.api.event.AnswerSubmittedEvent;
import com.campusoverflow.qa.domain.Answer;
import com.campusoverflow.qa.domain.AnswerRepository;
import com.campusoverflow.qa.domain.MarkdownBody;
import com.campusoverflow.qa.domain.Question;
import com.campusoverflow.qa.domain.QuestionRepository;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnswerServiceTest {
    @Mock
    private QuestionRepository questionRepository;
    @Mock
    private AnswerRepository answerRepository;
    @Mock
    private IntegrationEventPublisher publisher;
    @Mock
    private Clock clock;
    @Mock
    private Question question;
    @Mock
    private Title title;

    @InjectMocks
    private AnswerService answerService;

    private final Instant now = Instant.parse("2026-10-26T10:00:00Z");
    private final Actor actor = new Actor(1001L, "测试用户", Role.STUDENT);

    @Test
    void post_success() {
        PostAnswerCommand cmd = new PostAnswerCommand(1001L, "这是回答内容");

        when(clock.instant()).thenReturn(now);
        when(questionRepository.findById(1001L)).thenReturn(Optional.of(question));
        when(question.authorId()).thenReturn(2001L);
        when(question.title()).thenReturn(title);
        when(title.value()).thenReturn("测试问题标题");

        when(answerRepository.save(any(Answer.class))).thenAnswer(a -> {
            Answer ans = a.getArgument(0);
            ans.assignId(3001L);
            return ans;
        });

        long answerId = answerService.post(actor, cmd);
        assertEquals(3001L, answerId);

        verify(answerRepository).save(any(Answer.class));
        verify(publisher).publish(any(AnswerSubmittedEvent.class));
    }
}
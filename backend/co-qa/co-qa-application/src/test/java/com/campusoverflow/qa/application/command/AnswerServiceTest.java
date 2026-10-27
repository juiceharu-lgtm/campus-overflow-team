package com.campusoverflow.qa.application.command;

import com.campusoverflow.qa.domain.Answer;
import com.campusoverflow.qa.domain.AnswerRepository;
import com.campusoverflow.qa.domain.MarkdownBody;
import com.campusoverflow.qa.domain.Question;
import com.campusoverflow.qa.domain.QuestionRepository;
import com.campusoverflow.qa.domain.Title;
import com.campusoverflow.qa.domain.Tags;
import com.campusoverflow.shared.domain.ConflictException;
import com.campusoverflow.shared.domain.ForbiddenException;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
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

    @InjectMocks
    private AnswerService service;

    private final Instant now = Instant.parse("2026-10-26T00:00:00Z");
    // 提问者：学生
    private final Actor questionAuthor = new Actor(1001L, "提问学生", Role.STUDENT);
    // 教师角色
    private final Actor teacherActor = new Actor(999L, "授课老师", Role.TEACHER);
    private final long questionId = 10L;
    private final long answerId = 20L;

    @Test
    void post_shouldCreateAndPublishEvent() {
        when(clock.instant()).thenReturn(now);
        Title title = new Title("这是一条长度符合要求的测试问题标题");
        Tags tags = Tags.of(List.of("java"));
        Question question = Question.post(questionAuthor.userId(), 1L,
                title,
                MarkdownBody.ofQuestion("问题内容"),
                tags, now);
        question.assignId(questionId);
        when(questionRepository.findById(questionId)).thenReturn(Optional.of(question));

        Answer answer = Answer.submit(question, 2001L, MarkdownBody.ofAnswer("回答内容"), now);
        answer.assignId(answerId);
        when(answerRepository.save(any(Answer.class))).thenReturn(answer);

        PostAnswerCommand cmd = new PostAnswerCommand(questionId, "回答内容");
        long resultId = service.post(new Actor(2001L, "回答者", Role.STUDENT), cmd);

        assertEquals(answerId, resultId);
        verify(publisher).publish(any());
    }

    @Test
    void accept_AuthorAcceptAnswer_success() {
        when(clock.instant()).thenReturn(now);
        Title title = new Title("这是一条长度符合要求的测试问题标题");
        Tags tags = Tags.of(List.of("java"));
        Question question = Question.post(questionAuthor.userId(), 1L,
                title,
                MarkdownBody.ofQuestion("问题内容"),
                tags, now);
        question.assignId(questionId);

        Answer answer = Answer.submit(question, 2001L, MarkdownBody.ofAnswer("回答"), now);
        answer.assignId(answerId);

        when(answerRepository.findById(answerId)).thenReturn(Optional.of(answer));
        when(questionRepository.findById(questionId)).thenReturn(Optional.of(question));

        service.accept(questionAuthor, answerId, false);

        verify(questionRepository).save(question);
        verify(answerRepository).save(answer);
        verify(publisher).publish(any());
    }

    @Test
    void accept_AlreadyAccepted_ThrowConflict() {
        when(clock.instant()).thenReturn(now);
        Title title = new Title("这是一条长度符合要求的测试问题标题");
        Tags tags = Tags.of(List.of("java"));
        Question question = Question.post(questionAuthor.userId(), 1L,
                title,
                MarkdownBody.ofQuestion("问题内容"),
                tags, now);
        question.assignId(questionId);

        Answer answer1 = Answer.submit(question, 2001L, MarkdownBody.ofAnswer("回答1"), now);
        answer1.assignId(answerId);
        Answer answer2 = Answer.submit(question, 2002L, MarkdownBody.ofAnswer("回答2"), now);
        answer2.assignId(21L);

        when(answerRepository.findById(21L)).thenReturn(Optional.of(answer2));
        when(questionRepository.findById(questionId)).thenReturn(Optional.of(question));

        // 先采纳第一个答案
        question.accept(answer1, questionAuthor, false);

        ConflictException ex = assertThrows(ConflictException.class, () ->
                service.accept(questionAuthor, 21L, false));
        assertTrue(ex.getMessage().contains("已经存在被采纳的答案"));
    }

    @Test
    void endorse_TeacherEndorse_success() {
        when(clock.instant()).thenReturn(now);
        Title title = new Title("这是一条长度符合要求的测试问题标题");
        Tags tags = Tags.of(List.of("java"));
        Question question = Question.post(questionAuthor.userId(), 1L,
                title,
                MarkdownBody.ofQuestion("问题内容"),
                tags, now);
        question.assignId(questionId);

        Answer answer = Answer.submit(question, 2001L, MarkdownBody.ofAnswer("回答"), now);
        answer.assignId(answerId);

        when(answerRepository.findById(answerId)).thenReturn(Optional.of(answer));
        when(questionRepository.findById(questionId)).thenReturn(Optional.of(question));

        service.endorse(teacherActor, answerId, true);

        verify(answerRepository).save(answer);
        verify(publisher).publish(any());
    }

    @Test
    void endorse_NotTeacher_ThrowForbidden() {
        when(clock.instant()).thenReturn(now);
        Title title = new Title("这是一条长度符合要求的测试问题标题");
        Tags tags = Tags.of(List.of("java"));
        Question question = Question.post(questionAuthor.userId(), 1L,
                title,
                MarkdownBody.ofQuestion("问题内容"),
                tags, now);
        question.assignId(questionId);

        Answer answer = Answer.submit(question, 2001L, MarkdownBody.ofAnswer("回答"), now);
        answer.assignId(answerId);

        when(answerRepository.findById(answerId)).thenReturn(Optional.of(answer));
        when(questionRepository.findById(questionId)).thenReturn(Optional.of(question));

        Actor normalStudent = new Actor(555L, "普通学生", Role.STUDENT);
        ForbiddenException ex = assertThrows(ForbiddenException.class, () ->
                service.endorse(normalStudent, answerId, false));
        assertTrue(ex.getMessage().contains("只有课程管理者可以认证回答"));
    }
}
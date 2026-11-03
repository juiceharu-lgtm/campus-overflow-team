package com.campusoverflow.qa.application.command;

import com.campusoverflow.qa.domain.MarkdownBody;
import com.campusoverflow.qa.domain.Question;
import com.campusoverflow.qa.domain.QuestionRepository;
import com.campusoverflow.qa.domain.Tags;
import com.campusoverflow.qa.domain.Title;
import com.campusoverflow.shared.domain.ForbiddenException;
import com.campusoverflow.shared.domain.NotFoundException;
import com.campusoverflow.shared.security.Actor;
import com.campusoverflow.shared.security.Role;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EditQuestionUseCaseTest {

    @Mock
    private QuestionRepository questionRepository;

    @InjectMocks
    private EditQuestionUseCase useCase;

    private final Actor author = new Actor(1001L, "提问学生", Role.STUDENT);
    private final Actor teacher = new Actor(2001L, "授课老师", Role.TEACHER);
    private final Actor stranger = new Actor(9999L, "无关用户", Role.STUDENT);
    private final Instant fixedNow = Instant.parse("2026-11-01T12:00:00Z");

    @Test
    void execute_success_authorEdit() {
        when(questionRepository.save(any(Question.class))).thenAnswer(i -> i.getArgument(0));
        Title oldTitle = new Title("在校园平台如何高效提出自己的技术疑问");
        MarkdownBody oldBody = new MarkdownBody("这里是一段符合长度要求的问题正文，用来描述我在学习DDD领域驱动设计时遇到的疑惑，希望同学和老师帮忙解答。");
        Tags tags = Tags.of(List.of("Java"));
        Question q = Question.post(author.userId(), 10L, oldTitle, oldBody, tags, fixedNow);
        q.assignId(1L);

        when(questionRepository.findById(1L)).thenReturn(Optional.of(q));

        Title newTitle = new Title("修改之后：DDD聚合根设计的学习疑问");
        MarkdownBody newBody = new MarkdownBody("修改后的正文：我想了解聚合根和值对象的边界划分，在问答系统里该怎么落地。");
        Tags newTags = Tags.of(List.of("Java", "DDD"));

        useCase.execute(author, 1L, false, newTitle, newBody, newTags);
        verify(questionRepository).save(q);
    }

    @Test
    void execute_success_teacherManageEdit() {
        when(questionRepository.save(any(Question.class))).thenAnswer(i -> i.getArgument(0));
        Title oldTitle = new Title("在校园平台如何高效提出自己的技术疑问");
        MarkdownBody oldBody = new MarkdownBody("这里是一段符合长度要求的问题正文，用来描述我在学习DDD领域驱动设计时遇到的疑惑，希望同学和老师帮忙解答。");
        Tags tags = Tags.of(List.of("Java"));
        Question q = Question.post(author.userId(), 10L, oldTitle, oldBody, tags, fixedNow);
        q.assignId(1L);

        when(questionRepository.findById(1L)).thenReturn(Optional.of(q));

        Title newTitle = new Title("教师修订：DDD聚合根设计的学习疑问");
        MarkdownBody newBody = new MarkdownBody("教师修改正文：梳理领域不变量，明确问答业务规则边界。");
        Tags newTags = Tags.of(List.of("Java", "Spring"));

        useCase.execute(teacher, 1L, true, newTitle, newBody, newTags);
        verify(questionRepository).save(q);
    }

    @Test
    void execute_fail_strangerNoPermission() {
        Title oldTitle = new Title("在校园平台如何高效提出自己的技术疑问");
        MarkdownBody oldBody = new MarkdownBody("这里是一段符合长度要求的问题正文，用来描述我在学习DDD领域驱动设计时遇到的疑惑，希望同学和老师帮忙解答。");
        Tags tags = Tags.of(List.of("Java"));
        Question q = Question.post(author.userId(), 10L, oldTitle, oldBody, tags, fixedNow);
        q.assignId(1L);

        when(questionRepository.findById(1L)).thenReturn(Optional.of(q));

        Title newTitle = new Title("无关用户尝试修改这个问题标题");
        MarkdownBody newBody = new MarkdownBody("无关用户随便填写的正文内容，没有业务意义。");
        Tags newTags = Tags.of(List.of("Test"));

        ForbiddenException ex = assertThrows(ForbiddenException.class,
                () -> useCase.execute(stranger, 1L, false, newTitle, newBody, newTags));
        assertEquals("QA‑3002", ex.code());
    }

    @Test
    void execute_fail_notFound() {
        Title newTitle = new Title("一个合法长度的测试问题标题");
        MarkdownBody newBody = new MarkdownBody("一段满足校验长度的测试问题正文内容。");
        Tags newTags = Tags.of(List.of("Java"));
        when(questionRepository.findById(999L)).thenReturn(Optional.empty());

        NotFoundException ex = assertThrows(
                NotFoundException.class,
                () -> useCase.execute(author, 999L, false, newTitle, newBody, newTags));
        assertEquals("QA‑4001", ex.code());
    }
}

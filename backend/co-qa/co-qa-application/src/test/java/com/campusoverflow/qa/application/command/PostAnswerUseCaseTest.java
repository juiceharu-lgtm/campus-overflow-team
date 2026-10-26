package com.campusoverflow.qa.application.command;

import com.campusoverflow.qa.domain.Answer;
import com.campusoverflow.qa.domain.AnswerRepository;
import com.campusoverflow.qa.domain.MarkdownBody;
import com.campusoverflow.qa.domain.Question;
import com.campusoverflow.qa.domain.QuestionRepository;
import com.campusoverflow.qa.domain.Tags;
import com.campusoverflow.qa.domain.Title;
import com.campusoverflow.shared.domain.BusinessRuleException;
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
class PostAnswerUseCaseTest {

    @Mock
    private AnswerRepository answerRepository;

    @Mock
    private QuestionRepository questionRepository;

    @InjectMocks
    private PostAnswerUseCase useCase;

    private final Actor student = new Actor(1001L, "学生同学", Role.STUDENT);

    @Test
    void success_postAnswer() {
        Instant now = Instant.now();
        Title title = new Title("测试问题");
        MarkdownBody qBody = new MarkdownBody("问题正文");
        Tags tags = Tags.of(List.of("Java"));
        Question question = Question.post(2001L, 10L, title, qBody, tags, now);
        question.assignId(1L);

        MarkdownBody answerBody = new MarkdownBody("这是回答内容");
        Answer answer = Answer.submit(question, student.userId(), answerBody, now);
        answer.assignId(100L);

        when(questionRepository.findById(1L)).thenReturn(Optional.of(question));
        when(answerRepository.save(any(Answer.class))).thenReturn(answer);

        Long answerId = useCase.execute(student, 1L, answerBody);
        assertEquals(100L, answerId);
        verify(answerRepository).save(any(Answer.class));
    }

    @Test
    void fail_questionNotFound() {
        MarkdownBody body = new MarkdownBody("回答内容");
        when(questionRepository.findById(999L)).thenReturn(Optional.empty());

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> useCase.execute(student, 999L, body));
        assertEquals("QA-1020", ex.getMessage());
    }
}
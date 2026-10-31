package com.campusoverflow.qa.application.command;

import com.campusoverflow.qa.domain.Answer;
import com.campusoverflow.qa.domain.AnswerRepository;
import com.campusoverflow.qa.domain.Question;
import com.campusoverflow.qa.domain.QuestionRepository;
import com.campusoverflow.identity.api.IdentityApi;
import com.campusoverflow.shared.domain.NotFoundException;
import com.campusoverflow.shared.security.Actor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EndorseAnswerUseCaseTest {
    @Mock
    private AnswerRepository answerRepository;
    @Mock
    private QuestionRepository questionRepository;
    @Mock
    private IdentityApi identityApi;

    @InjectMocks
    private EndorseAnswerUseCase useCase;

    private final Actor teacherActor = new Actor(2L, "teacher", null);

    @Test
    void execute_success_when_teacher_endorse() {
        Answer answer = mock(Answer.class);
        Question question = mock(Question.class);
        when(answerRepository.findById(10L)).thenReturn(Optional.of(answer));
        when(answer.questionId()).thenReturn(20L);
        when(questionRepository.findById(20L)).thenReturn(Optional.of(question));
        when(identityApi.canManageCourse(2L, any())).thenReturn(true);

        useCase.execute(teacherActor, 10L);

        // 这里改：只传两个参数，去掉 Instant
        verify(answer).endorse(eq(teacherActor), eq(true));
        verify(answerRepository).save(answer);
    }

    @Test
    void execute_fail_answer_missing() {
        when(answerRepository.findById(99L)).thenReturn(Optional.empty());
        NotFoundException ex = assertThrows(NotFoundException.class,
                () -> useCase.execute(teacherActor,99L));
        assertEquals("QA‑4002", ex.code());
    }
}

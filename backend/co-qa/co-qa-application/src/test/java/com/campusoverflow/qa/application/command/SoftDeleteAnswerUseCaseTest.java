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

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SoftDeleteAnswerUseCaseTest {
    @Mock
    private AnswerRepository answerRepository;
    @Mock
    private QuestionRepository questionRepository;
    @Mock
    private IdentityApi identityApi;

    @InjectMocks
    private SoftDeleteAnswerUseCase useCase;

    private final Actor actor = new Actor(1L, "user", null);

    @Test
    void execute_success_owner_delete() {
        Answer answer = mock(Answer.class);
        Question question = mock(Question.class);
        when(answerRepository.findById(10L)).thenReturn(Optional.of(answer));
        when(answer.questionId()).thenReturn(20L);
        when(questionRepository.findById(20L)).thenReturn(Optional.of(question));
        when(identityApi.canManageCourse(1L, any())).thenReturn(false);
        when(question.isAccepted(10L)).thenReturn(false);

        useCase.execute(actor, 10L);

        verify(answer).softDelete(eq(actor), eq(false), eq(false), any(Instant.class));
        verify(answerRepository).save(answer);
    }

    @Test
    void execute_fail_not_found() {
        when(answerRepository.findById(99L)).thenReturn(Optional.empty());
        NotFoundException ex = assertThrows(NotFoundException.class,
                () -> useCase.execute(actor,99L));
        assertEquals("QA‑4002", ex.code());
    }
}

package com.campusoverflow.qa.application.command;

import com.campusoverflow.qa.domain.MarkdownBody;
import com.campusoverflow.qa.domain.Question;
import com.campusoverflow.qa.domain.QuestionRepository;
import com.campusoverflow.qa.domain.Tags;
import com.campusoverflow.qa.domain.Title;
import com.campusoverflow.shared.security.Actor;
import com.campusoverflow.shared.security.Role;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.Instant;
import java.util.List;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
class PostQuestionUseCaseTest {

    @Mock
    private QuestionRepository questionRepository;

    @InjectMocks
    private PostQuestionUseCase useCase;

    //严格按照 Actor record 的三个参数： userId, displayName, role
    private final Actor studentActor = new Actor(1001L, "学生小明", Role.STUDENT);

    @Test
    void execute_ShouldCreateAndSaveQuestion() {
        Title title = new Title("Java 泛型如何擦除？");
        MarkdownBody body = new MarkdownBody("请问泛型擦除底层原理是什么？");
        //Tags.of 需要传入 Collection<String>
        Tags tags = Tags.of(List.of("Java", "泛型"));

        Instant now = Instant.now();
        Question question = Question.post(studentActor.userId(), 1L, title, body, tags, now);
        question.assignId(10L);

        when(questionRepository.save(any(Question.class))).thenReturn(question);

        Long resultId = useCase.execute(studentActor, 1L, title, body, tags);

        assertEquals(10L, resultId);
        verify(questionRepository).save(any(Question.class));
    }
}
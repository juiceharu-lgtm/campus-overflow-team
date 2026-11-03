package com.campusoverflow.qa.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.campusoverflow.shared.domain.BusinessRuleException;
import com.campusoverflow.shared.domain.ConflictException;
import com.campusoverflow.shared.domain.ForbiddenException;
import com.campusoverflow.shared.security.Actor;
import com.campusoverflow.shared.security.Role;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class AnswerTest {

    static final Instant NOW = Instant.parse("2026-10-01T08:00:00Z");
    static final Actor ASKER = new Actor(1, "提问者", Role.STUDENT);
    static final Actor ANSWER_AUTHOR = new Actor(2, "回答作者", Role.STUDENT);
    static final Actor OTHER = new Actor(3, "路人", Role.STUDENT);
    static final Actor TEACHER = new Actor(9, "王老师", Role.TEACHER);

    static Question persistedQuestion() {
        Question q = Question.post(ASKER.userId(), 100L, new Title("红黑树插入后如何判断旋转方向？"),
                MarkdownBody.ofQuestion("插入节点后出现连续红色节点，什么时候左旋、什么时候右旋？"),
                Tags.of(List.of("数据结构", "红黑树")), NOW);
        q.assignId(10);
        return q;
    }

    static Answer persistedAnswer(Question q) {
        Answer a = Answer.submit(q, ANSWER_AUTHOR.userId(), MarkdownBody.ofAnswer("看叔叔节点的颜色和插入位置。"), NOW);
        a.assignId(50);
        return a;
    }

    @Nested
    class Submitting {
        @Test
        void cannotSubmitToDeletedQuestion() {
            Question q = persistedQuestion();
            q.softDelete(ASKER, false, NOW);
            assertThatThrownBy(() -> Answer.submit(q, ANSWER_AUTHOR.userId(), MarkdownBody.ofAnswer("测试回答"), NOW))
                    .isInstanceOf(BusinessRuleException.class);
        }

        @Test
        void bodyMustPassMarkdownRuleCheck() {
            assertThatThrownBy(() -> MarkdownBody.ofAnswer(""))
                    .isInstanceOf(BusinessRuleException.class);
        }
    }

    @Nested
    class Editing {
        @Test
        void onlyAuthorOrManagerCanEditAnswer() {
            Question q = persistedQuestion();
            Answer a = persistedAnswer(q);

            // 路人不能编辑
            assertThatThrownBy(() -> a.edit(OTHER, false, MarkdownBody.ofAnswer("路人改回答"), NOW.plusSeconds(60)))
                    .isInstanceOf(ForbiddenException.class);

            // 作者自己可以编辑
            a.edit(ANSWER_AUTHOR, false, MarkdownBody.ofAnswer("作者修改后的回答"), NOW.plusSeconds(60));
            assertThat(a.body().value()).contains("作者修改后的回答");

            // 老师（manager）可以代编辑
            a.edit(TEACHER, true, MarkdownBody.ofAnswer("老师修改后的回答"), NOW.plusSeconds(120));
            assertThat(a.body().value()).contains("老师修改后的回答");
        }

        @Test
        void deletedAnswerCannotBeEdited() {
            Question q = persistedQuestion();
            Answer a = persistedAnswer(q);
            // 第三个参数 answerIsAccepted=false
            a.softDelete(ANSWER_AUTHOR, false, false, NOW);

            assertThatThrownBy(() -> a.edit(ANSWER_AUTHOR, false, MarkdownBody.ofAnswer("想编辑已删回答"), NOW.plusSeconds(60)))
                    .isInstanceOf(BusinessRuleException.class);
        }
    }

    @Nested
    class Deleting {
        @Test
        void authorCannotDeleteAcceptedAnswer() {
            Question q = persistedQuestion();
            Answer a = persistedAnswer(q);
            q.accept(a, ASKER, false);

            // answerIsAccepted = true
            assertThatThrownBy(() -> a.softDelete(ANSWER_AUTHOR, false, true, NOW))
                    .isInstanceOf(BusinessRuleException.class);
        }

        @Test
        void managerTeacherCanDeleteAcceptedAnswer() {
            Question q = persistedQuestion();
            Answer a = persistedAnswer(q);
            q.accept(a, ASKER, false);

            a.softDelete(TEACHER, true, true, NOW);
            assertThat(a.isDeleted()).isTrue();
        }

        @Test
        void authorCanDeleteUnacceptedAnswer() {
            Question q = persistedQuestion();
            Answer a = persistedAnswer(q);

            a.softDelete(ANSWER_AUTHOR, false, false, NOW);
            assertThat(a.isDeleted()).isTrue();
        }

        @Test
        void otherUserCannotDeleteAnswer() {
            Question q = persistedQuestion();
            Answer a = persistedAnswer(q);

            assertThatThrownBy(() -> a.softDelete(OTHER, false, false, NOW))
                    .isInstanceOf(ForbiddenException.class);
        }
    }

    @Nested
    class Endorse {
        @Test
        void teacherCanEndorseAnswer() {
            Question q = persistedQuestion();
            Answer a = persistedAnswer(q);
            a.endorse(TEACHER, true, NOW);
            assertThat(a.endorsedBy()).isNotNull();
            assertThat(a.endorsedBy()).isEqualTo(TEACHER.userId());
        }

        @Test
        void studentCannotEndorse() {
            Question q = persistedQuestion();
            Answer a = persistedAnswer(q);
            assertThatThrownBy(() -> a.endorse(ANSWER_AUTHOR, false, NOW))
                    .isInstanceOf(ForbiddenException.class);
        }

        @Test
        void cannotEndorseTwice() {
            Question q = persistedQuestion();
            Answer a = persistedAnswer(q);
            a.endorse(TEACHER, true, NOW);
            assertThatThrownBy(() -> a.endorse(TEACHER, true, NOW.plusSeconds(100)))
                    .isInstanceOf(ConflictException.class);
        }

        @Test
        void deletedAnswerCannotBeEndorsed() {
            Question q = persistedQuestion();
            Answer a = persistedAnswer(q);
            a.softDelete(ANSWER_AUTHOR, false, false, NOW);
            assertThatThrownBy(() -> a.endorse(TEACHER, true, NOW))
                    .isInstanceOf(BusinessRuleException.class);
        }
    }
}

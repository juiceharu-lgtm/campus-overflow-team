package com.campusoverflow.qa.domain;

import com.campusoverflow.shared.domain.BusinessRuleException;
import com.campusoverflow.shared.domain.ConflictException;
import com.campusoverflow.shared.domain.ForbiddenException;
import com.campusoverflow.shared.security.Actor;
import com.campusoverflow.shared.security.Role;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import static org.junit.jupiter.api.Assertions.*;

class AnswerDomainTest {

    private final Instant now = Instant.now();
    private final MarkdownBody originBody = MarkdownBody.ofAnswer("原始回答内容");
    private final MarkdownBody newBody = MarkdownBody.ofAnswer("修改后的回答");

    private final Actor authorActor = new Actor(1001L, "回答作者", Role.STUDENT);
    private final Actor otherStudentActor = new Actor(1002L, "其他学生", Role.STUDENT);
    private final Actor teacherManagerActor = new Actor(2001L, "课程教师", Role.TEACHER);

    private Answer buildNormalAnswer() {
        return Answer.reconstitute(
                100L,
                1L,
                authorActor.userId(),
                originBody,
                0,
                null,
                now,
                now,
                null,
                0L
        );
    }

    // edit：作者编辑成功
    @Test
    void edit_byAuthor_success() {
        Answer ans = buildNormalAnswer();
        ans.edit(authorActor, false, newBody, now);
        assertEquals(newBody, ans.body());
        assertEquals(now, ans.updatedAt());
    }

    // edit：课程管理员编辑成功
    @Test
    void edit_byCourseManager_success() {
        Answer ans = buildNormalAnswer();
        ans.edit(teacherManagerActor, true, newBody, now);
        assertEquals(newBody, ans.body());
    }

    // edit：其他学生编辑 → 无权限异常
    @Test
    void edit_byOtherStudent_throwForbidden() {
        Answer ans = buildNormalAnswer();
        ForbiddenException ex = assertThrows(ForbiddenException.class,
                () -> ans.edit(otherStudentActor, false, newBody, now));
        assertEquals("QA‑3004", ex.getMessage());
    }

    // edit：已经删除的回答不能编辑
    @Test
    void edit_deletedAnswer_throwRule() {
        Answer ans = buildNormalAnswer();
        ans.softDelete(teacherManagerActor, true, false, now);
        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> ans.edit(authorActor, false, newBody, now));
        assertEquals("QA‑1021", ex.getMessage());
    }

    // endorse：课程教师认证回答成功
    @Test
    void endorse_byCourseManager_success() {
        Answer ans = buildNormalAnswer();
        ans.endorse(teacherManagerActor, true);
        assertEquals(teacherManagerActor.userId(), ans.endorsedBy());
    }

    // endorse：已经认证过，重复认证抛冲突异常
    @Test
    void endorse_alreadyEndorsed_throwConflict() {
        Answer ans = buildNormalAnswer();
        ans.endorse(teacherManagerActor, true);
        ConflictException ex = assertThrows(ConflictException.class,
                () -> ans.endorse(teacherManagerActor, true));
        assertEquals("QA‑2003", ex.getMessage());
    }

    // endorse：普通学生不能认证回答
    @Test
    void endorse_byStudent_throwForbidden() {
        Answer ans = buildNormalAnswer();
        ForbiddenException ex = assertThrows(ForbiddenException.class,
                () -> ans.endorse(otherStudentActor, false));
        assertEquals("QA‑3005", ex.getMessage());
    }

    // endorse：已删除回答不能认证
    @Test
    void endorse_deletedAnswer_throwRule() {
        Answer ans = buildNormalAnswer();
        ans.softDelete(teacherManagerActor, true, false, now);
        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> ans.endorse(teacherManagerActor, true));
        assertEquals("QA‑1021", ex.getMessage());
    }

    // softDelete：作者删除未被采纳的回答，成功
    @Test
    void softDelete_author_notAccepted_success() {
        Answer ans = buildNormalAnswer();
        ans.softDelete(authorActor, false, false, now);
        assertTrue(ans.isDeleted());
    }

    // softDelete：作者不能删除已经被采纳的回答
    @Test
    void softDelete_author_accepted_throwRule() {
        Answer ans = buildNormalAnswer();
        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> ans.softDelete(authorActor, false, true, now));
        assertEquals("QA‑1015", ex.getMessage());
    }

    // softDelete：别的学生无权删除
    @Test
    void softDelete_otherStudent_notManager_throwForbidden() {
        Answer ans = buildNormalAnswer();
        ForbiddenException ex = assertThrows(ForbiddenException.class,
                () -> ans.softDelete(otherStudentActor, false, false, now));
        assertEquals("QA‑3006", ex.getMessage());
    }

    // softDelete：课程管理员可以随便删（哪怕已采纳）
    @Test
    void softDelete_courseManager_anyCase_success() {
        Answer ans = buildNormalAnswer();
        ans.softDelete(teacherManagerActor, true, true, now);
        assertTrue(ans.isDeleted());
    }

    // softDelete：已经删过的回答再次删除抛异常
    @Test
    void softDelete_alreadyDeleted_throwRule() {
        Answer ans = buildNormalAnswer();
        ans.softDelete(teacherManagerActor, true, false, now);
        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> ans.softDelete(teacherManagerActor, true, false, now));
        assertEquals("QA‑1021", ex.getMessage());
    }
}

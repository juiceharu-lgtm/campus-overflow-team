package com.campusoverflow.identity.application;

import com.campusoverflow.identity.domain.CourseApplication;
import com.campusoverflow.identity.domain.CourseApplicationRepository;
import com.campusoverflow.identity.domain.CourseApplicationStatus;
import com.campusoverflow.identity.api.IdentityApi;
import com.campusoverflow.shared.audit.AuditTrail;
import com.campusoverflow.shared.domain.BusinessRuleException;
import com.campusoverflow.shared.domain.ForbiddenException;
import com.campusoverflow.shared.domain.NotFoundException;
import com.campusoverflow.shared.security.Actor;
import com.campusoverflow.shared.security.Role;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CourseApplicationServiceTest {

    @Mock
    private CourseApplicationRepository repository;
    @Mock
    private IdentityApi identityApi;
    @Mock
    private Clock clock;
    @Mock
    private AuditTrail auditTrail;

    @InjectMocks
    private CourseApplicationService service;

    private final Instant fixedNow = Instant.parse("2025-01-01T10:00:00Z");
    private final Actor courseTeacher = new Actor(100L, "teacher", Role.TEACHER);
    private final Actor normalStudent = new Actor(200L, "student", Role.STUDENT);
    private final Long appId = 1L;
    private final Long courseId = 10L;
    private final Long applicantId = 300L;

    @Test
    void teacher_approve_pending_application_success() {
        CourseApplication app = CourseApplication.create(applicantId, courseId, fixedNow);
        when(repository.findById(appId)).thenReturn(Optional.of(app));
        when(identityApi.canManageCourse(courseTeacher.userId(), courseId)).thenReturn(true);
        when(clock.instant()).thenReturn(fixedNow);

        service.approve(courseTeacher, appId);

        Assertions.assertEquals(CourseApplicationStatus.APPROVED, app.getStatus());
        verify(repository).save(app);
        verify(auditTrail).record(anyString(), anyLong(), anyString(), anyString(), any());
    }

    @Test
    void teacher_reject_pending_application_success() {
        CourseApplication app = CourseApplication.create(applicantId, courseId, fixedNow);
        when(repository.findById(appId)).thenReturn(Optional.of(app));
        when(identityApi.canManageCourse(courseTeacher.userId(), courseId)).thenReturn(true);
        when(clock.instant()).thenReturn(fixedNow);

        service.reject(courseTeacher, appId, "名额已满");

        Assertions.assertEquals(CourseApplicationStatus.REJECTED, app.getStatus());
        Assertions.assertEquals("名额已满", app.getRejectReason());
        verify(repository).save(app);
        verify(auditTrail).record(anyString(), anyLong(), anyString(), anyString(), any());
    }

    @Test
    void student_cannot_approve_forbidden() {
        CourseApplication app = CourseApplication.create(applicantId, courseId, fixedNow);
        when(repository.findById(appId)).thenReturn(Optional.of(app));
        when(identityApi.canManageCourse(normalStudent.userId(), courseId)).thenReturn(false);

        ForbiddenException ex = Assertions.assertThrows(ForbiddenException.class,
                () -> service.approve(normalStudent, appId));
        // 同时校验错误码 + 提示文本，完全符合实验书规范
        Assertions.assertEquals("ID-3003", ex.code());
        Assertions.assertEquals("只有课程管理者可以审批选课申请", ex.getMessage());
        verify(auditTrail, never()).record(any(),any(),any(),any(),any());
    }

    @Test
    void approve_already_processed_throw_business() {
        CourseApplication app = CourseApplication.create(applicantId, courseId, fixedNow);
        when(repository.findById(appId)).thenReturn(Optional.of(app));
        when(identityApi.canManageCourse(courseTeacher.userId(), courseId)).thenReturn(true);
        when(clock.instant()).thenReturn(fixedNow);

        service.approve(courseTeacher, appId);
        BusinessRuleException ex = Assertions.assertThrows(BusinessRuleException.class,
                () -> service.approve(courseTeacher, appId));
        Assertions.assertEquals("ID-3004", ex.code());
        Assertions.assertEquals("仅待审批状态的申请可以执行审批操作", ex.getMessage());
    }

    @Test
    void approve_not_exist_throw_not_found() {
        when(repository.findById(appId)).thenReturn(Optional.empty());
        NotFoundException ex = Assertions.assertThrows(NotFoundException.class,
                () -> service.approve(courseTeacher, appId));
        Assertions.assertEquals("ID-3001", ex.code());
        Assertions.assertEquals("选课申请不存在", ex.getMessage());
    }
}

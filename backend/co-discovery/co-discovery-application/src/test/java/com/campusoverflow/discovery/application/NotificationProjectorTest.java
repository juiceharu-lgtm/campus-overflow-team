package com.campusoverflow.discovery.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.campusoverflow.discovery.domain.notification.Notification;
import com.campusoverflow.discovery.domain.notification.NotificationRepository;
import com.campusoverflow.discovery.domain.notification.NotificationType;
import com.campusoverflow.identity.api.IdentityApi;
import com.campusoverflow.identity.api.UserSummary;
import com.campusoverflow.qa.api.QaQueryApi;
import com.campusoverflow.qa.api.QuestionRef;
import com.campusoverflow.qa.api.event.AnswerAcceptedEvent;
import com.campusoverflow.qa.api.event.AnswerEndorsedEvent;
import com.campusoverflow.qa.api.event.AnswerSubmittedEvent;
import com.campusoverflow.qa.api.event.CommentPostedEvent;
import com.campusoverflow.reputation.api.event.BadgeAwardedEvent;
import com.campusoverflow.reputation.api.event.BountyAwardedEvent;
import com.campusoverflow.shared.security.Role;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationProjectorTest {

    private static final Instant NOW = Instant.parse("2026-10-22T03:00:00Z");

    @Mock private NotificationRepository notifications;
    @Mock private IdentityApi identity;
    @Mock private QaQueryApi qa;

    private NotificationProjector projector;

    @BeforeEach
    void setUp() {
        projector = new NotificationProjector(notifications, identity, qa);
    }

    @Test
    void answerEventsCreateRecipientSpecificNotifications() {
        when(identity.findUser(1)).thenReturn(Optional.of(user(1, "小明")));
        when(identity.findUser(2)).thenReturn(Optional.of(user(2, "小红")));
        when(identity.findUser(9)).thenReturn(Optional.of(user(9, "王老师")));

        projector.on(new AnswerSubmittedEvent("submitted", NOW, 20, 10, 2, 1, "红黑树问题"));
        projector.on(new AnswerAcceptedEvent("accepted", NOW.plusSeconds(1), 20, 10, 2, 1, 1, "红黑树问题"));
        projector.on(new AnswerEndorsedEvent("endorsed", NOW.plusSeconds(2), 20, 10, 2, 9, "红黑树问题"));

        ArgumentCaptor<Notification> saved = ArgumentCaptor.forClass(Notification.class);
        verify(notifications, times(3)).save(saved.capture());
        List<Notification> values = saved.getAllValues();
        assertThat(values).extracting(Notification::recipientId).containsExactly(1L, 2L, 2L);
        assertThat(values).extracting(Notification::type).containsExactly(
                NotificationType.ANSWER_RECEIVED, NotificationType.ANSWER_ACCEPTED,
                NotificationType.ANSWER_ENDORSED);
        assertThat(values).allSatisfy(n -> assertThat(n.link()).isEqualTo("/questions/10#answer-20"));
        assertThat(values.get(0).title()).contains("小红", "红黑树问题");
        assertThat(values.get(1).title()).contains("小明", "红黑树问题");
        assertThat(values.get(2).title()).contains("王老师", "红黑树问题");
    }

    @Test
    void commentMentionsKnownOtherUsersOnAnExistingQuestion() {
        when(qa.findQuestion(10)).thenReturn(Optional.of(new QuestionRef(10, 7, null, "并发问题", null, false)));
        when(identity.findByDisplayName("作者")).thenReturn(Optional.of(user(1, "作者")));
        when(identity.findByDisplayName("小红")).thenReturn(Optional.of(user(2, "小红")));
        when(identity.findByDisplayName("不存在")).thenReturn(Optional.empty());
        when(identity.findUser(1)).thenReturn(Optional.of(user(1, "作者")));

        projector.on(new CommentPostedEvent("comment", NOW, 30, 10, 1,
                "@作者 @小红 @小红 @不存在 请看一下"));

        ArgumentCaptor<Notification> saved = ArgumentCaptor.forClass(Notification.class);
        verify(notifications).save(saved.capture());
        assertThat(saved.getValue().recipientId()).isEqualTo(2);
        assertThat(saved.getValue().type()).isEqualTo(NotificationType.MENTIONED);
        assertThat(saved.getValue().title()).contains("作者", "并发问题");
        assertThat(saved.getValue().link()).isEqualTo("/questions/10");
    }

    @Test
    void commentOnMissingOrDeletedQuestionProducesNoNotification() {
        CommentPostedEvent missing = new CommentPostedEvent("missing", NOW, 30, 10, 1, "@小红 看一下");
        when(qa.findQuestion(10)).thenReturn(Optional.empty());
        projector.on(missing);

        CommentPostedEvent deleted = new CommentPostedEvent("deleted", NOW, 31, 11, 1, "@小红 看一下");
        when(qa.findQuestion(11)).thenReturn(Optional.of(new QuestionRef(11, 1, null, "已删除", null, true)));
        projector.on(deleted);

        verifyNoInteractions(notifications);
        verify(identity, never()).findByDisplayName(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void bountyAndBadgeEventsCreateTheExpectedNotifications() {
        projector.on(new BountyAwardedEvent("bounty", NOW, 40, 10, 1, 2, 50));
        projector.on(new BadgeAwardedEvent("badge", NOW.plusSeconds(1), 3, "HELPFUL", "热心助人"));

        ArgumentCaptor<Notification> saved = ArgumentCaptor.forClass(Notification.class);
        verify(notifications, times(2)).save(saved.capture());
        assertThat(saved.getAllValues()).extracting(Notification::recipientId).containsExactly(2L, 3L);
        assertThat(saved.getAllValues()).extracting(Notification::type)
                .containsExactly(NotificationType.BOUNTY_AWARDED, NotificationType.BADGE_AWARDED);
        assertThat(saved.getAllValues().get(0).title()).contains("50");
        assertThat(saved.getAllValues().get(1).title()).contains("热心助人");
    }

    @Test
    void duplicateAndSelfTriggeredEventsDoNotCreateNotifications() {
        when(notifications.exists("duplicate", 1)).thenReturn(true);
        when(identity.findUser(2)).thenReturn(Optional.of(user(2, "小红")));
        projector.on(new AnswerSubmittedEvent("duplicate", NOW, 20, 10, 2, 1, "问题"));

        projector.on(new AnswerSubmittedEvent("self-answer", NOW, 21, 10, 1, 1, "问题"));
        projector.on(new AnswerAcceptedEvent("self-accept", NOW, 21, 10, 1, 1, 1, "问题"));
        projector.on(new AnswerEndorsedEvent("self-endorse", NOW, 21, 10, 1, 1, "问题"));
        projector.on(new BountyAwardedEvent("self-bounty", NOW, 40, 10, 1, 1, 50));

        verify(notifications, never()).save(org.mockito.ArgumentMatchers.any());
    }

    private static UserSummary user(long id, String name) {
        return new UserSummary(id, name, Role.STUDENT, false, null);
    }
}

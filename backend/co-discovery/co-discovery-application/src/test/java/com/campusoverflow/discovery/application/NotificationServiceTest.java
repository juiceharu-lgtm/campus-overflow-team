package com.campusoverflow.discovery.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.campusoverflow.discovery.domain.notification.Notification;
import com.campusoverflow.discovery.domain.notification.NotificationRepository;
import com.campusoverflow.discovery.domain.notification.NotificationType;
import com.campusoverflow.shared.domain.ForbiddenException;
import com.campusoverflow.shared.domain.NotFoundException;
import com.campusoverflow.shared.page.PageRequest;
import com.campusoverflow.shared.page.PageResult;
import com.campusoverflow.shared.security.Actor;
import com.campusoverflow.shared.security.Role;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    private static final Actor USER = new Actor(7, "小明", Role.STUDENT);
    private static final Instant NOW = Instant.parse("2026-10-22T03:00:00Z");

    @Mock private NotificationRepository notifications;

    private NotificationService service;

    @BeforeEach
    void setUp() {
        service = new NotificationService(notifications);
    }

    @Test
    void listMapsOnlyTheCurrentRecipientsPage() {
        PageRequest page = new PageRequest(2, 10);
        Notification unread = notification(1, 7, false);
        Notification read = notification(2, 7, true);
        when(notifications.findByRecipient(7, page)).thenReturn(new PageResult<>(List.of(unread, read), 12, 2, 10));

        PageResult<NotificationView> result = service.list(USER, page);

        assertThat(result.total()).isEqualTo(12);
        assertThat(result.items()).extracting(NotificationView::id).containsExactly(1L, 2L);
        assertThat(result.items()).extracting(NotificationView::type)
                .containsOnly(NotificationType.ANSWER_RECEIVED.name());
        assertThat(result.items()).extracting(NotificationView::read).containsExactly(false, true);
        verify(notifications).findByRecipient(7, page);
    }

    @Test
    void unreadAndReadAllAreScopedToCurrentUser() {
        when(notifications.countUnread(7)).thenReturn(3L);
        when(notifications.markAllRead(7)).thenReturn(3);

        assertThat(service.unreadCount(USER)).isEqualTo(3);
        assertThat(service.markAllRead(USER)).isEqualTo(3);
        verify(notifications).countUnread(7);
        verify(notifications).markAllRead(7);
    }

    @Test
    void markReadChangesAndPersistsOwnedNotification() {
        Notification notification = notification(1, 7, false);
        when(notifications.findById(1)).thenReturn(Optional.of(notification));

        service.markRead(USER, 1);

        assertThat(notification.read()).isTrue();
        verify(notifications).save(notification);
    }

    @Test
    void markReadDistinguishesMissingFromForeignNotification() {
        when(notifications.findById(99)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.markRead(USER, 99))
                .isInstanceOfSatisfying(NotFoundException.class,
                        error -> assertThat(error.code()).isEqualTo("DIS-4001"));

        Notification foreign = notification(2, 8, false);
        when(notifications.findById(2)).thenReturn(Optional.of(foreign));
        assertThatThrownBy(() -> service.markRead(USER, 2))
                .isInstanceOfSatisfying(ForbiddenException.class,
                        error -> assertThat(error.code()).isEqualTo("DIS-3001"));
        assertThat(foreign.read()).isFalse();
        verify(notifications, never()).save(foreign);
    }

    private static Notification notification(long id, long recipientId, boolean read) {
        return Notification.reconstitute(id, recipientId, NotificationType.ANSWER_RECEIVED, "收到回答",
                "/questions/10", "evt-" + id, read, NOW.plusSeconds(id));
    }
}

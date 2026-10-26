package com.campusoverflow.discovery.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.campusoverflow.discovery.application.NotificationProjector;
import com.campusoverflow.discovery.application.NotificationService;
import com.campusoverflow.discovery.domain.notification.NotificationRepository;
import com.campusoverflow.discovery.domain.search.SearchDocument;
import com.campusoverflow.discovery.domain.search.SearchQuery;
import com.campusoverflow.discovery.domain.search.SearchSort;
import com.campusoverflow.discovery.infrastructure.notification.NotificationJpaEntity;
import com.campusoverflow.discovery.infrastructure.notification.NotificationJpaRepository;
import com.campusoverflow.discovery.infrastructure.notification.NotificationRepositoryAdapter;
import com.campusoverflow.discovery.infrastructure.search.JdbcSearchIndex;
import com.campusoverflow.discovery.infrastructure.search.MySqlFullTextSearchAdapter;
import com.campusoverflow.identity.api.IdentityApi;
import com.campusoverflow.identity.api.UserSummary;
import com.campusoverflow.qa.api.QaQueryApi;
import com.campusoverflow.qa.api.event.AnswerSubmittedEvent;
import com.campusoverflow.shared.domain.ForbiddenException;
import com.campusoverflow.shared.page.PageRequest;
import com.campusoverflow.shared.security.Actor;
import com.campusoverflow.shared.security.Role;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Opt-in integration tests against a migrated MySQL 8 database. They are intentionally disabled unless
 * DISCOVERY_IT_DB_URL, DISCOVERY_IT_DB_USER and DISCOVERY_IT_DB_PASSWORD are supplied by the caller.
 * Upstream events are constructed directly, so these tests prove Discovery's database integration rather than a
 * cross-context end-to-end flow.
 */
@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.flyway.enabled=false"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({NotificationRepositoryAdapter.class, JdbcSearchIndex.class, MySqlFullTextSearchAdapter.class})
@EnabledIfEnvironmentVariable(named = "DISCOVERY_IT_DB_URL", matches = ".+")
class DiscoveryMySqlIT {

    private static final long QUESTION_ID = uniquePositiveId();
    private static final long RECIPIENT_ID = uniquePositiveId();
    private static final String ANSWER_EVENT_ID = "dis-it-answer-" + UUID.randomUUID();

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EntityScan(basePackageClasses = NotificationJpaEntity.class)
    @EnableJpaRepositories(basePackageClasses = NotificationJpaRepository.class)
    static class TestApplication {
    }

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> requiredEnvironment("DISCOVERY_IT_DB_URL"));
        registry.add("spring.datasource.username", () -> requiredEnvironment("DISCOVERY_IT_DB_USER"));
        registry.add("spring.datasource.password", () -> requiredEnvironment("DISCOVERY_IT_DB_PASSWORD"));
    }

    @Autowired
    NotificationRepository notifications;

    @Autowired
    JdbcSearchIndex searchIndex;

    @Autowired
    MySqlFullTextSearchAdapter search;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    PlatformTransactionManager transactionManager;

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void repeatedAnswerEventCreatesOnePersistentNotificationAndOnlyOwnerCanReadIt() {
        boolean created = false;
        try {
            IdentityApi identity = mock(IdentityApi.class);
            when(identity.findUser(88L)).thenReturn(Optional.of(
                    new UserSummary(88L, "回答者", Role.STUDENT, false, "计算机学院")));
            NotificationProjector projector = new NotificationProjector(
                    notifications, identity, mock(QaQueryApi.class));
            AnswerSubmittedEvent event = new AnswerSubmittedEvent(
                    ANSWER_EVENT_ID, Instant.parse("2026-10-25T06:00:00Z"),
                    9_300_001L, QUESTION_ID, 88L, RECIPIENT_ID, "并发结构如何选择");

            projector.on(event);
            created = true;
            projector.on(event);

            NotificationService service = new NotificationService(notifications);
            Actor owner = new Actor(RECIPIENT_ID, "问题作者", Role.STUDENT);
            var page = service.list(owner, new PageRequest(1, 20));
            assertThat(page.items()).hasSize(1);
            assertThat(service.unreadCount(owner)).isEqualTo(1);

            long notificationId = page.items().getFirst().id();
            Actor anotherUser = new Actor(RECIPIENT_ID + 1, "其他用户", Role.STUDENT);
            assertThatThrownBy(() -> service.markRead(anotherUser, notificationId))
                    .isInstanceOf(ForbiddenException.class)
                    .extracting(e -> ((ForbiddenException) e).code())
                    .isEqualTo("DIS-3001");

            service.markRead(owner, notificationId);
            assertThat(service.unreadCount(owner)).isZero();
        } finally {
            if (created) {
                cleanupNotificationFixture();
            }
        }
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void writesSearchProjectionFindsChineseKeywordAndHidesSoftDeletedQuestion() {
        Instant createdAt = Instant.parse("2026-10-25T06:10:00Z");
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        boolean inserted = false;
        try {
            transaction.executeWithoutResult(status -> searchIndex.insert(new SearchDocument(
                    QUESTION_ID, 77L, 88L, "并发编程中的跳表选择",
                    "跳表适合并发写入，红黑树适合单线程有序查询。",
                    List.of("数据结构", "并发编程"), createdAt)));
            inserted = true;

            SearchQuery query = new SearchQuery("跳表", null, null, SearchSort.RELEVANCE,
                    false, false, new PageRequest(1, 20));
            var beforeDelete = transaction.execute(status -> search.search(query));
            assertThat(beforeDelete).isNotNull();
            assertThat(beforeDelete.items()).singleElement().satisfies(hit -> {
                assertThat(hit.questionId()).isEqualTo(QUESTION_ID);
                assertThat(hit.tags()).containsExactly("并发编程", "数据结构");
            });

            transaction.executeWithoutResult(status -> searchIndex.markDeleted(QUESTION_ID));

            var afterDelete = transaction.execute(status -> search.search(query));
            assertThat(afterDelete).isNotNull();
            assertThat(afterDelete.items()).isEmpty();
        } finally {
            if (inserted) {
                transaction.executeWithoutResult(status -> {
                    jdbc.update("DELETE FROM dis_question_tag WHERE question_id = ?", QUESTION_ID);
                    jdbc.update("DELETE FROM dis_search_doc WHERE question_id = ?", QUESTION_ID);
                });
            }
        }
    }

    private static String requiredEnvironment(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(name + " is required when DiscoveryMySqlIT is enabled");
        }
        return value;
    }

    private void cleanupNotificationFixture() {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> jdbc.update(
                "DELETE FROM dis_notification WHERE source_event_id = ? AND recipient_id = ?",
                ANSWER_EVENT_ID, RECIPIENT_ID));
    }

    private static long uniquePositiveId() {
        long value = UUID.randomUUID().getMostSignificantBits() & Long.MAX_VALUE;
        return value == 0 ? 1 : value;
    }
}

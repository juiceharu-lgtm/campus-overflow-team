package com.campusoverflow.discovery.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.campusoverflow.discovery.domain.notification.MentionParser;
import com.campusoverflow.discovery.domain.notification.Notification;
import com.campusoverflow.discovery.domain.notification.NotificationType;
import com.campusoverflow.discovery.domain.search.RelatedQuestionPolicy;
import com.campusoverflow.discovery.domain.search.SearchIndex.RelatedCandidate;
import com.campusoverflow.discovery.domain.search.SearchQuery;
import com.campusoverflow.discovery.domain.search.SearchSort;
import com.campusoverflow.shared.domain.BusinessRuleException;
import com.campusoverflow.shared.domain.ForbiddenException;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class DiscoveryDomainTest {

    @Test
    void searchQueryStripsOperatorsAndPicksDefaultSort() {
        SearchQuery q = new SearchQuery("  +红黑树 -旋转*  ", null, " Java ", null, false, false, null);
        assertThat(q.terms()).containsExactly("红黑树", "旋转");
        assertThat(q.sort()).isEqualTo(SearchSort.RELEVANCE);
        assertThat(q.tag()).isEqualTo("java");

        SearchQuery noKeyword = new SearchQuery("   ", null, null, SearchSort.RELEVANCE, false, false, null);
        assertThat(noKeyword.keyword()).isNull();
        assertThat(noKeyword.sort()).isEqualTo(SearchSort.NEWEST);
    }

    @Test
    void singleCharacterTermsAreRejectedBecauseOfNgramTokenSize() {
        assertThatThrownBy(() -> new SearchQuery("树", null, null, null, false, false, null))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void jaccardRanking() {
        assertThat(RelatedQuestionPolicy.jaccard(2, 3, 3)).isEqualTo(0.5);
        List<RelatedCandidate> ranked = RelatedQuestionPolicy.rank(List.of(
                new RelatedCandidate(1, "A", 1, 5, 99, 3, true),   // J = 1/7
                new RelatedCandidate(2, "B", 2, 2, 0, 0, false),   // J = 2/3
                new RelatedCandidate(3, "C", 2, 3, 5, 1, true)     // J = 2/4
        ), 3, 2);
        assertThat(ranked).extracting(RelatedCandidate::questionId).containsExactly(2L, 3L);
    }

    @Test
    void mentionsAreParsedDeduplicatedAndCapped() {
        assertThat(MentionParser.extract("感谢 @王老师，另外 @小明 @小明 你看看")).containsExactly("王老师", "小明");
        assertThat(MentionParser.extract("@a1 @b2 @c3 @d4 @e5 @f6")).hasSize(MentionParser.MAX_MENTIONS);
        assertThat(MentionParser.extract("邮箱 a@b 不算")).isEmpty();
    }

    @Test
    void onlyRecipientCanMarkNotificationRead() {
        Notification n = Notification.create(7, NotificationType.ANSWER_RECEIVED, "你的问题收到新回答", "/questions/1",
                "evt-1", Instant.now());
        assertThatThrownBy(() -> n.markRead(8)).isInstanceOf(ForbiddenException.class);
        n.markRead(7);
        assertThat(n.read()).isTrue();
    }

    @Test
    void searchQuerySplitsWhitespaceKeepsDuplicatesAndLimitsTerms() {
        SearchQuery q = new SearchQuery("算法\t算法\n数据 结构 图论 编译", null, null, null, false, false, null);

        assertThat(q.terms()).containsExactly("算法", "算法", "数据", "结构", "图论");
    }

    @Test
    void searchQueryReturnsNoTermsWithoutAUsableKeyword() {
        SearchQuery missing = new SearchQuery(null, null, null, null, false, false, null);
        SearchQuery operatorsOnly = new SearchQuery(" + - * @ ", null, null, null, false, false, null);

        assertThat(missing.terms()).isEmpty();
        assertThat(operatorsOnly.terms()).isEmpty();
        assertThat(operatorsOnly.keyword()).isNull();
    }

    @Test
    void mentionsRespectPunctuationAndReturnEmptyForNoMentionText() {
        assertThat(MentionParser.extract("请看 @王老师，抄送 @小明。再次 @王老师！"))
                .containsExactly("王老师", "小明");
        assertThat(MentionParser.extract("这里没有提及任何人")).isEmpty();
        assertThat(MentionParser.extract("   ")).isEmpty();
        assertThat(MentionParser.extract(null)).isEmpty();
    }

    @Test
    void jaccardHandlesEmptySetsAndRankingFiltersAndLimits() {
        assertThat(RelatedQuestionPolicy.jaccard(0, 0, 0)).isZero();
        assertThat(RelatedQuestionPolicy.jaccard(0, 3, 2)).isZero();

        List<RelatedCandidate> ranked = RelatedQuestionPolicy.rank(List.of(
                new RelatedCandidate(1, "无交集", 0, 2, 100, 5, true),
                new RelatedCandidate(2, "较相关", 1, 2, 5, 1, false),
                new RelatedCandidate(3, "最相关", 2, 2, 0, 0, false)
        ), 2, 1);

        assertThat(ranked).extracting(RelatedCandidate::questionId).containsExactly(3L);
        assertThat(RelatedQuestionPolicy.rank(List.of(), 2, 5)).isEmpty();
        assertThat(RelatedQuestionPolicy.rank(ranked, 2, 0)).isEmpty();
    }

    @Test
    void relatedRankingUsesDocumentedTieBreakersDeterministically() {
        List<RelatedCandidate> ranked = RelatedQuestionPolicy.rank(List.of(
                new RelatedCandidate(4, "同分未采纳", 1, 2, 5, 0, false),
                new RelatedCandidate(3, "同分已采纳", 1, 2, 5, 1, true),
                new RelatedCandidate(2, "高分", 1, 2, 6, 0, false),
                new RelatedCandidate(1, "同分已采纳且 ID 小", 1, 2, 5, 1, true)
        ), 2, 10);

        assertThat(ranked).extracting(RelatedCandidate::questionId).containsExactly(2L, 1L, 3L, 4L);
    }

    @Test
    void notificationCreationStartsUnreadAndKeepsItsData() {
        Instant now = Instant.parse("2026-10-22T02:00:00Z");
        Notification n = Notification.create(7, NotificationType.MENTIONED, "你在评论中被提及", null,
                "evt-2", now);

        assertThat(n.id()).isNull();
        assertThat(n.recipientId()).isEqualTo(7);
        assertThat(n.type()).isEqualTo(NotificationType.MENTIONED);
        assertThat(n.title()).isEqualTo("你在评论中被提及");
        assertThat(n.link()).isNull();
        assertThat(n.sourceEventId()).isEqualTo("evt-2");
        assertThat(n.createdAt()).isEqualTo(now);
        assertThat(n.read()).isFalse();
    }

    @Test
    void notificationRequiresMandatoryData() {
        Instant now = Instant.parse("2026-10-22T02:00:00Z");

        assertThatThrownBy(() -> Notification.create(7, null, "标题", null, "evt-3", now))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Notification.create(7, NotificationType.MENTIONED, null, null, "evt-3", now))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Notification.create(7, NotificationType.MENTIONED, "标题", null, null, now))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Notification.create(7, NotificationType.MENTIONED, "标题", null, "evt-3", null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void markingNotificationReadIsIdempotentAndUsesDiscoveryErrorCode() {
        Notification n = Notification.create(7, NotificationType.ANSWER_ACCEPTED, "回答被采纳", "/questions/1",
                "evt-4", Instant.parse("2026-10-22T02:00:00Z"));

        assertThatThrownBy(() -> n.markRead(8))
                .isInstanceOfSatisfying(ForbiddenException.class,
                        error -> assertThat(error.code()).isEqualTo("DIS-3001"));

        n.markRead(7);
        n.markRead(7);
        assertThat(n.read()).isTrue();
    }
}

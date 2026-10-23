package com.campusoverflow.discovery.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.campusoverflow.discovery.domain.search.SearchHit;
import com.campusoverflow.discovery.domain.search.SearchIndex;
import com.campusoverflow.discovery.domain.search.SearchIndex.RelatedCandidate;
import com.campusoverflow.discovery.domain.search.SearchPort;
import com.campusoverflow.discovery.domain.search.SearchQuery;
import com.campusoverflow.identity.api.IdentityApi;
import com.campusoverflow.identity.api.UserSummary;
import com.campusoverflow.shared.page.PageRequest;
import com.campusoverflow.shared.page.PageResult;
import com.campusoverflow.shared.security.Role;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DiscoveryQueryServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-22T03:00:00Z");

    @Mock private SearchPort search;
    @Mock private SearchIndex index;
    @Mock private IdentityApi identity;

    private DiscoveryQueryService service;

    @BeforeEach
    void setUp() {
        service = new DiscoveryQueryService(search, index, identity);
    }

    @Test
    void searchMapsHitsAuthorsAndPaginationWithOneBatchLookup() {
        SearchQuery query = new SearchQuery("红黑树", 9L, "Java", null, false, true, new PageRequest(2, 10));
        SearchHit known = new SearchHit(11, "标题一", "摘要一", List.of("java"), 9L, 7, 5, 2,
                true, true, NOW);
        SearchHit deletedAuthor = new SearchHit(12, "标题二", "摘要二", List.of(), null, 8, 0, 0,
                false, false, NOW.plusSeconds(1));
        when(search.search(query)).thenReturn(new PageResult<>(List.of(known, deletedAuthor), 21, 2, 10));
        when(identity.findUsers(anyCollection())).thenReturn(Map.of(7L,
                new UserSummary(7, "小明", Role.STUDENT, true, "计算机学院")));

        PageResult<SearchResultView> result = service.search(query);

        assertThat(result.total()).isEqualTo(21);
        assertThat(result.page()).isEqualTo(2);
        assertThat(result.size()).isEqualTo(10);
        assertThat(result.items()).hasSize(2);
        assertThat(result.items().get(0))
                .extracting(SearchResultView::id, SearchResultView::excerpt, SearchResultView::bountyOpen)
                .containsExactly(11L, "摘要一", true);
        assertThat(result.items().get(0).author())
                .isEqualTo(new SearchResultView.Author(7, "小明", "STUDENT", true));
        assertThat(result.items().get(1).author())
                .isEqualTo(new SearchResultView.Author(8, "已注销用户", "STUDENT", false));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<java.util.Collection<Long>> ids = ArgumentCaptor.forClass(java.util.Collection.class);
        verify(identity).findUsers(ids.capture());
        assertThat(ids.getValue()).containsExactly(7L, 8L);
    }

    @Test
    void relatedRanksCandidatesAndMapsSimilarity() {
        when(index.findTags(10)).thenReturn(List.of("java", "并发"));
        when(index.findCandidatesSharingTags(10, List.of("java", "并发"), 2)).thenReturn(List.of(
                new RelatedCandidate(11, "低相关", 1, 4, 100, 5, true),
                new RelatedCandidate(12, "高相关", 2, 2, 1, 0, false),
                new RelatedCandidate(13, "中相关", 1, 2, 5, 3, true)
        ));

        List<RelatedQuestionView> result = service.related(10, 2);

        assertThat(result).extracting(RelatedQuestionView::id).containsExactly(12L, 13L);
        assertThat(result.get(0).similarity()).isEqualTo(1.0);
        assertThat(result.get(1).similarity()).isEqualTo(1.0 / 3.0);
        assertThat(result.get(1).answerCount()).isEqualTo(3);
        assertThat(result.get(1).accepted()).isTrue();
    }

    @Test
    void relatedSkipsCandidateQueryWithoutTagsOrPositiveLimit() {
        when(index.findTags(10)).thenReturn(List.of());

        assertThat(service.related(10, 5)).isEmpty();
        assertThat(service.related(10, 0)).isEmpty();

        verify(index, never()).findCandidatesSharingTags(org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.anyList(), org.mockito.ArgumentMatchers.anyInt());
    }
}

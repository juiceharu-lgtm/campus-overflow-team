package com.campusoverflow.discovery.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.campusoverflow.discovery.domain.search.SearchDocument;
import com.campusoverflow.discovery.domain.search.SearchIndex;
import com.campusoverflow.qa.api.event.AnswerAcceptedEvent;
import com.campusoverflow.qa.api.event.AnswerSubmittedEvent;
import com.campusoverflow.qa.api.event.ContentDeletedEvent;
import com.campusoverflow.qa.api.event.QuestionEditedEvent;
import com.campusoverflow.qa.api.event.QuestionPostedEvent;
import com.campusoverflow.qa.api.event.VoteCastEvent;
import com.campusoverflow.reputation.api.event.BountyAwardedEvent;
import com.campusoverflow.reputation.api.event.BountyClosedEvent;
import com.campusoverflow.reputation.api.event.BountyOpenedEvent;
import com.campusoverflow.shared.event.ProcessedEventStore;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SearchIndexProjectorTest {

    private static final Instant NOW = Instant.parse("2026-10-22T03:00:00Z");

    @Mock private SearchIndex index;
    @Mock private ProcessedEventStore processed;

    private SearchIndexProjector projector;

    @BeforeEach
    void setUp() {
        projector = new SearchIndexProjector(index, processed);
    }

    @Test
    void questionAndAnswerEventsBuildAndUpdateTheProjection() {
        allow("posted", "edited", "answered", "accepted");

        projector.on(new QuestionPostedEvent("posted", NOW, 10, 7, 9L, "原题", "原正文", List.of("java")));
        projector.on(new QuestionEditedEvent("edited", NOW.plusSeconds(1), 10, "新题", "新正文",
                List.of("java", "并发")));
        projector.on(new AnswerSubmittedEvent("answered", NOW.plusSeconds(2), 20, 10, 8, 7, "新题"));
        projector.on(new AnswerAcceptedEvent("accepted", NOW.plusSeconds(3), 20, 10, 8, 7, 7, "新题"));

        ArgumentCaptor<SearchDocument> document = ArgumentCaptor.forClass(SearchDocument.class);
        verify(index).insert(document.capture());
        assertThat(document.getValue()).isEqualTo(new SearchDocument(10, 9L, 7, "原题", "原正文",
                List.of("java"), NOW));
        verify(index).updateText(10, "新题", "新正文", List.of("java", "并发"), NOW.plusSeconds(1));
        verify(index).adjustAnswerCount(10, 1, NOW.plusSeconds(2));
        verify(index).markAccepted(10, NOW.plusSeconds(3));
    }

    @Test
    void deletionBranchesUpdateOnlySupportedTargetTypes() {
        allow("delete-question", "delete-answer", "delete-other");

        projector.on(new ContentDeletedEvent("delete-question", NOW, "QUESTION", 10, 10));
        projector.on(new ContentDeletedEvent("delete-answer", NOW.plusSeconds(1), "ANSWER", 20, 10));
        projector.on(new ContentDeletedEvent("delete-other", NOW.plusSeconds(2), "COMMENT", 30, 10));

        verify(index).markDeleted(10);
        verify(index).adjustAnswerCount(10, -1, NOW.plusSeconds(1));
        verify(index, times(1)).adjustAnswerCount(org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void votesUpdateQuestionScoreButIgnoreAnswerVotes() {
        allow("question-vote", "answer-vote");

        projector.on(new VoteCastEvent("question-vote", NOW, "QUESTION", 10, 7, 8, 10, 1, -1));
        projector.on(new VoteCastEvent("answer-vote", NOW.plusSeconds(1), "ANSWER", 20, 9, 8, 10, 0, 1));

        verify(index).adjustScore(10, -2);
        verify(index, times(1)).adjustScore(org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    void bountyEventsOpenAndCloseTheProjectionFlag() {
        allow("opened", "awarded", "closed");

        projector.on(new BountyOpenedEvent("opened", NOW, 30, 10, 1, 50, NOW.plusSeconds(86400)));
        projector.on(new BountyAwardedEvent("awarded", NOW.plusSeconds(1), 30, 10, 1, 2, 50));
        projector.on(new BountyClosedEvent("closed", NOW.plusSeconds(2), 31, 11, 1, 50));

        verify(index).setBountyOpen(10, true);
        verify(index).setBountyOpen(10, false);
        verify(index).setBountyOpen(11, false);
    }

    @Test
    void duplicateEventProducesNoProjectionSideEffect() {
        QuestionPostedEvent duplicate = new QuestionPostedEvent("duplicate", NOW, 10, 7, null, "题目", "正文",
                List.of("java"));
        when(processed.markProcessed("duplicate", SearchIndexProjector.CONSUMER)).thenReturn(false);

        projector.on(duplicate);

        verifyNoInteractions(index);
        verify(processed).markProcessed("duplicate", SearchIndexProjector.CONSUMER);
    }

    private void allow(String... eventIds) {
        for (String eventId : eventIds) {
            when(processed.markProcessed(eventId, SearchIndexProjector.CONSUMER)).thenReturn(true);
        }
    }
}

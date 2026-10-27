package com.campusoverflow.qa.infrastructure.persistence;

import com.campusoverflow.qa.domain.Answer;
import com.campusoverflow.qa.domain.AnswerRepository;
import com.campusoverflow.qa.domain.MarkdownBody;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class AnswerRepositoryAdapter implements AnswerRepository {

    private final AnswerJpaRepository jpa;

    public AnswerRepositoryAdapter(AnswerJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<Answer> findById(long id) {
        return jpa.findById(id).map(e -> Answer.reconstitute(e.getId(), e.getQuestionId(), e.getAuthorId(),
                new MarkdownBody(e.getBody()), e.getScore(), e.getEndorsedBy(), e.getCreatedAt(), e.getUpdatedAt(),
                e.getDeletedAt(), e.getVersion()));
    }

    @Override
    public Answer save(Answer a) {
        AnswerJpaEntity e = a.id() == null
                ? new AnswerJpaEntity(a.questionId(), a.authorId(), a.createdAt())
                : jpa.findById(a.id()).orElseThrow();
        e.setBody(a.body().value());
        e.setEndorsedBy(a.endorsedBy());
        e.setUpdatedAt(a.updatedAt());
        e.setDeletedAt(a.deletedAt());
        AnswerJpaEntity saved = jpa.save(e);
        // 删掉 a.assignId(saved.getId())，因为 Answer 的 id 是 final，不支持事后赋值
        return Answer.reconstitute(saved.getId(), saved.getQuestionId(), saved.getAuthorId(),
                new MarkdownBody(saved.getBody()), saved.getScore(), saved.getEndorsedBy(),
                saved.getCreatedAt(), saved.getUpdatedAt(), saved.getDeletedAt(), saved.getVersion());
    }

    @Override
    public void adjustScore(long answerId, int delta) {
        jpa.adjustScore(answerId, delta);
    }
}

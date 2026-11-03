package com.campusoverflow.qa.infra.persistence.jpa;

import com.campusoverflow.qa.domain.Question;
import com.campusoverflow.qa.domain.QuestionRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public class QuestionJpaAdapter implements QuestionRepository {

    private final QuestionJpaRepository jpaRepository;

    public QuestionJpaAdapter(QuestionJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<Question> findById(long id) {
        return jpaRepository.findById(id)
                .map(QuestionEntity::toDomain);
    }

    @Override
    @Transactional
    public Question save(Question question) {
        QuestionEntity entity = QuestionEntity.fromDomain(question);
        QuestionEntity saved = jpaRepository.save(entity);
        return saved.toDomain();
    }

    @Override
    @Transactional
    public void adjustAnswerCount(long questionId, int delta) {
        jpaRepository.adjustAnswerCount(questionId, delta);
    }

    @Override
    @Transactional
    public void adjustScore(long questionId, int delta) {
        jpaRepository.adjustScore(questionId, delta);
    }
}

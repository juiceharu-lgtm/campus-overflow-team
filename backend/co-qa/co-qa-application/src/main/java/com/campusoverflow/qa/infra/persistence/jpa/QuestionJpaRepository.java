package com.campusoverflow.qa.infra.persistence.jpa;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

public interface QuestionJpaRepository extends CrudRepository<QuestionEntity, Long> {

    @Modifying
    @Query("UPDATE QuestionEntity q SET q.answerCount = q.answerCount + :delta WHERE q.id = :id")
    void adjustAnswerCount(@Param("id") long id, @Param("delta") int delta);

    @Modifying
    @Query("UPDATE QuestionEntity q SET q.score = q.score + :delta WHERE q.id = :id")
    void adjustScore(@Param("id") long id, @Param("delta") int delta);
}

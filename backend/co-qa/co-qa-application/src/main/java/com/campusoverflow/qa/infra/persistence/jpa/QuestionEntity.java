package com.campusoverflow.qa.infra.persistence.jpa;

import com.campusoverflow.qa.domain.MarkdownBody;
import com.campusoverflow.qa.domain.Question;
import com.campusoverflow.qa.domain.Tags;
import com.campusoverflow.qa.domain.Title;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.List;

@Entity
@Table(name = "t_question")
public class QuestionEntity {

    @Id
    private Long id;
    private long authorId;
    private Long courseId;
    private String title;
    @Column(length = 4096)
    private String body;
    private String tags; // 逗号分隔标签
    private Long acceptedAnswerId;
    private int score;
    private int answerCount;
    private int viewCount;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant deletedAt;
    private long version;

    public static QuestionEntity fromDomain(Question question) {
        QuestionEntity e = new QuestionEntity();
        e.id = question.id();
        e.authorId = question.authorId();
        e.courseId = question.courseId();
        e.title = question.title().value();
        e.body = question.body().value();
        e.tags = String.join(",", question.tags().names());
        e.acceptedAnswerId = question.acceptedAnswerId();
        e.score = question.score();
        e.answerCount = question.answerCount();
        e.viewCount = question.viewCount();
        e.createdAt = question.createdAt();
        e.updatedAt = question.updatedAt();
        e.deletedAt = question.deletedAt();
        e.version = question.version();
        return e;
    }

    public Question toDomain() {
        List<String> tagList = (tags == null || tags.isBlank())
                ? List.of()
                : List.of(tags.split(","));

        return Question.reconstitute(
                id,
                authorId,
                courseId,
                new Title(title),
                MarkdownBody.ofQuestion(body),
                Tags.of(tagList),
                acceptedAnswerId,
                score,
                answerCount,
                viewCount,
                createdAt,
                updatedAt,
                deletedAt,
                version
        );
    }

    // getter setter
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public long getAuthorId() { return authorId; }
    public void setAuthorId(long authorId) { this.authorId = authorId; }
    public Long getCourseId() { return courseId; }
    public void setCourseId(Long courseId) { this.courseId = courseId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }
    public Long getAcceptedAnswerId() { return acceptedAnswerId; }
    public void setAcceptedAnswerId(Long acceptedAnswerId) { this.acceptedAnswerId = acceptedAnswerId; }
    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }
    public int getAnswerCount() { return answerCount; }
    public void setAnswerCount(int answerCount) { this.answerCount = answerCount; }
    public int getViewCount() { return viewCount; }
    public void setViewCount(int viewCount) { this.viewCount = viewCount; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public Instant getDeletedAt() { return deletedAt; }
    public void setDeletedAt(Instant deletedAt) { this.deletedAt = deletedAt; }
    public long getVersion() { return version; }
    public void setVersion(long version) { this.version = version; }
}

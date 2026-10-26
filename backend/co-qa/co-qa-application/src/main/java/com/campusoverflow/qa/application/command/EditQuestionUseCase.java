package com.campusoverflow.qa.application.command;

import com.campusoverflow.qa.domain.MarkdownBody;
import com.campusoverflow.qa.domain.Question;
import com.campusoverflow.qa.domain.QuestionRepository;
import com.campusoverflow.qa.domain.Tags;
import com.campusoverflow.qa.domain.Title;
import com.campusoverflow.shared.security.Actor;
import org.springframework.stereotype.Service;
import java.time.Instant;

@Service
public class EditQuestionUseCase {

    private final QuestionRepository questionRepository;

    public EditQuestionUseCase(QuestionRepository questionRepository) {
        this.questionRepository = questionRepository;
    }

    /**
     * @param actor 当前操作人
     * @param questionId 问题ID
     * @param actorManagesCourse 是否是课程管理者（教师/助教/管理员）
     * @param newTitle 新标题
     * @param newBody 新正文
     * @param newTags 新标签
     */
    public void execute(Actor actor, long questionId, boolean actorManagesCourse,
                       Title newTitle, MarkdownBody newBody, Tags newTags) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new com.campusoverflow.shared.domain.BusinessRuleException("QA-1020", "问题不存在"));

        Instant now = Instant.now();
        question.edit(actor, actorManagesCourse, newTitle, newBody, newTags, now);
        questionRepository.save(question);
    }
}
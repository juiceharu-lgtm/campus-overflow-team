package com.campusoverflow.qa.domain;

import com.campusoverflow.shared.domain.BusinessRuleException;
import com.campusoverflow.shared.domain.Guard;

public record MarkdownBody(String value) {

    /**
     * 问题正文工厂：问题正文长度规则
     */
    public static MarkdownBody ofQuestion(String raw) {
        String checked = Guard.requireText(raw, 10, 2000, "QA-MARKDOWN-LEN", "问题正文");
        Guard.requireNonNull(checked, "QA-MARKDOWN-001", "markdown内容");
        return new MarkdownBody(checked);
    }

    /**
     * 回答正文工厂：回答正文长度规则
     */
    public static MarkdownBody ofAnswer(String raw) {
        String checked = Guard.requireText(raw, 5, 5000, "QA-MARKDOWN-LEN", "回答正文");
        Guard.requireNonNull(checked, "QA-MARKDOWN-001", "markdown内容");
        return new MarkdownBody(checked);
    }
}

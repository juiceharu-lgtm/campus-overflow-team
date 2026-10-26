package com.campusoverflow.qa.domain;
import com.campusoverflow.shared.domain.BusinessRuleException;
import com.campusoverflow.shared.domain.Guard;
public record MarkdownBody(String value) {
    public MarkdownBody {
        // 非空校验
        value = Guard.requireNonNull(value, "QA-MARKDOWN-001", "markdown内容");
        // 长度校验，按需调整范围，这里设置0~2000
        value = Guard.requireText(value, 0, 2000, "QA-MARKDOWN-LEN", "markdown内容");
    }

    // TODO 补齐：问题正文工厂
    public static MarkdownBody ofQuestion(String raw) {
        return new MarkdownBody(raw);
    }
    // TODO 补齐：回答正文工厂（可以单独控制回答最大长度）
    public static MarkdownBody ofAnswer(String raw) {
        return new MarkdownBody(raw);
    }
}
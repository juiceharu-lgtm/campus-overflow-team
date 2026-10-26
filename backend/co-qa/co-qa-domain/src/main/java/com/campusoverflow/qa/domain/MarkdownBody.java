package com.campusoverflow.qa.domain;
import com.campusoverflow.shared.domain.Guard;
import com.campusoverflow.shared.domain.BusinessRuleException;

/** Markdown 正文。只保存原文；渲染与 XSS 净化在基础设施层完成。 */
public record MarkdownBody(String value) {

    private static final int QUESTION_MIN_LEN = 20;
    private static final int ANSWER_MIN_LEN = 10;
    private static final int MAX_LEN = 30000;

    public static MarkdownBody ofQuestion(String raw) {
        Guard.notNull(raw, "正文不能为空");
        if(raw.length() < QUESTION_MIN_LEN || raw.length() > MAX_LEN){
            throw new BusinessRuleException("QA‑1002",
                    String.format("问题正文长度必须在 %d‑%d 字符之间", QUESTION_MIN_LEN, MAX_LEN));
        }
        return new MarkdownBody(raw);
    }

    public static MarkdownBody ofAnswer(String raw) {
        Guard.notNull(raw, "正文不能为空");
        if(raw.length() < ANSWER_MIN_LEN || raw.length() > MAX_LEN){
            throw new BusinessRuleException("QA‑1002",
                    String.format("回答正文长度必须在 %d‑%d 字符之间", ANSWER_MIN_LEN, MAX_LEN));
        }
        return new MarkdownBody(raw);
    }
}

package com.campusoverflow.qa.application.query;

import com.campusoverflow.identity.api.IdentityApi;
import com.campusoverflow.identity.api.UserSummary;
import com.campusoverflow.qa.application.port.ContentRenderer;
import com.campusoverflow.qa.application.port.QaReadModel;
import com.campusoverflow.qa.application.port.QuestionListCache;
import com.campusoverflow.shared.domain.NotFoundException;
import com.campusoverflow.shared.page.PageRequest;
import com.campusoverflow.shared.page.PageResult;
import com.campusoverflow.shared.security.Actor;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class QuestionQueryService {

    private static final int EXCERPT_LENGTH = 160;

    private final QaReadModel readModel;
    private final IdentityApi identity;
    private final ContentRenderer renderer;
    private final QuestionListCache cache;

    public QuestionQueryService(QaReadModel readModel,
                                IdentityApi identity,
                                ContentRenderer renderer,
                                QuestionListCache cache) {
        this.readModel = readModel;
        this.identity = identity;
        this.renderer = renderer;
        this.cache = cache;
    }

    /**
     * 问题列表查询（完整 Cache-Aside：先读缓存，缺失再查DB，结果回写缓存）
     */
    public PageResult<QuestionSummaryView> list(Long courseId, PageRequest page) {
        // 第一步：尝试读取缓存
        Optional<PageResult<QuestionSummaryView>> cachedResult = cache.get(courseId, page);
        if (cachedResult.isPresent()) {
            return cachedResult.get();
        }

        // 第二步：缓存未命中，查询读模型
        PageResult<QuestionRow> rowPage = readModel.listQuestions(courseId, page);
        if (rowPage.items().isEmpty()) {
            PageResult<QuestionSummaryView> emptyResult = PageResult.empty(page.page(), page.size());
            cache.put(courseId, page, emptyResult);
            return emptyResult;
        }

        Set<Long> authorIds = rowPage.items().stream()
                .map(QuestionRow::authorId)
                .collect(Collectors.toSet());
        Map<Long, UserSummary> userMap = identity.findUsers(authorIds);

        PageResult<QuestionSummaryView> result = rowPage.map(row -> {
            UserSummary authorUs = userMap.get(row.authorId());
            AuthorView author = AuthorView.of(row.authorId(), authorUs);
            String excerpt = renderer.toPlainText(row.body(), EXCERPT_LENGTH);
            boolean accepted = row.acceptedAnswerId() != null;
            return new QuestionSummaryView(
                    row.id(),
                    row.title(),
                    excerpt,
                    row.tags(),
                    row.courseId(),
                    author,
                    row.score(),
                    row.answerCount(),
                    row.viewCount(),
                    accepted,
                    row.createdAt()
            );
        });

        // 第三步：查询结果写入缓存
        cache.put(courseId, page, result);
        return result;
    }

    /**
     * 问题详情查询，携带当前访问者 Actor，用于计算权限
     * @param questionId 问题id
     * @param actor 当前访问主体（可为null代表匿名）
     */
    public QuestionDetailView detail(long questionId, Actor actor) {
        QuestionRow qRow = readModel.findQuestion(questionId)
                .orElseThrow(() -> new NotFoundException("QA-4001", "该问题不存在或已删除"));

        List<AnswerRow> answerRows = readModel.findAnswers(questionId);
        List<CommentRow> commentRows = readModel.findComments(questionId);

        final Long voterId = actor == null ? null : actor.userId();
        final Map<String, Integer> voteMap;
        if (voterId != null) {
            List<Long> ansIds = answerRows.stream().map(AnswerRow::id).collect(Collectors.toList());
            voteMap = readModel.findMyVotes(voterId, questionId, ansIds);
        } else {
            voteMap = Map.of();
        }
        int myVote = voteMap.getOrDefault("QUESTION:" + questionId, 0);

        // 批量收集所有需要查用户信息的id
        Set<Long> allUserIds = commentRows.stream().map(CommentRow::authorId).collect(Collectors.toSet());
        allUserIds.add(qRow.authorId());
        answerRows.forEach(ar -> {
            allUserIds.add(ar.authorId());
            if (ar.endorsedBy() != null) {
                allUserIds.add(ar.endorsedBy());
            }
        });
        Map<Long, UserSummary> userMap = identity.findUsers(allUserIds);

        // 组装回答 AnswerView
        List<AnswerView> answerViews = answerRows.stream()
                .map(ar -> {
                    UserSummary ansUs = userMap.get(ar.authorId());
                    AuthorView ansAuthor = AuthorView.of(ar.authorId(), ansUs);

                    AuthorView endorsedByView = null;
                    if (ar.endorsedBy() != null) {
                        UserSummary endorseUs = userMap.get(ar.endorsedBy());
                        endorsedByView = AuthorView.of(ar.endorsedBy(), endorseUs);
                    }
                    int ansVote = voteMap.getOrDefault("ANSWER:" + ar.id(), 0);
                    boolean accepted = qRow.acceptedAnswerId() != null && qRow.acceptedAnswerId().equals(ar.id());
                    // 回答编辑权限：本人 或者 老师/管理员
                    boolean answerCanEdit = actor != null && (actor.is(ar.authorId()) || actor.isTeacherOrAdmin());
                    return new AnswerView(
                            ar.id(),
                            ar.body(),
                            renderer.toSafeHtml(ar.body()),
                            ansAuthor,
                            ar.score(),
                            accepted,
                            endorsedByView,
                            ansVote,
                            answerCanEdit,
                            ar.createdAt(),
                            ar.updatedAt(),
                            List.of()
                    );
                })
                .collect(Collectors.toList());

        // 组装评论 CommentView
        List<CommentView> commentViews = commentRows.stream()
                .map(cr -> {
                    UserSummary cmtUs = userMap.get(cr.authorId());
                    AuthorView cmtAuthor = AuthorView.of(cr.authorId(), cmtUs);
                    return new CommentView(cr.id(), cmtAuthor, cr.parentId(), cr.body(), cr.createdAt());
                })
                .collect(Collectors.toList());

        // ========= 计算 PermissionsView 权限 =========
        boolean isOwner = actor != null && actor.is(qRow.authorId());
        boolean isTeacherOrAdmin = actor != null && actor.isTeacherOrAdmin();
        boolean canEdit = isOwner || isTeacherOrAdmin;
        boolean canDelete = isOwner || isTeacherOrAdmin;
        boolean canAccept = isOwner; // 只有提问者可以采纳答案
        boolean canManage = isTeacherOrAdmin;
        PermissionsView permissions = new PermissionsView(canEdit, canDelete, canAccept, canManage);

        // 课程视图：QaReadModel没有课程查询能力，置为null
        CourseRefView course = null;

        return new QuestionDetailView(
                qRow.id(),
                qRow.title(),
                qRow.body(),
                renderer.toSafeHtml(qRow.body()),
                qRow.tags(),
                course,
                AuthorView.of(qRow.authorId(), userMap.get(qRow.authorId())),
                qRow.score(),
                qRow.answerCount(),
                qRow.viewCount(),
                qRow.acceptedAnswerId(),
                myVote,
                qRow.createdAt(),
                qRow.updatedAt(),
                permissions,
                commentViews,
                answerViews
        );
    }

    /** 热门标签 */
    public List<TagView> popularTags(int limit) {
        return readModel.popularTags(limit);
    }
}

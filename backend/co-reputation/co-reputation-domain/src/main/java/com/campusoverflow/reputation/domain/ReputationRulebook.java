package com.campusoverflow.reputation.domain;

import com.campusoverflow.shared.security.Role;

/**
 * 声誉规则手册——把“游戏规则”集中在一处，便于课堂讨论与参数调整。
 * <ul>
 *   <li>回答被采纳 +15（自问自答被采纳不加分）；</li>
 *   <li>被赞 +10，被踩 −2；改投、撤销按“新效果 − 旧效果”计算增量；</li>
 *   <li>反作弊：每人每个自然日（UTC）因被投票获得的正向声誉最多 200。</li>
 * </ul>
 */
public final class ReputationRulebook {

    public static final int ACCEPTED = 15;
    public static final int UPVOTE = 10;
    public static final int DOWNVOTE = -2;
    public static final int DAILY_VOTE_CAP = 200;

    // 新增处理器需要用到的常量
    public static final int REGISTER_INIT = 10;
    public static final int ANSWER_SUBMIT = 5;
    public static final int ENDORSE = 3;

    private ReputationRulebook() {
    }

    public static int voteEffect(int value) {
        return switch (value) {
            case 1 -> UPVOTE;
            case -1 -> DOWNVOTE;
            case 0 -> 0; // 撤销投票
            default -> 0;
        };
    }

    public static int voteDelta(int oldValue, int newValue) {
        return voteEffect(newValue) - voteEffect(oldValue);
    }

    public static int acceptedDelta(long answerAuthorId, long questionAuthorId) {
        return answerAuthorId == questionAuthorId ? 0 : ACCEPTED;
    }

    /** 对正向投票增量应用每日上限；负向增量不受限。 */
    public static int applyDailyCap(int delta, int positiveVoteGainToday) {
        // 负向分值，直接返回，不做上限拦截
        if (delta <= 0) {
            return delta;
        }
        int remaining = DAILY_VOTE_CAP - positiveVoteGainToday;
        return Math.min(delta, Math.max(remaining, 0));
    }

    /** 开户时发放的悬赏积分（与声誉是两种不同的“货币”）。 */
    public static int initialPoints(Role role) {
        if (role == Role.TEACHER) {
            return 1000;
        }
        // 学生、其他角色默认0悬赏积分
        return 0;
    }
}

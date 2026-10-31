# Q&A问答上下文｜事件风暴笔记
> 参与者、命令、领域事件、聚合、外部依赖

## 参与者（Actor）
1. 普通学生(STUDENT)：提问、回答、投票、评论、编辑自己的内容、删除无回答的自己的问题
2. 提问者：可以采纳自己问题下的最佳回答
3. 课程教师/助教(TEACHER/TA)：课程管理者；编辑、删除任意内容；教师认证回答；可代为采纳答案
4. 管理员(ADMIN)：平台管理员，可以管理全部内容

## 命令（Command｜对应应用服务入口）
|命令|触发来源|应用服务方法|
|---|---|---|
|发布问题|学生|QuestionCommandService#post|
|编辑问题|作者/课程管理者|QuestionCommandService#edit|
|软删除问题|作者/课程管理者|QuestionCommandService#delete|
|提交回答|学生|AnswerCommandService#submit|
|编辑回答|作者/课程管理者|AnswerCommandService#edit|
|软删除回答|作者/课程管理者|AnswerCommandService#delete|
|采纳回答|提问者/课程管理者|AnswerCommandService#accept|
|教师认证回答|课程教师/助教|AnswerCommandService#endorse|
|投票/改投/撤销投票|登录用户|VoteService#cast|
|发布评论/回复评论|登录用户|CommentService#post|

## 领域事件（IntegrationEvent，输出到outbox_event发件箱）
|事件|触发时机|订阅方|
|---|---|---|
|QuestionPostedEvent|发布新问题|Discovery（搜索投影）|
|QuestionEditedEvent|编辑问题|Discovery（更新搜索投影）|
|AnswerSubmittedEvent|提交回答|Reputation、Discovery（通知提问者）|
|AnswerEditedEvent|编辑回答|Discovery|
|AnswerAcceptedEvent|采纳最佳回答|Reputation（声誉+徽章）、Discovery（标记已采纳、通知回答者）|
|AnswerEndorsedEvent|教师认证回答|Reputation（徽章）、Discovery（通知回答者）|
|VoteCastEvent|投票/改投/撤销投票|Reputation（计分）、Discovery（同步得分）|
|CommentPostedEvent|新增评论|Discovery（解析@提及、通知）|
|ContentDeletedEvent|问题/回答软删除|Discovery（投影标记删除）|

## 外部依赖（Q&A向外调用API）
- `IdentityApi`：查询用户摘要UserSummary；判断用户是否课程管理者`canManageCourse(userId,courseId)`

## 聚合分配（命令 → 操作哪个聚合根）
1. post/edit/delete Question → Question
2. submit/edit/delete/accept/endorse Answer → Answer / Question
3. cast Vote → Vote
4. post Comment → Comment

## 核心业务不变量汇总（来自事件风暴）
1. 问题被采纳后不能二次采纳；
2. 作者不能删除已经拥有回答的问题；
3. 被采纳的回答，作者本人不能删除；
4. 回答只允许教师认证一次；
5. 不能给自己的内容投票；
6. 评论回复仅支持一层嵌套回复。

## 边界
Q&A只负责问答业务；**声誉计算、通知、搜索投影，属于Reputation、Discovery上下文，通过事件异步消费，不属于Q&A领域内业务逻辑**。

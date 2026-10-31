# Q&A 问答上下文｜统一语言 Ubiquitous Language
> 业务术语与代码映射；团队沟通、Issue、PR、代码全部使用本套词汇，禁止口语别名

|业务术语|代码实体|业务定义与规则|
|---|---|---|
|问题|`Question`|学生发布求助；拥有标题、Markdown正文、标签，可归属课程；可以被回答、采纳、软删除|
|标题|`Title`|问题标题；长度约束10‑150字符，不能为空|
|问题正文|`MarkdownBody.ofQuestion()`|Markdown格式正文；20‑30000字符|
|回答|`Answer`|针对某个问题的解答，隶属于一个questionId；独立聚合根，不属于Question内部实体|
|回答正文|`MarkdownBody.ofAnswer()`|回答Markdown正文；10‑30000字符|
|标签|`TagName / Tags`|标签，自动转为小写，去除#；一题1‑5个，自动去重|
|采纳最佳答案|`question.accept()`|提问者或者课程管理者标记某回答为本问题最佳答案；一题最多一次，不可修改|
|教师认证|`answer.endorse()`|课程教师/助教给优质回答打上权威标记；一个回答最多认证一次，存入`endorsedBy`用户ID|
|软删除|`softDelete()`|不物理删除；设置`deletedAt`时间戳；列表、详情查询过滤已删除内容；删除操作写审计记录|
|投票|`Vote`|对问题或回答投票；值`+1`赞成、`‑1`反对；撤销投票业务上传值0；禁止给自己内容投票；一个用户对一个目标只能一票|
|评论|`Comment`|挂载到问题/回答下面的文字评论；支持一层回复；回复不能再嵌套回复；正文2‑600字符|
|父评论|`parentId`|评论的父评论ID；仅用于回复，回复只能指向顶层评论|
|课程管理者|`identity.canManageCourse()`|管理员、该课程教师/助教；拥有更高编辑、删除、采纳、认证权限|
|计数器|score、answerCount、viewCount|聚合根内部只读；**不允许聚合内部set修改**；全部由仓储`adjustXXX()`原子SQL维护|
|集成事件|*Event record|Q&A产生的跨上下文事件，存入outbox_event事务发件箱，供Reputation、Discovery消费|
|审计记录|AuditTrail|所有写操作记录审计，记录操作者id、操作类型、目标实体id|
|缓存失效|QuestionListCache|问题列表缓存；任何问题/回答写操作执行`invalidateAll()`清理缓存|

## 错误码对照表（Q&A）
|错误码|异常类型|含义|
|---|---|---|
|QA‑1001|BusinessRuleException|标题不合法|
|QA‑1002|BusinessRuleException|正文长度不合法|
|QA‑1004|BusinessRuleException|标签格式非法|
|QA‑1005|BusinessRuleException|标签数量必须1‑5个|
|QA‑1012|BusinessRuleException|被采纳的回答，作者不能删除|
|QA‑1013|BusinessRuleException|问题已有回答，提问者不能删除|
|QA‑1014|BusinessRuleException|投票值非法，只能‑1 / +1|
|QA‑1015|BusinessRuleException|不能给自己的内容投票|
|QA‑1016|BusinessRuleException|回复不允许多层嵌套|
|QA‑1017|BusinessRuleException|回复评论targetType/targetId不匹配父评论|
|QA‑1020|BusinessRuleException|资源已删除，禁止操作|
|QA‑2002|ConflictException|问题已经采纳过答案，不可重复采纳|
|QA‑2003|ConflictException|该回答已经被教师认证，不可重复认证|
|QA‑3001|ForbiddenException|无权采纳答案|
|QA‑3002|ForbiddenException|无权编辑|
|QA‑3003|ForbiddenException|无权删除|
|QA‑3004|ForbiddenException|无权执行教师认证|
|QA‑4001|NotFoundException|问题不存在或已删除|
|QA‑4002|NotFoundException|回答不存在或已删除|
|QA‑4003|NotFoundException|评论不存在或已删除|

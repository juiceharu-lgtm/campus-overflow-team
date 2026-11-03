# Q&A 问答上下文 — 聚合与值对象清单
> 对应：Sprint1 Q&A领域建模，来源事件风暴产出，DDD战术设计

## 聚合根列表
### 1. Question 问题聚合根
**职责**：封装问题全部业务规则；维护标签、采纳状态、删除标记；保护内部不变量。
- 字段：`id`、`authorId`、`courseId`、`title(Title)`、`body(MarkdownBody)`、`tags(Tags)`、`acceptedAnswerId`、`deletedAt`、`createdAt`、`updatedAt`、`version`
- 工厂方法：`post()` 创建新问题，校验值对象，初始化计数器、未采纳、未删除
- 领域行为：
  - `edit()`：编辑问题；校验：**未删除；本人或课程管理者才允许编辑**
  - `accept()`：采纳最佳答案；不变量：
    1. 问题未删除
    2. 操作者为提问者 或者 课程管理者
    3. 问题**只能被采纳一次，不可重复采纳**
    4. 被采纳回答必须属于本问题，且回答未软删除
  - `softDelete()`：软删除问题；不变量：
    1. 普通作者**仅当没有任何回答才可以删除自己的问题**
    2. 课程管理者/管理员可以无条件删除
  - `ensureNotDeleted()`：已删除抛出业务异常 `QA‑1020`
  - `ensureOpenForAnswers()`：已删除不允许提交回答

**业务不变量汇总 Question**
1. 标题长度 10‑150字符；
2. 问题正文Markdown：20‑30000字符；
3. 标签数量 1‑5个，自动规范化小写，去重；
4. 一题最多一个被采纳答案，采纳后不能二次采纳；
5. 已删除问题禁止编辑、回答、采纳。

### 2. Answer 回答聚合根
**职责**：回答内容、教师认证标记、软删除状态
- 字段：`id`、`questionId`、`authorId`、`body(MarkdownBody)`、`score`、`endorsedBy`、`deletedAt`、`createdAt`、`updatedAt`、`version`
- 工厂：`submit()`，依赖外部调用 `question.ensureOpenForAnswers()`
- 领域行为
  - `edit()`：编辑回答；未删除；作者本人 / 课程管理者有权限
  - `endorse()`：教师认证回答；不变量：**未删除；课程管理者；不能重复认证**
  - `softDelete(boolean isAcceptedAnswer)`：软删除回答
    - 作者：**如果本回答是已采纳回答，作者不能删除**；管理者不受该限制
    - 课程管理者可以直接删除
  - `ensureNotDeleted()`：已删除抛出异常

**业务不变量 Answer**
1. 回答正文 Markdown：10‑30000字符；
2. 同一个回答只能被教师认证一次；
3. 被采纳的回答，普通作者不能删除。

### 3. Vote 投票聚合根
**职责**：对问题/回答进行投票，每人单条内容只能投一票
- 字段：`id`、`targetType`（QUESTION / ANSWER）、`targetId`、`voterId`、`value ∈ {-1, +1}`、`updatedAt`
- 工厂行为 `cast()`；变更 `change()`
**不变量**
1. 禁止给自己发布的问题/回答投票 `QA‑1015`；
2. 投票值只能为 +1 / -1，撤销投票外部置0；
3. 同一个用户对同一个目标只能存在一条投票记录。

### 4. Comment 评论聚合根
**职责**：问题或回答下评论，支持一层回复，不允许多层嵌套回复
- 字段：`id`、`targetType`、`targetId`、`questionId`、`parentId`、`authorId`、`body`、`createdAt`
- 工厂：`post()`
**不变量**
1. 评论正文长度2‑600字符；
2. 回复仅允许一层：`parentId` 指向顶层评论，**回复的parent不能再是回复**；
3. 回复评论必须挂载到同一个targetType、targetId。

## 值对象（无id，自包含校验逻辑）
1. `Title`：标题，构造器校验长度 10‑150
2. `MarkdownBody`：`ofQuestion()`、`ofAnswer()` 两套静态工厂，区分问题/回答正文长度约束
3. `TagName`：单个标签，规范化，去除#号、小写
4. `Tags`：标签集合；去重，数量1‑5；提供`names()`返回字符串列表

## 仓储接口（domain包内，仅定义接口，实现在infrastructure）
1. QuestionRepository
2. AnswerRepository
3. VoteRepository
4. CommentRepository
5. TagRepository

> 计数器（score / answerCount/viewCount）遵循ADR‑008：聚合内部只读；全部使用仓储`adjustXXX()`原子SQL更新，聚合不做set修改计数器。

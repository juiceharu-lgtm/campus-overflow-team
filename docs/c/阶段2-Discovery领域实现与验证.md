# C 成员阶段 2：Discovery 领域实现与验证

> 日期：2026-10-22（Asia/Shanghai）
>
> 分支：`feature/c-discovery`
>
> 范围：配置当前会话 JDK 21，完成 Discovery 领域层 7 个 TODO，补充并运行领域单元测试。

## 1. 开始前检查

- 已阅读适用目录，没有发现本仓库适用的 `AGENTS.md`。
- 已阅读 `docs/c/阶段1-环境与任务梳理.md`。
- 开始时分支为 `feature/c-discovery`，HEAD 为 PR #1 合并提交 `6697eed3564568bc34b1d98c7b42ad5f49c8382b`。
- 开始时唯一工作树变更为未跟踪文件 `docs/c/阶段1-环境与任务梳理.md`；该文件已保留。
- 没有 reset、checkout 覆盖、clean 或其他破坏已有改动的操作。

## 2. JDK 21 配置

### 2.1 实际来源

定向检查了 `JAVA_HOME`、命令来源、常见 JDK 安装目录以及仓库 IDE 配置，没有进行全盘递归扫描。在以下位置找到电脑已有的 Oracle JDK：

```text
C:\Program Files\Java\jdk-21.0.11
```

因此没有下载或安装 JDK，没有卸载原有 Temurin JDK 17，也没有修改系统级环境变量、IDE 配置或仓库 Java 版本要求。

### 2.2 当前执行会话配置

每次需要 Java/Maven 的 PowerShell 执行会话均临时设置：

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-21.0.11'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
```

实测结果：

```text
java version "21.0.11" 2026-04-21 LTS
javac 21.0.11
Apache Maven 3.9.16
Java version: 21.0.11, vendor: Oracle Corporation
runtime: C:\Program Files\Java\jdk-21.0.11
```

这些变量只对相应执行进程生效；新终端若要复现，需要再次设置。未安装 npm 或 Docker，未启动数据库或部署服务。

## 3. 领域实现

本阶段完成 4 个领域文件中的全部 7 个 TODO，未修改公开接口签名或共享契约，未引入 Spring/JPA 依赖。

### 3.1 SearchQuery：2 个 TODO

- `terms()` 委托给内部词项解析。
- `terms(String)` 对 `null`/空白返回空列表，按一个或多个空白切分，最多保留前 5 个词项。
- 现有构造器继续负责全文运算符清理、50 个 Unicode 码点上限、单词至少 2 个码点、标签规范化和默认排序。
- 规范只要求“切分为词项，最多 5 个”，未要求去重，所以重复词保持原顺序，也计入 5 个上限。

### 3.2 MentionParser：1 个 TODO

- `null`、空白和无提及文本返回空的有序集合。
- 使用骨架已有正则解析 2–20 字符昵称。
- 使用 `LinkedHashSet` 按首次出现顺序去重。
- 最多返回 5 个不同昵称。
- 延续已有正则定义的中英文标点、空白和 `@` 边界，不扩大昵称语法。

### 3.3 RelatedQuestionPolicy：2 个 TODO

- `jaccard` 按 `shared / (sizeA + sizeB - shared)` 计算；空并集返回 `0.0`，避免除零。
- `rank` 对空候选、非正自身标签数、非正 limit 返回空列表。
- 过滤 Jaccard 相似度不为正的候选。
- 排序依次为：相似度降序、得分降序、已采纳优先；完全同分时以问题 ID 升序作为稳定兜底。
- 最后按 `limit` 截断，返回不可修改的流结果列表。

### 3.4 Notification：2 个 TODO

- `create` 创建无 ID、未读的通知，完整保留收件人、类型、标题、可空链接、来源事件 ID 和创建时间。
- 按数据库非空约束，类型、标题、来源事件 ID、创建时间不能为空；链接允许为 `null`。
- `markRead` 仅允许 `recipientId == actorId`，否则抛 `ForbiddenException`。
- 越权错误码使用 Discovery 权限段 `DIS-3001`；本人重复标记保持幂等。

## 4. 单元测试补充

保留了原有 5 个测试方法和全部原断言，新增 8 个测试方法：

1. `searchQuerySplitsWhitespaceKeepsDuplicatesAndLimitsTerms`
   - 混合空格、Tab、换行分词；重复词顺序；最多 5 词。
2. `searchQueryReturnsNoTermsWithoutAUsableKeyword`
   - `null` 关键词和仅含全文运算符的输入。
3. `mentionsRespectPunctuationAndReturnEmptyForNoMentionText`
   - 中英文标点、重复提及、普通文本、空白与 `null`。
4. `jaccardHandlesEmptySetsAndRankingFiltersAndLimits`
   - 空集合相似度、零交集过滤、数量限制、空候选和非正 limit。
5. `relatedRankingUsesDocumentedTieBreakersDeterministically`
   - 相似度相同时的得分、采纳状态和 ID 稳定排序。
6. `notificationCreationStartsUnreadAndKeepsItsData`
   - 新通知未读及字段映射；可空链接。
7. `notificationRequiresMandatoryData`
   - 类型、标题、来源事件 ID、创建时间的非空约束。
8. `markingNotificationReadIsIdempotentAndUsesDiscoveryErrorCode`
   - `DIS-3001` 越权错误码和本人重复标记幂等。

修改后的 `DiscoveryDomainTest` 共 13 个测试方法。

## 5. 实际验证

### 5.1 指定模块测试

从 `backend/` 执行：

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-21.0.11'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
mvn -pl co-discovery/co-discovery-domain -am test
```

第一次在受限沙箱内运行时，Maven 无法写入 `C:\Users\思源\.m2` 的 `.lastUpdated` 文件，在项目解析阶段因 `AccessDeniedException` 退出；这不是编译或测试失败。取得相应文件/网络权限后以完全相同的 Maven 参数重跑成功。清理一处新增测试使用的 AssertJ 弃用重载后又复跑一次，最终结果：

```text
DiscoveryDomainTest: Tests run: 13, Failures: 0, Errors: 0, Skipped: 0
MaskingTest（-am 带入的 shared-kernel）: Tests run: 2, Failures: 0, Errors: 0, Skipped: 0
Reactor: 4/4 modules SUCCESS
BUILD SUCCESS
```

因此：Discovery 领域测试 **13/13 通过**；本次 Reactor 实际执行的测试合计 **15/15 通过**。

没有运行 Discovery 应用层测试、完整后端测试、数据库集成测试、Docker 部署或端到端测试。

## 6. 尚存问题与业务歧义

- `docs/错误码.md` 只给出 Discovery 的分段规则和 `DIS-1001` 示例，没有逐项列出通知越权错误码。本实现按权限段采用 `DIS-3001`；在 API 契约冻结前应由小组确认并补入错误码文档。
- 规范没有要求搜索词项去重，本实现保留重复词。如果产品希望减少重复 Boolean Fulltext 条件，应先补充明确规则和契约测试，再改变行为。
- 相关问题规则只明确了相似度、得分、采纳状态；为保证完全同分时结果确定，本实现用问题 ID 升序兜底。
- 当前会话已经能使用 JDK 21；npm、Docker/Compose 仍不可用，但不影响本阶段纯领域测试。
- Discovery 应用层仍有 25 个原始 TODO，本阶段按范围没有实现。

## 7. 修改范围

本阶段修改：

- `backend/co-discovery/co-discovery-domain/src/main/java/com/campusoverflow/discovery/domain/search/SearchQuery.java`
- `backend/co-discovery/co-discovery-domain/src/main/java/com/campusoverflow/discovery/domain/notification/MentionParser.java`
- `backend/co-discovery/co-discovery-domain/src/main/java/com/campusoverflow/discovery/domain/search/RelatedQuestionPolicy.java`
- `backend/co-discovery/co-discovery-domain/src/main/java/com/campusoverflow/discovery/domain/notification/Notification.java`
- `backend/co-discovery/co-discovery-domain/src/test/java/com/campusoverflow/discovery/domain/DiscoveryDomainTest.java`
- `docs/c/阶段2-Discovery领域实现与验证.md`

保留的上一阶段未跟踪文件：

- `docs/c/阶段1-环境与任务梳理.md`

没有修改 Discovery 应用层、Q&A、Reputation、Identity、基础设施、POM、CI 或覆盖率门槛。本阶段没有提交、推送、创建 PR 或合并 `main`。

## 8. 自查结果

- `git diff --check`：通过，退出码 0。
- Discovery 领域模块中搜索 `TODO(S3)` 和 `UnsupportedOperationException`：无残留。
- 已跟踪文件的修改范围仅为 4 个 Discovery 领域实现文件和 1 个领域测试文件。
- 未跟踪文件仅为阶段 1、阶段 2 两份 C 成员文档。

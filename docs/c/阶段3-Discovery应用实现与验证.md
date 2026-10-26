# C 成员阶段 3：Discovery 应用实现与验证

> 日期：2026-10-22（Asia/Shanghai）
>
> 分支：`feature/c-discovery`
>
> 范围：完成 Discovery 应用层 25 个 TODO，补充端口级单元测试，验证完整 Discovery 模块与既有 ArchUnit 规则。

## 1. 开始前检查与环境

- 已阅读适用目录；仓库内没有发现适用的 `AGENTS.md`。
- 已阅读 `docs/c/阶段1-环境与任务梳理.md`、`docs/c/阶段2-Discovery领域实现与验证.md`，并保留阶段 1、阶段 2 的文档、领域实现和领域测试。
- 开始及结束时均位于 `feature/c-discovery`，没有 reset、clean、覆盖已有改动、提交、推送、创建 PR 或合并 `main`。
- Java/Maven 命令均在对应 PowerShell 进程内临时设置：

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-21.0.11'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
```

`mvn -v` 实测使用 Oracle JDK `21.0.11`，运行时目录为 `C:\Program Files\Java\jdk-21.0.11`。没有修改系统环境变量，没有启动 Docker、数据库或部署服务。

## 2. 完成的应用层方法

四个应用类原有的 25 个待实现方法已全部完成；应用层和领域层主代码均无 `TODO(S3)`、`UnsupportedOperationException` 或占位 `return null` 残留。

### 2.1 DiscoveryQueryService：2 个方法

- `search`：保持 `SearchPort` 对查询参数、过滤、排序和分页的既有处理；对当前页作者 ID 去重后一次调用 `IdentityApi.findUsers`；映射时完整保留分页元数据和搜索命中字段。身份记录缺失时按“已注销用户”、`STUDENT`、未认证降级，不跨上下文读取 Identity 仓储。
- `related`：非正 limit 或当前问题无标签时直接返回空列表；通过 `SearchIndex` 读取候选，再交给领域层 `RelatedQuestionPolicy` 过滤、确定性排序、截断并计算 Jaccard 相似度。

### 2.2 NotificationService：5 个方法

- 列表、未读数和全部已读都只使用当前 `Actor.userId()`，不接受客户端传入其他收件人。
- 单条已读先按 ID 查找；不存在抛 `NotFoundException("DIS-4001", "通知不存在")`，存在时由领域对象执行收件人归属校验并保存。
- 视图映射保留 ID、类型、标题、链接、已读状态和创建时间。

### 2.3 NotificationProjector：8 个方法

- 回答提交、回答采纳、回答认证分别通知问题作者或回答作者，并排除由收件人本人触发的自通知。
- 评论事件先经 `QaQueryApi.findQuestion` 验证问题存在且未删除，再用 `MentionParser` 提取去重后的昵称、经 `IdentityApi.findByDisplayName` 解析；未知昵称和评论作者本人不生成通知。
- 悬赏发放通知获奖者并排除发起人与获奖者相同的情况；徽章发放通知徽章获得者。
- 用户显示名通过公开 `IdentityApi` 获取，身份已删除时降级为“已注销用户”。
- 每次保存前按 `(sourceEventId, recipientId)` 调用 `NotificationRepository.exists`；数据库既有唯一键继续承担并发兜底。

### 2.4 SearchIndexProjector：10 个方法

- 所有事件先调用既有 `ProcessedEventStore.markProcessed(eventId, "discovery.search-index")`；只有首次事件修改投影，且去重记录和投影写入处于同一事务。
- 问题发布插入完整 `SearchDocument`；问题编辑更新标题、纯文本、标签和时间。
- 回答提交增加回答数，回答删除减少回答数，问题删除标记投影删除；未知删除目标不修改索引。
- 回答采纳更新采纳状态；只有问题投票按 `newValue - oldValue` 调整问题得分，回答投票不影响问题得分。
- 悬赏开启设置 `bountyOpen=true`，悬赏发放或关闭设置为 `false`。

## 3. 新增应用层测试

新增 4 个测试类、17 个测试方法，使用 Mockito 隔离数据库和其他上下文，仅通过已有公开端口/API 验证真实返回值和副作用：

1. `DiscoveryQueryServiceTest`：3 项，覆盖搜索字段和分页映射、作者批量查询及注销用户降级、相关问题过滤/排序/相似度/限制、无标签和非正 limit。
2. `NotificationServiceTest`：4 项，覆盖当前收件人的分页列表、未读数、单条/全部已读、保存后的状态，以及不存在 `DIS-4001` 与越权 `DIS-3001` 的区分。
3. `NotificationProjectorTest`：5 项，覆盖回答/采纳/认证、评论提及、悬赏/徽章通知，缺失或已删除问题、未知昵称、自通知和重复事件分支。
4. `SearchIndexProjectorTest`：5 项，覆盖发布、编辑、回答计数、采纳、删除、问题/回答投票、悬赏状态及重复事件无副作用。

阶段 2 的 `DiscoveryDomainTest` 13 项保持不变，没有删除测试、修改原断言或降低覆盖率要求。

## 4. 错误码核对

- `docs/错误码.md` 明确 `DIS-3xxx` 属于 Discovery 权限错误，`ForbiddenException` 的既有 HTTP 映射为 403，因此领域层当前 `DIS-3001` 的异常类型和 HTTP 语义一致。
- 但错误码速查只登记了 `DIS-1001`，没有显式登记 `DIS-3001` 或 `DIS-4001`；仓库也没有另一条冲突定义。阶段 3 没有擅自修改公共错误码文档或共享 HTTP 映射。
- 当前实现继续使用阶段 2 已采用的 `DIS-3001` 表示通知越权，使用分段约定下的 `DIS-4001` 表示通知不存在。两者应在 A/B/C 合并前由团队确认并正式登记到错误码文档。

## 5. 实际验证与结果

### 5.1 应用层及依赖测试

从 `backend/` 执行：

```powershell
mvn -pl co-discovery/co-discovery-application -am test
```

最终结果为 `BUILD SUCCESS`：

- Discovery 领域层：13/13 通过。
- Discovery 应用层：17/17 通过（3 + 5 + 4 + 5）。
- `-am` 带入的 shared-kernel `MaskingTest`：2/2 通过。
- 本次 reactor 合计：32/32 通过，0 失败、0 错误、0 跳过。

第一次受限执行时，Mockito inline mock maker 无法在沙箱权限下附加 Byte Buddy agent，17 个应用测试在进入测试方法前统一报初始化错误；允许 Maven/JVM 使用本机依赖缓存和测试 agent 后，以最终文件集复跑通过。这是执行环境限制，不是业务断言失败。成功运行仍打印 Mockito 关于未来 JDK 将限制动态 agent 的警告；本阶段没有因此修改生产配置、POM 或跳过测试。

### 5.2 完整 Discovery 子模块

按要求执行：

```powershell
mvn -pl co-discovery -am test
```

该命令虽然 `BUILD SUCCESS`，但 Maven 在当前父 reactor 的项目选择行为只执行了根 POM 和 `co-discovery` 聚合 POM，没有进入其五个子模块，所以不把它当作完整模块验证。随后补跑：

```powershell
mvn -pl co-discovery/co-discovery-infrastructure,co-discovery/co-discovery-interfaces -am test
```

结果为 `BUILD SUCCESS`，14/14 reactor 项成功；Discovery API、domain、application、infrastructure、interfaces 五层均实际编译/测试。测试仍为 Discovery 30/30，加 shared-kernel 2/2；基础设施和接口模块当前没有测试源。

受限沙箱中的第一次尝试因不能写 `C:\Users\思源\.m2\...lastUpdated` 而在项目解析阶段退出；授予本机 Maven 缓存访问后同参数重跑成功，此失败不属于源码或测试失败。

### 5.3 ArchUnit

执行现有架构测试：

```powershell
mvn -pl co-bootstrap -am '-Dtest=ArchitectureTest' '-Dsurefire.failIfNoSpecifiedTests=false' test
```

结果：`ArchitectureTest` 1/1 通过，0 失败、0 错误、0 跳过；27/27 reactor 项成功，ArchUnit 检测到 Java 21.0.11。第一次未加引号的 PowerShell 命令因属性参数解析错误，未进入编译或测试；修正命令后成功。

没有运行依赖 Docker 的集成测试、完整 `mvn verify`、端到端测试或部署。

## 6. 与 A、B 的后续联调事项

- 等待 A 的 Q&A 实际发布并经 outbox 转发 Question/Answer/Comment/Vote/ContentDeleted 事件，核对事件 ID、发生时间、目标类型以及编辑/删除时序。
- 等待 A 的 `IdentityApi.findUser/findUsers/findByDisplayName` 真实行为稳定，验证注销用户降级、显示名唯一解析和提及通知。
- 等待 B 的悬赏与徽章事件实际发布，验证悬赏开启/发放/关闭的最终状态及通知收件人。
- 在 MySQL 8 ngram 环境补基础设施集成测试：中文全文检索、标签回填、分页排序、软删除不可检索、重复事件唯一约束与事务回滚。
- 三方契约稳定后再做“提问 → 搜索 → 回答/提及通知 → 采纳 → 声誉/悬赏/徽章 → 搜索状态更新”的端到端链路；本阶段未伪造联调或实验结果。

## 7. 修改范围与自查

本阶段新增或修改的应用层文件：

- `backend/co-discovery/co-discovery-application/src/main/java/com/campusoverflow/discovery/application/DiscoveryQueryService.java`
- `backend/co-discovery/co-discovery-application/src/main/java/com/campusoverflow/discovery/application/NotificationService.java`
- `backend/co-discovery/co-discovery-application/src/main/java/com/campusoverflow/discovery/application/NotificationProjector.java`
- `backend/co-discovery/co-discovery-application/src/main/java/com/campusoverflow/discovery/application/SearchIndexProjector.java`
- `backend/co-discovery/co-discovery-application/src/test/java/com/campusoverflow/discovery/application/DiscoveryQueryServiceTest.java`
- `backend/co-discovery/co-discovery-application/src/test/java/com/campusoverflow/discovery/application/NotificationServiceTest.java`
- `backend/co-discovery/co-discovery-application/src/test/java/com/campusoverflow/discovery/application/NotificationProjectorTest.java`
- `backend/co-discovery/co-discovery-application/src/test/java/com/campusoverflow/discovery/application/SearchIndexProjectorTest.java`
- `docs/c/阶段3-Discovery应用实现与验证.md`

工作区还保留阶段 1、阶段 2 的文档，以及阶段 2 的 4 个领域实现文件和 `DiscoveryDomainTest`。没有修改 Q&A、Reputation、Identity 的业务源码，也没有修改控制器、基础设施适配器、shared-kernel、POM、Docker、CI、断言或覆盖率门槛。

- `git diff --check`：通过，退出码 0。
- Discovery 主代码 TODO 扫描：应用层、领域层均无业务 TODO 残留。
- 当前仍有未提交修改和未跟踪文档/测试，符合本阶段“不提交、不推送、不创建 PR”的要求。

## 8. PR #9 合并后复核（2026-10-26）

- PR #9 在 head `0beaa78`、base `feature/c-discovery`、`reviewDecision=APPROVED`、无未解决线程、`mergeStateStatus=CLEAN` 的条件下正常合并，合并提交为 `ff90278`；没有绕过检查或保护规则。
- 本地 `feature/c-discovery` 通过 fast-forward 同步到 `ff90278`。相对最新 main 的 PR #4 差异只包含 Discovery 领域/应用实现、对应测试、基础设施测试依赖、阶段 1～3 记录和错误码登记，没有前端或 A/B 业务代码。
- JDK 21.0.11 下执行 `mvn -pl co-discovery/co-discovery-infrastructure -am test` 成功：Discovery domain 13/13、application 17/17，合计 30/30；另有 shared-kernel 2/2，均无失败、错误或跳过。
- 显式设置 `DISCOVERY_IT_DB_URL`、`DISCOVERY_IT_DB_USER`、`DISCOVERY_IT_DB_PASSWORD`，连接本机 `127.0.0.1:3307` 的 MySQL 8.0.39，执行 `mvn -pl co-discovery/co-discovery-infrastructure -am '-Dtest=DiscoveryMySqlIT' '-Dsurefire.failIfNoSpecifiedTests=false' test`。两项 IT 最终 2/2 通过、0 跳过：覆盖通知持久化/重复事件幂等/归属校验/已读计数，以及中文搜索投影/标签读取/软删除排除。
- 首次数据库命令因 PowerShell URL 变量边界写法错误而未进入断言；修正会话参数后连接成功。随后发现 infrastructure 测试模块缺少 application 测试已有的 Mockito subclass 配置，在 Oracle JDK 21 下初始化失败；补充同一测试资源后通过，未修改业务逻辑、断言、CI或覆盖率门槛。
- 测试使用随机正数问题/收件人 ID 和随机事件 ID，并在 `finally` 中按本次 ID 清理。复核 `dis_notification` 的 `dis-it-answer-%` 夹具及测试标题对应的 `dis_search_doc` 行数均为 0，没有残留本次测试数据。

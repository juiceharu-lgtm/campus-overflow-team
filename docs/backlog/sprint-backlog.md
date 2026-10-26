# CampusOverflow Sprint Backlog

更新基线：2026-10-26。PR #10、#4、#5 已依次正常进入 main；PR #9 已先合入 Discovery 分支。Q&A 已有 PR #15，但最新 `dd761dc` 仍有 4 个查询 TODO，application 测试因 Answer 服务构造器契约不一致而无法编译。原 `co-reputation` 仍未完成。没有远端证据的成员工作继续标为“待负责人确认”。

故事 ID 原样引用 A 的 [PR #3](https://github.com/juiceharu-lgtm/campus-overflow-team/pull/3) Product Backlog（US‑001～US‑013），本文件不重新编号用户故事。Sprint 与课程原文的对应关系已按 `CampusOverflow实验部分 (1).pdf` 文件页序复核：Lab1/Sprint 0（PDF 第1～3页）、Lab2～3/Sprint 1（第3～6页）、Lab4～5/Sprint 2（第6～10页）、Lab6～7/Sprint 3（第10～13页）、Lab8/Sprint 4（第13～15页）。

## 分工差异说明

本 Backlog 采用当前已确定的分工：A 负责 Q&A、Identity 扩展；B 负责 Reputation、整体测试和覆盖率；C 负责 Discovery、前端增强、文档、流水线和最终交付。

PR #3 的 `team-work-allocation-v1.md` 同时将 Lab4～Lab6 的前端主体分配给 B，并将 C 写为前端校验、Discovery 和材料归档。该差异尚未看到全员确认记录，需由 A、B、C 明确“前端主体”“前端增强”和“联调”的最终边界。本文件不修改 PR #3 原文。

## S0 环境与协作准备

| 任务 ID | 关联故事 | 任务内容 | 负责人 | 依赖 | 验收条件 | 当前状态 | 证据链接 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| S0-01 | — | 恢复完整初始框架和正确目录 | C | 老师原始 ZIP | main 中后端、前端、部署和 CI 结构齐全 | 已合并 | [PR #1](https://github.com/juiceharu-lgtm/campus-overflow-team/pull/1) |
| S0-02 | — | 环境核验与架构阅读记录 | C | JDK 21、Node 20+ | 区分执行环境与个人电脑，记录真实命令和限制 | 已提交待评审 | [PR #4 阶段1文档](https://github.com/juiceharu-lgtm/campus-overflow-team/pull/4) |
| S0-03 | — | A 个人介绍与角色承诺 | A | 无 | 文档内容经本人和队友评审 | 已合并 | [PR #2](https://github.com/juiceharu-lgtm/campus-overflow-team/pull/2)；main `de75f0d` |
| S0-04 | — | C 个人介绍与角色承诺 | C | 本人确认 | 仅含已知信息；承诺文本由本人确认；课程要求的签字记录真实存在 | 进行中 | `docs/team/hello-C.md`、`docs/team/role-commit-C.md`（待本人确认，尚未签字） |
| S0-05 | — | B 个人介绍与角色承诺 | B | B 提供内容 | 文档存在并经本人确认 | 待负责人确认 | 尚无可引用证据 |
| S0-06 | — | 团队章程、分支和 DoD | 全员；C 整理 | 全员确认 | 价值观、沟通、开发规约、DoD 均明确；形成课程要求的正式 `charter-v1.0.md` 并保留确认记录 | 进行中 | `docs/team/team-charter-v1-draft.md`（待全员确认，不是正式版） |
| S0-07 | — | 看板、IM 和分支保护核验 | 全员 | 仓库管理权限 | 保存真实配置或截图，不含凭据 | 待负责人确认 | 尚无可引用证据 |

## S1 Identity 增量

| 任务 ID | 关联故事 | 任务内容 | 负责人 | 依赖 | 验收条件 | 当前状态 | 证据链接 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| S1-ID-01 | US‑001 | 保持注册、登录参考实现可用 | A | 初始 Identity 实现 | Identity 既有测试和接口通过 | 已合并 | [main README](../../README.md)；初始框架 Identity 测试 |
| S1-ID-02 | US‑002 | 个人资料增量需求与实现 | A | 需求确认、Identity 契约 | 用例、权限和测试明确并通过 | 待负责人确认 | PR #2～#5 未见对应成果 |
| S1-ID-03 | US‑003 | 选课申请审批领域模型与用例 | A | Identity 仓储、权限和接口适配 | 领域约束、应用用例、持久化/API 和测试闭环 | 进行中 | [远端提交 7323cef](https://github.com/juiceharu-lgtm/campus-overflow-team/commit/7323cef)；尚未在 PR #2～#5 中提交 |
| S1-ID-04 | US‑002、US‑003 | Identity 增量测试与前端联调 | A；C 协助前端 | S1-ID-02、S1-ID-03 | 正常、边界、越权场景通过真实接口验证 | 待负责人确认 | 尚无可引用验证结果 |

## S2 Q&A

| 任务 ID | 关联故事 | 任务内容 | 负责人 | 依赖 | 验收条件 | 当前状态 | 证据链接 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| S2-QA-01 | US‑004～US‑011 | Q&A 领域层 36 个 S2 TODO 中的领域规则 | A | Identity API、既有测试 | Question、Answer、Comment、Vote、Tags、MarkdownBody 测试通过 | 进行中 | [PR #15](https://github.com/juiceharu-lgtm/campus-overflow-team/pull/15)：domain 18/18；36 个可执行 TODO 已完成 32 个 |
| S2-QA-02 | US‑004～US‑011 | Q&A 应用层命令与查询用例 | A | S2-QA-01、仓储/API | 应用测试通过，跨上下文仅经 API/事件 | 进行中 | PR #15 仍缺 4 个查询 TODO，Answer 服务入口、事件、缓存和审计契约未对齐 |
| S2-QA-03 | US‑004～US‑011 | Q&A 补充单元测试与接口自测 | A；B 统筹质量 | S2-QA-01、S2-QA-02 | 既有断言保留，新增边界测试，结果可追溯 | 进行中 | application 在 `AnswerAndVoteServiceTest` 测试编译阶段失败；PR 正文“全部通过”需更正 |
| S2-QA-04 | US‑004～US‑011 | Q&A 事件发布与 Discovery/Reputation 契约联调 | A；B、C 配合 | Q&A、Reputation、Discovery 实现 | 问题、回答、投票、采纳、删除等事件真实投递且幂等 | 未开始 | 三个业务分支尚未共同进入 main |

## S3 Reputation 与 Discovery

| 任务 ID | 关联故事 | 任务内容 | 负责人 | 依赖 | 验收条件 | 当前状态 | 证据链接 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| S3-REP-01 | US‑010～US‑012 | Reputation 领域层 TODO 与单元测试 | B | Q&A 事件契约 | 账户、规则、徽章、悬赏、学期规则测试通过 | 待负责人确认 | [任务清单](../学生任务清单.md)；未看到 B 的远端成果，不能据此判断是否未开发 |
| S3-REP-02 | US‑010～US‑012 | Reputation 应用层、事件处理和幂等 | B | S3-REP-01、Q&A 事件 | 应用测试通过，重复事件无重复积分或副作用 | 待负责人确认 | 尚无可引用证据 |
| S3-DIS-01 | US‑013 | Discovery 领域层 7 个 TODO 与测试 | C | Q&A 数据语义 | 领域测试 13/13 通过 | 已合并 | [PR #4](https://github.com/juiceharu-lgtm/campus-overflow-team/pull/4)；main 合并提交 `b4a136d` |
| S3-DIS-02 | US‑013 | Discovery 应用层 25 个 TODO 与测试 | C | Identity API、Q&A API、事件契约 | 应用测试 17/17 通过，事件消费幂等 | 已提交待评审 | [正式 PR #4](https://github.com/juiceharu-lgtm/campus-overflow-team/pull/4)；提交 `d600edf` 后 17/17 通过 |
| S3-DIS-03 | US‑013 | Discovery MySQL ngram、通知与跨上下文联调 | C；A、B 配合 | S2、S3-REP、Docker/MySQL | 真实 MySQL 搜索、通知、编辑删除和重复事件链路通过 | 未开始 | PR #4 明确未完成 Docker/MySQL/E2E |
| S3-INT-01 | US‑010～US‑013 | Q&A→Reputation/Discovery 事件链联调 | A、B、C | 三个模块合并或可组合环境 | outbox 投递、重试、幂等和最终一致性均有真实记录 | 未开始 | 当前无真实 E2E 证据 |

## S4 前端、质量、流水线与交付

| 任务 ID | 关联故事 | 任务内容 | 负责人 | 依赖 | 验收条件 | 当前状态 | 证据链接 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| S4-FE-01 | US‑004～US‑013 | 页面状态、Markdown 预览、悬赏对话框、通知和移动端样式 | C | 现有前端骨架 | npm ci、typecheck、build 通过；安全与键盘交互可评审 | 已合并 | [PR #5](https://github.com/juiceharu-lgtm/campus-overflow-team/pull/5)；main 合并提交 `9e4b4ce`；组合分支构建通过 |
| S4-FE-02 | US‑004～US‑013 | 三个业务上下文真实前端联调 | C；A、B 配合 | Q&A、Reputation、Discovery、Identity 可运行 | 不使用生产伪数据；关键成功、错误和权限场景通过 | 未开始 | PR #5 仅完成隔离 mock 浏览器验证 |
| S4-FE-03 | US‑004～US‑013 | 移动端与可访问性验收 | C | 可调视口浏览器或实体设备 | 指定窄视口、键盘和焦点场景有真实截图/记录 | 进行中 | PR #5 已实现样式和键盘逻辑；窄视口实测待补 |
| S4-QA-01 | — | 全量单元测试与覆盖率统筹 | B；全员修复各自模块 | S1～S3 业务完成 | 全量测试通过；Lab7 团队单测覆盖率 >70%、核心聚合 90%+；README 领域层门槛 ≥70%；形成 JaCoCo HTML | 进行中 | CI 已运行但被 Q&A 原始 TODO 阻塞；覆盖率步骤尚未执行 |
| S4-QA-02 | — | ArchUnit 架构约束 | B 统筹；全员 | 可编译的完整业务代码 | 全量 ArchitectureTest 通过 | 进行中 | PR #4 记录其分支 1/1 通过；PR #5 CI 因 Q&A 失败未执行全量架构步骤 |
| S4-CI-01 | — | CI/CD 最终验证 | C；B 统筹测试 | 全模块测试和配置 | README 验收全绿；Lab7 质量门禁持续生效；Lab8 从 push 到测试环境自动部署、Smoke Test 成功 | 进行中 | [PR #5 Actions](https://github.com/juiceharu-lgtm/campus-overflow-team/actions/runs/39763296741)：仅确认前端成功、后端测试失败；尚无自动部署成功记录 |
| S4-E2E-01 | US‑001～US‑013 | Docker、MySQL、Redis 和真实 E2E | C；全员 | 三个业务模块、Docker | 提问→回答→采纳→声誉→搜索→通知链路通过 | 未开始 | 仅有预置配置和测试骨架，无实际成功记录 |
| S4-PERF-01 | — | 压测、指标与瓶颈分析 | C；全员 | 稳定可运行环境 | 记录场景、数据量、工具、吞吐、延迟、错误率和结论 | 未开始 | 尚无压测报告或原始结果 |
| S4-OPS-01 | — | 部署、监控和运维手册 | C；A、B 配合 | Docker/E2E 通过、部署环境 | 健康检查、指标、告警、回滚和故障处理均可复现 | 未开始 | main 仅有预置 Dockerfile、compose、Prometheus 配置 |
| S4-DEMO-01 | — | 演示脚本、截图、录屏和最终材料 | C 汇总；全员提供模块材料 | 所有验收结果 | 每项材料有真实来源、负责人、路径和缺失说明 | 进行中 | `docs/c/交付材料清单.md` |

## 当前合并门槛

- PR #4 已登记 `DIS-3001` / `DIS-4001` 并转为正式 PR；登记明确为 C 依据现有异常映射提出，仍需团队在评审中确认。新提交 `d600edf` 后需以最新 Actions 和有效批准为准。
- PR #5 可进行代码评审，`npm ci`、类型检查和构建已复跑通过；隔离 mock 浏览器验证不能替代真实后端和移动端验收。npm 审计仍报告需跨主版本处理的 Vite/React Router 风险，不能盲目 `audit fix --force`。
- PR #6 已安全同步 PR #2、#3 合并后的 main，并更新状态证据；新提交后需重新核对批准和 Actions。
- 全量 CI 仍被 main 中 Q&A 原始 TODO 阻塞；不得为变绿而删除测试或降低门槛。
- GitHub API 当前未返回 main 分支保护或仓库 ruleset；这与课程要求不一致，需仓库管理员配置并保存真实证据。
- B 的实际进展需要 B 本人或可引用的分支/PR/报告确认。

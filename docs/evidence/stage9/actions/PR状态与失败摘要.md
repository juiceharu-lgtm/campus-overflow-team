# 阶段9远端 PR 与 Actions 证据摘要

核验时间：2026-10-23；增量核验：2026-10-25（Asia/Shanghai）

核验方式：`git fetch`/`git ls-remote`、GitHub Pull Request/Review/Checks API、失败 Job 原始日志。仅记录可复核摘要，不保存凭据或庞大原始日志。

## 当前远端

- `origin/main`：`de75f0d`，包含已合并 PR #2、#3。
- A Identity 分支：`feature/a-s1-identity-extend` at `7323cef`；未发现对应 PR。
- 未发现 A 的 Q&A 业务分支/PR，也未发现 B 的 Reputation 业务分支/PR；未推送的本地进度均为“待负责人确认”。

## 本人 PR

| PR | 当前状态 | 评审 | 检查与阻碍 |
| --- | --- | --- | --- |
| #4 Discovery | 已从 Draft 转正式；最新提交 `d600edf` | 原提交有 `ashy069`、`SophiaYuan11701` 两位批准；新增提交后需重新核对批准有效性 | 新一轮 Actions 待完成；既有后端失败来自 Q&A TODO |
| #5 前端增强 | 开放正式 PR，提交 `b4376e3` | 两位批准，无行内修改意见 | 前端成功；后端失败；镜像跳过；真实后端和窄视口验收未完成 |
| #6 C 文档 | 开放正式 PR；本轮安全同步 main 并更新清单 | 原提交有两位批准；新增提交后需重新核对 | 此前前端成功；后端失败；镜像跳过 |

GitHub API 查询 main protection 返回 404，仓库 ruleset 列表为空；当前没有可引用的分支保护证据。课程要求仍需仓库管理员实际配置并截图。

## 后端失败定位

PR #4、#5、#6 既有失败 Job 的结论一致：`co-qa-domain` 运行 18 项，2 项失败、13 项错误。直接原因是以下 Q&A 初始 TODO 抛出 `UnsupportedOperationException`：

- `MarkdownBody.ofQuestion`
- `Tags.names`
- `Comment.post`
- `Vote.cast`

这不是 Discovery 或前端断言失败，但必需的完整后端流水线仍未通过。没有删除测试、放宽断言、降低门槛或修改 CI 来掩盖结果。

## 2026-10-25 增量状态

| PR/分支 | 最新 SHA | 评审 | Actions/结论 |
| --- | --- | --- | --- |
| PR #4 Discovery | `d600edf` | 两份批准均针对旧 SHA `64b48cd`，需重新评审 | 前端成功；后端失败；镜像跳过，不可合并 |
| PR #5 前端增强 | `b4376e3` | 两份批准均针对当前 SHA | 前端成功；后端失败；镜像跳过，不满足检查条件 |
| PR #6 C 文档 | `9e4f7dd` | 两份批准均针对旧 SHA `8145968`，需重新评审 | 前端成功；后端失败；镜像跳过，不可合并 |
| PR #7 联调准备 | `3fcdf8b` | 无评审 | 前端成功；后端失败；镜像跳过，本次也不自动合并 |
| PR #8 A Lab3 文档拆分 | `a0df58a` | 无评审 | 前端成功；后端失败；镜像跳过；失败仍为相同 Q&A TODO |

PR #8 的最新失败 Job（`120912072735`）实际运行 `co-qa-domain` 18 项，结果仍为 2 failures、13 errors；日志中的直接抛出点与此前相同。该 PR 只修改 `docs/ddd/` 下4个 Markdown文件，没有提交 Q&A 或 Reputation 业务实现。

远端仍只有 A 的 Identity 增量分支 `7323cef`，未形成 PR或测试/基础设施闭环；未发现 A 的 Q&A 业务分支/PR，也未发现 B 的 Reputation 业务分支/PR。未知的本地工作继续标记为“待负责人确认”。

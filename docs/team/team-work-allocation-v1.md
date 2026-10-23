## CampusOverflow Lab完整角色分工
> 一次性集中完成模式，骨架、Docker、CI、控制器、适配器等基础设施代码已预置，不重复开发；仅编写业务领域层、应用层代码、配套文档与测试。

### 通用协作规则
1. 所有研讨会议（团队组建、故事地图、事件风暴、Sprint规划、Review、回顾会）全员共同参与讨论。
2. 模块负责人为主执笔、实现；另外两人必须完成代码、文档、领域模型Review，给出可落地修改意见，留存评审截图。
3. 严格Git分支开发流程，所有改动走PR评审，双人评审通过后方可合并；Git、PR评审全员参与。
4. 工作量应急调整：
   - A后端编码压力大，B分担部分后端单元测试；
   - B测试工作量过载，C协助补充模块单元测试；
   - C文档部署压力大，A、B协助校对文档、排查流水线、接口联调；
   - 所有产出物仍需要交叉评审，守住ArchUnit架构约束。

### 角色总览
- **A角色**：需求建模主负责人；S1 Identity增量扩展；S2 Q&A上下文（36个TODO）后端领域&应用编码；对应Lab1‑Lab4、Lab6的后端业务、领域建模工作
- **B角色**：验收用例、测试质量总负责人；S3 Reputation声誉模块实现；统筹单元测试、覆盖率、ArchUnit架构校验；前端业务特性开发；对应Lab1‑Lab3评审校验、Lab5前端主体、Lab6前端交互、Lab7质量门禁
- **C角色**：环境搭建、工程配置、全部文档/截图/录屏归档；S3 Discovery发现上下文实现；S4前端增强；CI/CD、容器编排、监控运维；对应全实验产物归档、Lab8容器化部署上线

---

## 分实验详细分工
### Lab1 团队组建与协作工具链｜Sprint0
> 实验产出：团队仓库、分支保护配置、看板、IM工具、个人hello文档、角色承诺书、《团队章程v1.0》
- **A**：参与团队角色确认；协助执笔《团队章程 v1.0》；完成个人hello‑A.md、role‑commit‑A.md文档，提交分支与PR；参与全部团队讨论。
- **B**：协作工具链校验；检查仓库分支保护、GitHub看板规则是否生效；完成A、C文档的PR评审，给出修改意见。
- **C**：Fork团队仓库；完成GitHub/Gitee仓库、Project看板、IM工具链配置；收集Lab1全部终端截图、PR评审记录，统一归档。

### Lab2 需求建模工作坊｜Sprint1
> 实验产出：用户故事地图、Product Backlog表格（INVEST校验+优先级）、Gherkin验收用例、Sprint1 Planning与Sprint Backlog
- **A（主执笔）**：主导绘制用户故事地图；完成Product Backlog表格编写，执行INVEST原则校验、价值成本优先级排序；产出Backlog初稿，交付B做验收用例评审。
- **B**：基于Backlog，编写全部Gherkin `.feature`验收场景，覆盖正常、边界、异常路径；评审故事地图、Backlog条目合理性。
- **C**：整理Sprint1 Planning会议输出；拆分Sprint Backlog任务；导出Lab2全部产物，归档到仓库对应目录。

### Lab3 领域驱动设计‑事件风暴｜Sprint1
> 实验产出：事件风暴白板记录、聚合识别清单、上下文映射图Context Map、统一语言术语表v1.0
- **A（主执笔）**：主导事件风暴，梳理领域事件、命令、角色卡片；完成聚合识别，输出聚合清单（聚合根、业务不变式）。
- **B**：审核聚合不变式合理性；评审事件风暴业务模型；校验业务术语准确性。
- **C**：绘制上下文映射图Context Map；编写《统一语言术语表v1.0》；将Lab1‑Lab2‑Lab3所有Markdown文档、截图统一归档入库。

### Lab4 后端工程骨架与领域代码｜Sprint2
> Maven骨架、Flyway脚本、全局异常、日志、控制器、JPA适配器为预置代码，不重复开发
> 实验产出：Q&A领域与应用层业务实现、单元测试、ADR架构决策记录、接口自测验证
- **A（主开发）**：完成Q&A上下文全部36个TODO业务代码（领域层+应用服务层）；编写业务单元测试；输出1份ADR架构决策文档；完成后端接口自测、Postman接口验证；完成后端代码自审，提交PR；参与前端PR文档评审。
- **B（主开发）**：基于预置模板完成前端业务页面开发；完善路由、布局、提问/列表/详情页面；完成前后端提问流程端到端调试，录制演示录屏；保证`pnpm lint`、`pnpm typecheck`校验。
- **C**：校验前端各类检查命令全绿；检查pre‑commit钩子拦截效果；Lab4、Lab5全部工程文档、截图统一归档。

### Lab5 前端工程骨架与对接｜Sprint2
> 前端工程基础骨架、openapi类型生成基础模板预置，不从零搭建
> 实验产出：前端页面、路由布局、提问‑列表‑详情端到端调通、录屏
- **A**：配合前端接口联调，排查后端接口异常；参与前端PR文档评审。
- **B（主开发）**：基于预置模板完成前端业务页面开发；完善路由、布局、提问/列表/详情页面；完成前后端提问流程端到端调试，录制演示录屏；保证`pnpm lint`、`pnpm typecheck`校验。
- **C**：校验前端各类检查命令全绿；检查pre‑commit钩子拦截效果；Lab4、Lab5全部工程文档、截图统一归档。

### Lab6 核心特性端到端开发与Sprint Review｜Sprint3
> 实验产出：投票/采纳/标签/声誉业务代码、领域事件联动、单元测试、Sprint Review演示脚本、Sprint3回顾纪要
- **A**：后端参与：评审Reputation、Discovery模块业务代码；继续完善Q&A模块遗留BUG；准备Sprint Review后端功能演示脚本；参与Sprint Retrospective回顾会。
- **B（主开发）**：实现Reputation声誉模块领域层+应用层业务代码；开发前端投票、采纳、标签UI交互；联调后端接口；手工功能测试，记录BUG；参与Sprint Review现场演示。
- **C**：实现Discovery发现上下文剩余TODO；主持Sprint Retrospective；撰写`sprint‑3.md`回顾纪要；编写至少1份关键设计ADR文档；录制Sprint Review演示录屏；校验main分支增量完整可用；Lab6全部产出整理提交仓库。

### Lab7 测试、代码评审与质量门禁｜Sprint3
> SonarQube、JaCoCo基础配置模板预置，无需从零编写全部配置
> 实验产出：单元&集成测试、覆盖率报告、SonarQube质量门禁、PR评审记录、团队评审清单
- **A**：补齐Q&A领域层单元测试；使用Testcontainers编写Controller‑Service‑Repository集成测试；清理无效断言，补充边界测试用例。
- **B（测试总负责人）**：完善JaCoCo、SonarQube质量门禁规则；接入CI流水线；推动整体领域覆盖率≥70%，核心聚合≥90%；获取SonarQube仪表盘、JaCoCo覆盖率报告；维护测试用例。
- **C**：完成多条PR深度评审，收集≥4份PR评审记录；编写《团队代码评审清单v1.0》；Lab7报告、截图、文档统一归档仓库。

### Lab8 CI/CD流水线、容器化部署与上线｜Sprint4
> Dockerfile、docker‑compose、CI流水线基础模板预置，仅做业务适配修改，禁止从零手写整套
> 实验产出：Docker镜像、编排配置、CI/CD流水线、Grafana监控、线上可访问系统、运维手册
- **A**：后端适配预置Dockerfile，核对后端环境变量，禁止硬编码密码；本地验证后端镜像启动；参与上线验证。
- **B**：适配前端Dockerfile与Nginx配置；SPA路由fallback、api反向代理校验；本地验证前端镜像。
- **C（部署总负责人）**：完善docker‑compose.yml，MySQL/Redis/Prometheus/Grafana健康检查配置；完善CI/CD流水线（构建‑镜像推送‑SSH部署‑冒烟测试）；配置Grafana面板、告警规则；线上URL验证；编写《运维手册v1.0》；收集流水线、监控截图，Lab8全部配置与文档归档。

---

### 禁止重复开发清单（平台已预置，无需实现）
- 搭建Maven多模块骨架、维护父POM
- 手写Flyway初始化脚本、全局异常处理器、日志框架完整配置
- 从零编写前后端完整Dockerfile、Nginx全套配置
- 从零编写完整docker‑compose、从零配置GitHub Actions流水线
- 手写JPA、Redis基础设施适配器、REST控制器

### Sprint阶段简表
| Sprint | 主线任务 | 牵头负责人 |
|--------|----------|------------|
| S0 | 环境验证、读懂Identity范例，熟悉预置工程骨架 | C，全员参与 |
| S1 | Identity增量扩展：选课审批、个人资料需求梳理与建模 | A，全员评审需求 |
| S2 | Q&A上下文36个TODO实现、领域建模、自测 | A |
| S3 | Reputation声誉模块；Discovery发现模块开发；统筹全量测试、覆盖率、架构校验 | B（Reputation+测试总负责）、C（Discovery） |
| S4 | 前端增强；全员压测、演示准备；流水线验收、全套材料汇总提交 | C牵头，全员参与 |

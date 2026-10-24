# 限界上下文映射 Context Map
1. Identity ←→ Q&A / Reputation / Discovery：客户‑供应商模式，其他上下文只读查询用户基础身份信息，不修改Identity内部数据。
2. Q&A → Reputation：发布‑订阅模式，Q&A产生领域事件，Reputation监听事件自动更新声誉。
3. Discovery ← Q&A：客户供应商，Discovery读取Q&A数据构建检索索引，单向数据同步。

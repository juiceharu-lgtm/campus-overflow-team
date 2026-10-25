# 跨上下文领域事件通信规范
## 1. 通用事件结构
```json
{
  "eventId": "uuid",
  "eventType": "固定事件名",
  "aggregateId": "聚合根ID",
  "occurredAt": "时间戳",
  "publisher": "发布上下文",
  "payload": {}
}

# 后端开发说明

## 后端完成了什么
- 提供完整用户与内容系统：注册登录、资料管理、文章发布/更新/删除、评论回复。
- 提供文章互动能力：浏览计数、点赞（收藏）统计、分享统计、相关推荐。
- 提供作者分析能力：我的文章数据查询、热度计算、Excel 报表导出（Apache POI）。
- 提供实时能力：WebSocket 推送社区实时指标（已发布文章数、用户数）。
- 提供缓存与性能能力：Redis + Spring Cache。

## 架构与实现要点
- 架构分层：Controller / Service / Repository / Entity / DTO。
- 鉴权方案：Spring Security + JWT。
- 数据层：Spring Data JPA 操作 MySQL，Redis 用于缓存与性能优化。
- 异常处理：统一异常响应结构，避免接口裸异常。
- 日志与监控：Actuator + Prometheus + Loki + Grafana。

## 技术栈映射
- Spring Boot / Spring MVC：API 服务主框架。
- Spring Task：定时推送实时指标。
- Swagger (springdoc)：接口调试与文档。
- WebSocket：实时社区数据广播。
- POI：作者分析报表导出（`.xlsx`）。
- Maven / JUnit：构建与测试基础。

## 关键目录
- `backend/src/main/java/com/blogplatform/controller`：REST API 入口。
- `backend/src/main/java/com/blogplatform/service`：业务逻辑与分析导出。
- `backend/src/main/java/com/blogplatform/config/WebSocketConfig.java`：WebSocket 配置。
- `backend/src/main/resources/data.sql`：初始化演示数据（游戏场景）。

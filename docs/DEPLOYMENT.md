# 部署文档

## 1. 启动方式
1. 进入目录：`cd label-3467`
2. 启动服务：`docker compose up --build`
3. 访问地址：`http://localhost:3467`

## 2. 服务拓扑
- `frontend`：Vue 打包产物服务
- `gateway`：Nginx 网关，统一入口
- `backend-1/backend-2`：Spring Boot 双实例
- `db`：MySQL 8.0
- `redis`：缓存服务
- `prometheus/loki/promtail/grafana`：监控与日志

## 3. 环境变量
- `SPRING_DATASOURCE_URL=jdbc:mysql://db:3306/psyche_game...`
- `SPRING_REDIS_HOST=redis`
- `APP_FRONTEND_URL=http://localhost:3467`
- `JWT_SECRET=...`

## 4. 运维建议
- 首次上线请替换 JWT 密钥与数据库密码。
- 生产环境建议将 `db_data`、`uploads_data` 等卷映射到持久化存储。
- 通过 Grafana 检查 API 延迟、错误率和日志异常峰值。

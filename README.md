# Psyche Game (label-3467)

一个类似 4399 的游戏门户 + 攻略社区网站，支持用户浏览小游戏入口、发布攻略文章、评论互动，并在创作后台查看自己的浏览量/点赞量/评论量分析。

## 🛠 技术栈
- Frontend: Vue 3 + Vite + Element Plus + Pinia + Vue Router
- Backend: Spring Boot 3 + Spring MVC + Spring Security (JWT) + Spring Task + Spring Data JPA + Spring Data Redis + WebSocket + Swagger + POI
- Database: MySQL 8.0 + Redis 7
- Proxy/LB: Nginx (双实例后端反向代理)
- Deployment: Docker Compose (All-in-One)
- Engineering: Maven + Git + JUnit + Postman

## 🚀 启动指南 (How to Run)
1. 确保 Docker Desktop 已启动。
2. 进入项目目录：`cd label-3467`
3. 一键启动：`docker compose up --build`
4. 首次构建完成后，访问前端地址进行体验。

## 🔗 服务地址 (Services)
- Frontend + Gateway: [http://localhost:3467](http://localhost:3467)
- Backend API (直连实例1): [http://localhost:8467](http://localhost:8467)
- Swagger: [http://localhost:3467/swagger-ui](http://localhost:3467/swagger-ui)
- WebSocket Endpoint: `ws://localhost:3467/ws`
- MySQL: `localhost:13467` (user: `root` / pass: `root123`)
- Redis: `localhost:16467`
- Prometheus: [http://localhost:39467](http://localhost:39467)
- Grafana: [http://localhost:33467](http://localhost:33467) (`admin` / `admin123`)
- Loki: [http://localhost:33468](http://localhost:33468)

## 🧪 测试账号
- 管理员: `admin@psychegame.com` / `password`
- 创作者1: `luna@psychegame.com` / `password`
- 创作者2: `ryan@psychegame.com` / `password`

## ✅ 核心验收路径
1. 打开 [http://localhost:3467](http://localhost:3467)，查看“游戏广场”与攻略流。
2. 使用创作者账号登录后，发布一篇攻略并进入“创作数据中心”。
3. 在“创作数据中心”确认浏览量、点赞量、评论数与热度排行可见。
4. 点击“导出分析报表”，下载 Excel（POI 生成）。
5. 在 Swagger 中调用文章、评论、收藏、分享接口，验证真实数据库读写。

## 📁 项目结构
```
label-3467/
├── frontend/                 # Vue 前端（游戏广场 + 攻略社区）
├── backend/                  # Spring Boot 后端（API + Redis + WebSocket + POI）
├── infra/                    # Nginx / Prometheus / Loki / Promtail / Grafana 配置
├── docs/                     # 项目流程、前后端开发文档、API、部署、测试
├── docker-compose.yml        # 一键启动全链路
├── ai.md                     # 本次实现思考记录
└── README.md
```

## 📚 开发文档
- [项目流程与设计说明](./docs/PROJECT_FLOW.md)
- [前端开发说明](./docs/FRONTEND_DEV.md)
- [后端开发说明](./docs/BACKEND_DEV.md)
- [接口文档](./docs/API.md)
- [部署文档](./docs/DEPLOYMENT.md)
- [测试报告](./docs/TEST_REPORT.md)
- [用户手册](./docs/USER_MANUAL.md)

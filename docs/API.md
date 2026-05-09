# API 文档（Psyche Game）

## 基础信息
- 网关前缀：`/api`
- Swagger：`/swagger-ui`
- OpenAPI：`/api-docs`

## 认证模块
- `POST /api/auth/register` 注册
- `POST /api/auth/verify-email` 邮箱验证
- `POST /api/auth/login` 登录
- `POST /api/auth/forgot-password` 忘记密码
- `POST /api/auth/reset-password` 重置密码
- `GET /api/auth/password-strength` 密码强度检查

## 攻略文章模块
- `GET /api/articles` 文章列表（关键词/分类/时间筛选）
- `GET /api/articles/{id}` 文章详情（自动增加浏览量）
- `POST /api/articles` 发布文章
- `PUT /api/articles/{id}` 编辑文章
- `DELETE /api/articles/{id}` 删除文章
- `GET /api/articles/{id}/edit` 编辑详情
- `GET /api/articles/mine` 我的文章分页
- `GET /api/articles/mine/analytics/export` 导出作者分析 Excel
- `GET /api/articles/{id}/revisions` 历史版本
- `GET /api/articles/{id}/related` 相关推荐
- `POST /api/articles/{id}/favorite` 点赞/取消点赞
- `POST /api/articles/{id}/share` 分享统计

## 评论模块
- `GET /api/articles/{id}/comments` 评论树
- `POST /api/articles/{id}/comments` 发表评论/回复
- `DELETE /api/comments/{commentId}` 删除评论
- `POST /api/comments/{commentId}/like` 评论点赞

## 分类与标签
- `GET /api/categories` 分类列表
- `POST /api/categories` 新增分类（管理员）
- `GET /api/tags` 热门标签

## 用户模块
- `GET /api/users/me` 当前用户信息
- `PUT /api/users/me` 修改资料
- `PUT /api/users/me/password` 修改密码
- `POST /api/users/me/avatar` 上传头像
- `GET /api/users/me/favorites` 我的点赞文章

## 管理员模块
- `GET /api/admin/articles` 全站文章管理
- `GET /api/admin/users` 用户管理
- `PUT /api/admin/users/{id}/role` 修改角色
- `DELETE /api/admin/users/{id}` 删除用户

## 文件上传
- `POST /api/files/upload` 上传图片

## 实时能力
- WebSocket 连接：`ws://localhost:3467/ws`
- 订阅主题：`/topic/community-stats`
- 推送字段：`timestamp`、`publishedArticles`、`registeredUsers`

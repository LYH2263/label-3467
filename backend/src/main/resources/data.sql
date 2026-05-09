-- 用户初始化 (密码统一: password)
INSERT INTO users (username, email, password, role, email_verified, bio, created_at, updated_at)
SELECT 'admin', 'admin@psychegame.com', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'ROLE_ADMIN', true,
       'Psyche Game 平台管理员', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'admin@psychegame.com');

INSERT INTO users (username, email, password, role, email_verified, bio, created_at, updated_at)
SELECT 'luna', 'luna@psychegame.com', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'ROLE_USER', true,
       '擅长动作类游戏速通与入门攻略', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'luna@psychegame.com');

INSERT INTO users (username, email, password, role, email_verified, bio, created_at, updated_at)
SELECT 'ryan', 'ryan@psychegame.com', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'ROLE_USER', true,
       '偏爱策略与经营模拟，专注数据分析', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'ryan@psychegame.com');

-- 分类
INSERT INTO categories (name, description)
SELECT '动作竞技', '连招、位移、对抗与竞技玩法'
WHERE NOT EXISTS (SELECT 1 FROM categories WHERE name = '动作竞技');

INSERT INTO categories (name, description)
SELECT '休闲闯关', '轻松上手、节奏舒适、碎片化体验'
WHERE NOT EXISTS (SELECT 1 FROM categories WHERE name = '休闲闯关');

INSERT INTO categories (name, description)
SELECT '策略经营', '资源配置、关卡规划与高分策略'
WHERE NOT EXISTS (SELECT 1 FROM categories WHERE name = '策略经营');

INSERT INTO categories (name, description)
SELECT '双人联机', '本地或在线双人协作与对战'
WHERE NOT EXISTS (SELECT 1 FROM categories WHERE name = '双人联机');

-- 标签
INSERT INTO tags (name)
SELECT '动作' WHERE NOT EXISTS (SELECT 1 FROM tags WHERE name = '动作');
INSERT INTO tags (name)
SELECT '速通' WHERE NOT EXISTS (SELECT 1 FROM tags WHERE name = '速通');
INSERT INTO tags (name)
SELECT '新手' WHERE NOT EXISTS (SELECT 1 FROM tags WHERE name = '新手');
INSERT INTO tags (name)
SELECT '高分' WHERE NOT EXISTS (SELECT 1 FROM tags WHERE name = '高分');
INSERT INTO tags (name)
SELECT '策略' WHERE NOT EXISTS (SELECT 1 FROM tags WHERE name = '策略');
INSERT INTO tags (name)
SELECT '双人' WHERE NOT EXISTS (SELECT 1 FROM tags WHERE name = '双人');

-- 文章
INSERT INTO articles (title, slug, summary, content_markdown, content_html, status, cover_image_url, view_count,
                      published_at, author_id, category_id, created_at, updated_at)
SELECT 'Neon Drift Rush 新手 3 天入门路线图',
       'neon-drift-rush-beginner-guide',
       '从按键习惯、路线选择到道具优先级，快速建立稳定上分节奏。',
       '# Neon Drift Rush 新手 3 天入门路线图\n\n本文提供基础操作、冲刺时机和容错练习清单。',
       '<article><h1>Neon Drift Rush 新手 3 天入门路线图</h1><p>本文提供基础操作、冲刺时机和容错练习清单。</p></article>',
       'PUBLISHED',
       'https://images.pexels.com/photos/442576/pexels-photo-442576.jpeg?auto=compress&cs=tinysrgb&w=1600',
       328,
       DATE_SUB(NOW(), INTERVAL 3 DAY),
       (SELECT id FROM users WHERE email = 'luna@psychegame.com'),
       (SELECT id FROM categories WHERE name = '动作竞技'),
       DATE_SUB(NOW(), INTERVAL 3 DAY),
       DATE_SUB(NOW(), INTERVAL 1 DAY)
WHERE NOT EXISTS (SELECT 1 FROM articles WHERE slug = 'neon-drift-rush-beginner-guide');

INSERT INTO articles (title, slug, summary, content_markdown, content_html, status, cover_image_url, view_count,
                      published_at, author_id, category_id, created_at, updated_at)
SELECT 'Block Garden 12 关高分路线：零氪也能上榜',
       'block-garden-high-score-route',
       '拆解每关资源分配与连消窗口，适合休闲玩家稳定拿高分。',
       '# Block Garden 12 关高分路线\n\n核心是节奏控制与连消触发窗口。',
       '<article><h1>Block Garden 12 关高分路线</h1><p>核心是节奏控制与连消触发窗口。</p></article>',
       'PUBLISHED',
       'https://images.pexels.com/photos/7915576/pexels-photo-7915576.jpeg?auto=compress&cs=tinysrgb&w=1600',
       214,
       DATE_SUB(NOW(), INTERVAL 2 DAY),
       (SELECT id FROM users WHERE email = 'ryan@psychegame.com'),
       (SELECT id FROM categories WHERE name = '休闲闯关'),
       DATE_SUB(NOW(), INTERVAL 2 DAY),
       DATE_SUB(NOW(), INTERVAL 12 HOUR)
WHERE NOT EXISTS (SELECT 1 FROM articles WHERE slug = 'block-garden-high-score-route');

INSERT INTO articles (title, slug, summary, content_markdown, content_html, status, cover_image_url, view_count,
                      published_at, author_id, category_id, created_at, updated_at)
SELECT '双人联机开黑房间设计手册（草稿）',
       'duo-online-room-design-draft',
       '讨论双人匹配逻辑、掉线重连和沟通效率的最小实践。',
       '# 双人联机开黑房间设计手册\n\n草稿阶段，持续补充。',
       '<article><h1>双人联机开黑房间设计手册</h1><p>草稿阶段，持续补充。</p></article>',
       'DRAFT',
       'https://images.pexels.com/photos/9072206/pexels-photo-9072206.jpeg?auto=compress&cs=tinysrgb&w=1600',
       37,
       NULL,
       (SELECT id FROM users WHERE email = 'luna@psychegame.com'),
       (SELECT id FROM categories WHERE name = '双人联机'),
       NOW(),
       NOW()
WHERE NOT EXISTS (SELECT 1 FROM articles WHERE slug = 'duo-online-room-design-draft');

-- 文章标签
INSERT INTO article_tags (article_id, tag_id)
SELECT a.id, t.id
FROM articles a
JOIN tags t ON t.name = '动作'
WHERE a.slug = 'neon-drift-rush-beginner-guide'
  AND NOT EXISTS (SELECT 1 FROM article_tags x WHERE x.article_id = a.id AND x.tag_id = t.id);

INSERT INTO article_tags (article_id, tag_id)
SELECT a.id, t.id
FROM articles a
JOIN tags t ON t.name = '速通'
WHERE a.slug = 'neon-drift-rush-beginner-guide'
  AND NOT EXISTS (SELECT 1 FROM article_tags x WHERE x.article_id = a.id AND x.tag_id = t.id);

INSERT INTO article_tags (article_id, tag_id)
SELECT a.id, t.id
FROM articles a
JOIN tags t ON t.name = '高分'
WHERE a.slug = 'block-garden-high-score-route'
  AND NOT EXISTS (SELECT 1 FROM article_tags x WHERE x.article_id = a.id AND x.tag_id = t.id);

INSERT INTO article_tags (article_id, tag_id)
SELECT a.id, t.id
FROM articles a
JOIN tags t ON t.name = '策略'
WHERE a.slug = 'block-garden-high-score-route'
  AND NOT EXISTS (SELECT 1 FROM article_tags x WHERE x.article_id = a.id AND x.tag_id = t.id);

INSERT INTO article_tags (article_id, tag_id)
SELECT a.id, t.id
FROM articles a
JOIN tags t ON t.name = '双人'
WHERE a.slug = 'duo-online-room-design-draft'
  AND NOT EXISTS (SELECT 1 FROM article_tags x WHERE x.article_id = a.id AND x.tag_id = t.id);

-- 评论与回复
INSERT INTO comments (article_id, user_id, parent_id, content, like_count, created_at, updated_at)
SELECT (SELECT id FROM articles WHERE slug = 'neon-drift-rush-beginner-guide'),
       (SELECT id FROM users WHERE email = 'ryan@psychegame.com'),
       NULL,
       '第三关冲刺点这个建议很关键，我首通时间缩短了接近 40%。',
       11,
       DATE_SUB(NOW(), INTERVAL 20 HOUR),
       DATE_SUB(NOW(), INTERVAL 20 HOUR)
WHERE NOT EXISTS (
  SELECT 1 FROM comments WHERE content = '第三关冲刺点这个建议很关键，我首通时间缩短了接近 40%。'
);

INSERT INTO comments (article_id, user_id, parent_id, content, like_count, created_at, updated_at)
SELECT (SELECT id FROM articles WHERE slug = 'neon-drift-rush-beginner-guide'),
       (SELECT id FROM users WHERE email = 'luna@psychegame.com'),
       (SELECT id FROM comments WHERE content = '第三关冲刺点这个建议很关键，我首通时间缩短了接近 40%。' LIMIT 1),
       '收到，我会补一版键位映射和移动端手势参数。',
       5,
       DATE_SUB(NOW(), INTERVAL 16 HOUR),
       DATE_SUB(NOW(), INTERVAL 16 HOUR)
WHERE NOT EXISTS (
  SELECT 1 FROM comments WHERE content = '收到，我会补一版键位映射和移动端手势参数。'
);

-- 收藏
INSERT INTO favorites (user_id, article_id, created_at)
SELECT (SELECT id FROM users WHERE email = 'luna@psychegame.com'),
       (SELECT id FROM articles WHERE slug = 'block-garden-high-score-route'),
       NOW()
WHERE NOT EXISTS (
  SELECT 1 FROM favorites
  WHERE user_id = (SELECT id FROM users WHERE email = 'luna@psychegame.com')
    AND article_id = (SELECT id FROM articles WHERE slug = 'block-garden-high-score-route')
);

-- 分享统计
INSERT INTO share_stats (article_id, platform, share_count, updated_at)
SELECT (SELECT id FROM articles WHERE slug = 'neon-drift-rush-beginner-guide'), 'wechat', 18, NOW()
WHERE NOT EXISTS (
  SELECT 1 FROM share_stats WHERE article_id = (SELECT id FROM articles WHERE slug = 'neon-drift-rush-beginner-guide') AND platform = 'wechat'
);

INSERT INTO share_stats (article_id, platform, share_count, updated_at)
SELECT (SELECT id FROM articles WHERE slug = 'block-garden-high-score-route'), 'link', 13, NOW()
WHERE NOT EXISTS (
  SELECT 1 FROM share_stats WHERE article_id = (SELECT id FROM articles WHERE slug = 'block-garden-high-score-route') AND platform = 'link'
);

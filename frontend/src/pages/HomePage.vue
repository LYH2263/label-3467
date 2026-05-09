<template>
  <section class="hero glass-card fade-in">
    <div class="hero-text">
      <p class="eyebrow">Psyche Game</p>
      <h1 class="page-title">像 4399 一样好逛，像创作平台一样好写。</h1>
      <p class="muted">
        发现热门小游戏、发布通关攻略、沉淀创作内容。每位作者都能实时看到自己的浏览量、点赞量和互动数据。
      </p>
      <div class="hero-actions">
        <el-button type="primary" size="large" @click="toCreate">发布攻略</el-button>
        <el-button size="large" @click="scrollToArticles">浏览文章</el-button>
      </div>
    </div>
    <div class="hero-stats">
      <article class="metric-card">
        <span>在线专题</span>
        <strong>{{ categories.length || 6 }}</strong>
      </article>
      <article class="metric-card">
        <span>文章总数</span>
        <strong>{{ total }}</strong>
      </article>
      <article class="metric-card">
        <span>今日推荐</span>
        <strong>{{ featuredGames.length }}</strong>
      </article>
    </div>
  </section>

  <section class="game-hall glass-card fade-in">
    <div class="section-head">
      <div>
        <h2 class="page-title">游戏广场</h2>
        <p class="muted">精选轻量游戏入口，优先展示高人气品类与高质量攻略。</p>
      </div>
      <div class="hall-tags">
        <button
          v-for="tag in gameTags"
          :key="tag"
          class="hall-tag"
          :class="{ active: activeTag === tag }"
          @click="activeTag = tag"
        >
          {{ tag }}
        </button>
      </div>
    </div>

    <div class="game-grid">
      <article v-for="game in filteredGames" :key="game.id" class="game-card">
        <img :src="game.image" :alt="game.title" />
        <div class="overlay"></div>
        <div class="game-body">
          <p>{{ game.genre }} · {{ game.difficulty }}</p>
          <h3>{{ game.title }}</h3>
          <div class="game-foot">
            <span>热度 {{ game.plays }}</span>
            <a :href="game.playUrl" target="_blank" rel="noreferrer">立即开玩</a>
          </div>
        </div>
      </article>
    </div>
  </section>

  <section id="articles" class="article-shell fade-in">
    <section class="filters glass-card">
      <el-form inline>
        <el-form-item>
          <el-input v-model="query.keyword" placeholder="搜索攻略、评测或活动" clearable @keyup.enter="search" />
        </el-form-item>
        <el-form-item>
          <el-select v-model="query.categoryId" clearable placeholder="游戏分区" style="width: 170px">
            <el-option v-for="item in categories" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-date-picker
            v-model="query.dateRange"
            type="daterange"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="search">筛选</el-button>
          <el-button @click="reset">重置</el-button>
        </el-form-item>
      </el-form>
    </section>

    <div class="article-main">
      <section class="content">
        <el-skeleton :loading="loading" animated :rows="6" v-if="loading" />

        <div class="grid" v-else>
          <article v-for="article in articles" :key="article.id" class="article-card glass-card">
            <img v-if="article.coverImageUrl" :src="article.coverImageUrl" class="cover" alt="cover" />
            <div class="article-body">
              <div class="meta-line">
                <el-tag size="small">{{ article.category }}</el-tag>
                <span>{{ formatTime(article.publishedAt || article.updatedAt) }}</span>
              </div>
              <h3 @click="openDetail(article.id)">{{ article.title }}</h3>
              <p>{{ article.summary }}</p>

              <div class="tags">
                <el-tag v-for="tag in article.tags" :key="tag" size="small" type="info" round># {{ tag }}</el-tag>
              </div>

              <div class="bottom-row">
                <span>{{ article.author }}</span>
                <div class="stat">
                  <span>👁 {{ article.viewCount }}</span>
                  <span>👍 {{ article.favoriteCount }}</span>
                  <span>💬 {{ article.commentCount }}</span>
                </div>
              </div>
            </div>
          </article>
        </div>

        <el-empty v-if="!loading && !articles.length" description="暂无攻略文章" />

        <el-pagination
          v-if="total > pageSize"
          class="pager"
          background
          layout="prev, pager, next"
          :total="total"
          :page-size="pageSize"
          :current-page="page + 1"
          @current-change="onPageChange"
        />
      </section>

      <aside class="ranking glass-card">
        <h3>本周热文榜</h3>
        <el-empty v-if="!ranking.length" description="暂无数据" :image-size="70" />
        <ul v-else>
          <li v-for="(item, index) in ranking" :key="item.id" @click="openDetail(item.id)">
            <span>{{ index + 1 }}</span>
            <div>
              <h4>{{ item.title }}</h4>
              <p>👁 {{ item.viewCount }} · 👍 {{ item.favoriteCount }}</p>
            </div>
          </li>
        </ul>
      </aside>
    </div>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import dayjs from 'dayjs'
import http from '../api/http'
import { useAuthStore } from '../stores/auth'

const auth = useAuthStore()
const router = useRouter()

const gameTags = ['全部', '动作', '休闲', '策略', '双人']
const activeTag = ref('全部')

const featuredGames = [
  {
    id: 1,
    title: 'Neon Drift Rush',
    genre: '动作',
    difficulty: '中等',
    plays: '182k',
    tag: '动作',
    playUrl: 'https://poki.com/zh/g/stickman-hook',
    image: 'https://images.pexels.com/photos/442576/pexels-photo-442576.jpeg?auto=compress&cs=tinysrgb&w=1600'
  },
  {
    id: 2,
    title: 'Block Garden',
    genre: '休闲',
    difficulty: '简单',
    plays: '96k',
    tag: '休闲',
    playUrl: 'https://poki.com/zh/g/subway-surfers',
    image: 'https://images.pexels.com/photos/7915576/pexels-photo-7915576.jpeg?auto=compress&cs=tinysrgb&w=1600'
  },
  {
    id: 3,
    title: 'Mecha Frontier',
    genre: '策略',
    difficulty: '困难',
    plays: '73k',
    tag: '策略',
    playUrl: 'https://poki.com/zh/g/merge-round-racers',
    image: 'https://images.pexels.com/photos/1293261/pexels-photo-1293261.jpeg?auto=compress&cs=tinysrgb&w=1600'
  },
  {
    id: 4,
    title: 'Pixel Clash Duo',
    genre: '双人',
    difficulty: '中等',
    plays: '68k',
    tag: '双人',
    playUrl: 'https://poki.com/zh/g/12-mini-battles',
    image: 'https://images.pexels.com/photos/7862601/pexels-photo-7862601.jpeg?auto=compress&cs=tinysrgb&w=1600'
  },
  {
    id: 5,
    title: 'Skyline Brawl',
    genre: '动作',
    difficulty: '困难',
    plays: '51k',
    tag: '动作',
    playUrl: 'https://poki.com/zh/g/smash-karts',
    image: 'https://images.pexels.com/photos/3165335/pexels-photo-3165335.jpeg?auto=compress&cs=tinysrgb&w=1600'
  },
  {
    id: 6,
    title: 'Candy Workshop',
    genre: '休闲',
    difficulty: '简单',
    plays: '47k',
    tag: '休闲',
    playUrl: 'https://poki.com/zh/g/temple-run-2',
    image: 'https://images.pexels.com/photos/9072206/pexels-photo-9072206.jpeg?auto=compress&cs=tinysrgb&w=1600'
  }
]

const filteredGames = computed(() => {
  if (activeTag.value === '全部') return featuredGames
  return featuredGames.filter((item) => item.tag === activeTag.value)
})

const categories = ref([])
const loading = ref(false)
const articles = ref([])
const total = ref(0)
const page = ref(0)
const pageSize = 9

const query = reactive({
  keyword: '',
  categoryId: undefined,
  dateRange: []
})

const ranking = computed(() => {
  return [...articles.value]
    .sort((a, b) => (b.viewCount || 0) - (a.viewCount || 0))
    .slice(0, 5)
})

const loadCategories = async () => {
  const res = await http.get('/categories')
  categories.value = res.data
}

const loadArticles = async () => {
  loading.value = true
  try {
    const params = {
      page: page.value,
      size: pageSize,
      keyword: query.keyword || undefined,
      categoryId: query.categoryId || undefined,
      startDate: query.dateRange?.[0] || undefined,
      endDate: query.dateRange?.[1] || undefined
    }
    const res = await http.get('/articles', { params })
    articles.value = res.data.content
    total.value = res.data.totalElements
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    loading.value = false
  }
}

const search = () => {
  page.value = 0
  loadArticles()
}

const reset = () => {
  query.keyword = ''
  query.categoryId = undefined
  query.dateRange = []
  search()
}

const onPageChange = (value) => {
  page.value = value - 1
  loadArticles()
}

const openDetail = (id) => {
  router.push(`/articles/${id}`)
}

const formatTime = (value) => (value ? dayjs(value).format('YYYY-MM-DD HH:mm') : '-')

const toCreate = () => {
  if (!auth.isAuthenticated) {
    router.push('/login')
    return
  }
  router.push('/editor')
}

const scrollToArticles = () => {
  document.getElementById('articles')?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

onMounted(async () => {
  try {
    await Promise.all([loadCategories(), loadArticles()])
  } catch (error) {
    ElMessage.error(error.message)
  }
})
</script>

<style scoped>
.hero {
  margin-bottom: 16px;
  padding: 28px;
  display: grid;
  grid-template-columns: minmax(0, 1fr) 280px;
  gap: 18px;
}

.hero-text .eyebrow {
  margin: 0;
  font-size: 12px;
  letter-spacing: 0.26em;
  text-transform: uppercase;
  color: var(--brand);
}

.hero-text h1 {
  margin: 10px 0 12px;
  font-size: clamp(28px, 4vw, 46px);
  line-height: 1.18;
}

.hero-actions {
  margin-top: 18px;
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.hero-stats {
  display: grid;
  gap: 10px;
}

.metric-card {
  border-radius: var(--radius-md);
  padding: 14px;
  background: var(--chip-bg);
  border: 1px solid var(--card-border);
  box-shadow: var(--shadow-card);
}

.metric-card span {
  font-size: 13px;
  color: var(--text-secondary);
}

.metric-card strong {
  display: block;
  margin-top: 10px;
  font-size: 30px;
}

.game-hall {
  padding: 20px;
  margin-bottom: 16px;
}

.section-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: flex-end;
  margin-bottom: 14px;
}

.section-head h2 {
  margin: 0;
  font-size: 30px;
}

.section-head p {
  margin: 6px 0 0;
}

.hall-tags {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.hall-tag {
  border: 1px solid var(--card-border);
  border-radius: 999px;
  padding: 8px 14px;
  background: var(--chip-bg);
  color: var(--text-primary);
  cursor: pointer;
  transition: all 0.2s ease;
}

.hall-tag.active,
.hall-tag:hover {
  background: var(--brand);
  color: #fff;
  border-color: transparent;
}

.game-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}

.game-card {
  position: relative;
  border-radius: 20px;
  overflow: hidden;
  min-height: 226px;
  transform: translateZ(0);
  transition: transform 0.26s ease;
}

.game-card:hover {
  transform: translateY(-5px) scale(1.01);
}

.game-card img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.overlay {
  position: absolute;
  inset: 0;
  background: linear-gradient(180deg, rgba(0, 0, 0, 0.12), rgba(0, 0, 0, 0.7));
}

.game-body {
  position: absolute;
  left: 14px;
  right: 14px;
  bottom: 14px;
  color: #fff;
}

.game-body p {
  margin: 0;
  font-size: 12px;
  opacity: 0.92;
}

.game-body h3 {
  margin: 6px 0 0;
  font-size: 24px;
}

.game-foot {
  margin-top: 8px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.game-foot a {
  color: #fff;
  font-weight: 600;
}

.article-shell {
  display: grid;
  gap: 14px;
}

.filters {
  padding: 14px;
}

.article-main {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 300px;
  gap: 14px;
}

.content {
  display: grid;
  gap: 14px;
}

.grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.article-card {
  overflow: hidden;
  transition: transform 0.2s ease;
}

.article-card:hover {
  transform: translateY(-3px);
}

.cover {
  width: 100%;
  height: 168px;
  object-fit: cover;
}

.article-body {
  padding: 14px;
}

.meta-line {
  display: flex;
  justify-content: space-between;
  color: var(--text-secondary);
  font-size: 12px;
}

.article-body h3 {
  margin: 12px 0 8px;
  font-size: 22px;
  cursor: pointer;
}

.article-body h3:hover {
  color: var(--brand);
}

.article-body p {
  color: var(--text-secondary);
  min-height: 44px;
}

.tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin: 10px 0 12px;
}

.bottom-row {
  display: flex;
  justify-content: space-between;
  font-size: 13px;
  color: var(--text-secondary);
}

.stat {
  display: flex;
  gap: 8px;
}

.ranking {
  padding: 16px;
  height: fit-content;
}

.ranking h3 {
  margin: 0 0 12px;
}

.ranking ul {
  margin: 0;
  padding: 0;
  list-style: none;
  display: grid;
  gap: 8px;
}

.ranking li {
  display: grid;
  grid-template-columns: 24px minmax(0, 1fr);
  gap: 8px;
  border: 1px solid var(--card-border);
  border-radius: 12px;
  padding: 10px;
  cursor: pointer;
  transition: transform 0.2s ease;
}

.ranking li:hover {
  transform: translateX(4px);
}

.ranking li span {
  color: var(--brand);
  font-weight: 700;
}

.ranking h4 {
  margin: 0;
  font-size: 14px;
}

.ranking p {
  margin: 5px 0 0;
  color: var(--text-secondary);
  font-size: 12px;
}

.pager {
  justify-content: center;
  margin-top: 6px;
}

@media (max-width: 1080px) {
  .hero {
    grid-template-columns: 1fr;
  }

  .article-main {
    grid-template-columns: 1fr;
  }

  .ranking {
    order: -1;
  }
}

@media (max-width: 860px) {
  .game-grid,
  .grid {
    grid-template-columns: 1fr;
  }

  .section-head {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>

<template>
  <section class="page-card fade-in">
    <div class="head glass-card">
      <div>
        <h2 class="page-title">创作数据中心</h2>
        <p class="muted">查看我发布文章的浏览、点赞、评论趋势与内容表现。</p>
      </div>
      <div class="head-actions">
        <el-button @click="exportAnalytics">导出分析报表</el-button>
        <el-button type="primary" @click="router.push('/editor')">发布新文章</el-button>
      </div>
    </div>

    <div class="metrics-grid">
      <article class="metric glass-card">
        <span>总浏览量</span>
        <strong>{{ metrics.totalViews }}</strong>
      </article>
      <article class="metric glass-card">
        <span>总点赞量</span>
        <strong>{{ metrics.totalLikes }}</strong>
      </article>
      <article class="metric glass-card">
        <span>总评论数</span>
        <strong>{{ metrics.totalComments }}</strong>
      </article>
      <article class="metric glass-card">
        <span>已发布文章</span>
        <strong>{{ metrics.publishedCount }}</strong>
      </article>
    </div>

    <div class="content-grid">
      <section class="glass-card table-card">
        <el-table :data="articles" v-loading="loading" border>
          <el-table-column prop="title" label="标题" min-width="220" />
          <el-table-column prop="category" label="分类" width="120" />
          <el-table-column prop="status" label="状态" width="96">
            <template #default="scope">
              <el-tag :type="scope.row.status === 'PUBLISHED' ? 'success' : 'warning'">
                {{ scope.row.status === 'PUBLISHED' ? '已发布' : '草稿' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="数据表现" width="220">
            <template #default="scope">
              <div class="stat-line">
                <span>👁 {{ scope.row.viewCount }}</span>
                <span>👍 {{ scope.row.favoriteCount }}</span>
                <span>💬 {{ scope.row.commentCount }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="250" fixed="right">
            <template #default="scope">
              <el-button size="small" @click="edit(scope.row.id)">编辑</el-button>
              <el-button size="small" @click="viewRevisions(scope.row.id)">历史</el-button>
              <el-popconfirm title="确认删除该文章？" @confirm="remove(scope.row.id)">
                <template #reference>
                  <el-button size="small" type="danger" plain>删除</el-button>
                </template>
              </el-popconfirm>
            </template>
          </el-table-column>
        </el-table>

        <el-pagination
          class="pager"
          background
          layout="prev, pager, next"
          :total="total"
          :page-size="pageSize"
          :current-page="page + 1"
          @current-change="changePage"
        />
      </section>

      <aside class="glass-card insight-card">
        <h3>文章热度排行</h3>
        <el-empty v-if="!topArticles.length" description="暂无数据" :image-size="70" />
        <ul v-else>
          <li v-for="(item, index) in topArticles" :key="item.id" @click="router.push(`/articles/${item.id}`)">
            <div class="rank">{{ index + 1 }}</div>
            <div class="info">
              <h4>{{ item.title }}</h4>
              <p>浏览 {{ item.viewCount }} · 点赞 {{ item.favoriteCount }}</p>
              <el-progress :percentage="item.heat" :stroke-width="8" :show-text="false" />
            </div>
          </li>
        </ul>
      </aside>
    </div>
  </section>

  <el-drawer v-model="historyVisible" title="修改历史记录" size="42%">
    <el-timeline>
      <el-timeline-item v-for="item in revisions" :key="item.id" :timestamp="formatTime(item.createdAt)">
        <h4>{{ item.title }}</h4>
        <p><strong>变更说明:</strong> {{ item.changeNote || '内容更新' }}</p>
        <p><strong>修改人:</strong> {{ item.createdBy }}</p>
      </el-timeline-item>
    </el-timeline>
    <el-empty v-if="!revisions.length" description="暂无历史版本" />
  </el-drawer>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import dayjs from 'dayjs'
import http from '../api/http'

const router = useRouter()

const loading = ref(false)
const articles = ref([])
const total = ref(0)
const page = ref(0)
const pageSize = 10

const historyVisible = ref(false)
const revisions = ref([])

const metrics = computed(() => {
  const totalViews = articles.value.reduce((sum, item) => sum + (item.viewCount || 0), 0)
  const totalLikes = articles.value.reduce((sum, item) => sum + (item.favoriteCount || 0), 0)
  const totalComments = articles.value.reduce((sum, item) => sum + (item.commentCount || 0), 0)
  const publishedCount = articles.value.filter((item) => item.status === 'PUBLISHED').length
  return {
    totalViews,
    totalLikes,
    totalComments,
    publishedCount
  }
})

const topArticles = computed(() => {
  const maxHeat = Math.max(
    ...articles.value.map((item) => (item.viewCount || 0) + (item.favoriteCount || 0) * 4 + (item.commentCount || 0) * 2),
    1
  )
  return [...articles.value]
    .map((item) => {
      const score = (item.viewCount || 0) + (item.favoriteCount || 0) * 4 + (item.commentCount || 0) * 2
      return {
        ...item,
        heat: Math.max(6, Math.round((score / maxHeat) * 100))
      }
    })
    .sort((a, b) => b.heat - a.heat)
    .slice(0, 5)
})

const loadMine = async () => {
  loading.value = true
  try {
    const res = await http.get('/articles/mine', {
      params: {
        page: page.value,
        size: pageSize
      }
    })
    articles.value = res.data.content
    total.value = res.data.totalElements
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    loading.value = false
  }
}

const edit = (id) => router.push(`/editor/${id}`)

const remove = async (id) => {
  try {
    await http.delete(`/articles/${id}`)
    ElMessage.success('删除成功')
    await loadMine()
  } catch (error) {
    ElMessage.error(error.message)
  }
}

const viewRevisions = async (id) => {
  try {
    const res = await http.get(`/articles/${id}/revisions`)
    revisions.value = res.data
    historyVisible.value = true
  } catch (error) {
    ElMessage.error(error.message)
  }
}

const exportAnalytics = async () => {
  try {
    const response = await http.get('/articles/mine/analytics/export', {
      responseType: 'blob'
    })
    const file = new Blob([response], {
      type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
    })
    const url = URL.createObjectURL(file)
    const link = document.createElement('a')
    link.href = url
    link.download = 'psyche-game-analytics.xlsx'
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    URL.revokeObjectURL(url)
    ElMessage.success('报表导出成功')
  } catch (error) {
    ElMessage.error(error.message)
  }
}

const changePage = (value) => {
  page.value = value - 1
  loadMine()
}

const formatTime = (value) => dayjs(value).format('YYYY-MM-DD HH:mm')

onMounted(loadMine)
</script>

<style scoped>
.page-card {
  display: grid;
  gap: 14px;
}

.head {
  padding: 18px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.head h2 {
  margin: 0;
  font-size: 30px;
}

.head-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.head p {
  margin: 6px 0 0;
}

.metrics-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
}

.metric {
  padding: 14px;
}

.metric span {
  color: var(--text-secondary);
  font-size: 13px;
}

.metric strong {
  display: block;
  margin-top: 8px;
  font-size: 28px;
}

.content-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 320px;
  gap: 12px;
}

.table-card {
  padding: 12px;
}

.stat-line {
  display: flex;
  gap: 12px;
  font-size: 13px;
}

.pager {
  margin-top: 14px;
  justify-content: center;
}

.insight-card {
  padding: 14px;
}

.insight-card h3 {
  margin: 2px 0 10px;
}

.insight-card ul {
  margin: 0;
  padding: 0;
  list-style: none;
  display: grid;
  gap: 10px;
}

.insight-card li {
  border: 1px solid var(--card-border);
  border-radius: 14px;
  padding: 10px;
  display: grid;
  grid-template-columns: 24px minmax(0, 1fr);
  gap: 8px;
  cursor: pointer;
}

.rank {
  color: var(--brand);
  font-weight: 700;
}

.info h4 {
  margin: 0;
  font-size: 14px;
}

.info p {
  margin: 5px 0 8px;
  color: var(--text-secondary);
  font-size: 12px;
}

@media (max-width: 1080px) {
  .metrics-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .content-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 720px) {
  .metrics-grid {
    grid-template-columns: 1fr;
  }

  .head {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>

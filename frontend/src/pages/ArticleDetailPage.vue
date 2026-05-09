<template>
  <div v-if="loading" class="glass-card loading-box">
    <el-skeleton animated :rows="8" />
  </div>

  <div v-else-if="detail" class="detail-layout fade-in">
    <section class="main-col glass-card">
      <img v-if="detail.article.coverImageUrl" :src="detail.article.coverImageUrl" class="hero-img" alt="cover" />
      <div class="article-head">
        <el-tag effect="dark">{{ detail.article.category }}</el-tag>
        <h1>{{ detail.article.title }}</h1>
        <div class="meta">
          <span>{{ detail.article.author }}</span>
          <span>{{ formatTime(detail.article.publishedAt || detail.article.updatedAt) }}</span>
          <span>👁 {{ detail.article.viewCount }}</span>
        </div>
      </div>

      <article class="content" v-html="detail.contentHtml"></article>

      <div class="action-row">
        <el-button :type="detail.favorited ? 'warning' : 'default'" @click="toggleFavorite">
          {{ detail.favorited ? '已点赞' : '点赞攻略' }}
        </el-button>
        <el-dropdown @command="shareArticle">
          <el-button type="primary">分享文章</el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="wechat">微信</el-dropdown-item>
              <el-dropdown-item command="weibo">微博</el-dropdown-item>
              <el-dropdown-item command="link">链接</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>

      <section class="comment-section">
        <h3>玩家评论区</h3>

        <el-form @submit.prevent>
          <el-input
            v-model="commentForm.content"
            type="textarea"
            :rows="3"
            placeholder="写下你的通关心得..."
            maxlength="1500"
            show-word-limit
          />
          <div class="comment-actions">
            <el-button type="primary" :loading="commentLoading" @click="submitComment">发表评论</el-button>
          </div>
        </el-form>

        <div class="comment-list">
          <div class="comment-item" v-for="comment in comments" :key="comment.id">
            <div class="comment-top">
              <div class="author">
                <el-avatar :size="30" :src="comment.avatarUrl || ''">{{ comment.username.slice(0, 1).toUpperCase() }}</el-avatar>
                <div>
                  <strong>{{ comment.username }}</strong>
                  <p>{{ formatTime(comment.createdAt) }}</p>
                </div>
              </div>
              <div class="buttons">
                <el-button text @click="likeComment(comment.id)">👍 {{ comment.likeCount }}</el-button>
                <el-button text @click="setReply(comment.id)">回复</el-button>
              </div>
            </div>
            <p>{{ comment.content }}</p>

            <div v-if="replyParentId === comment.id" class="reply-box">
              <el-input v-model="replyContent" type="textarea" :rows="2" placeholder="回复内容" />
              <div class="reply-actions">
                <el-button size="small" @click="replyParentId = null">取消</el-button>
                <el-button size="small" type="primary" @click="submitReply(comment.id)">发送回复</el-button>
              </div>
            </div>

            <div v-for="reply in comment.replies" :key="reply.id" class="reply-item">
              <div class="comment-top">
                <div class="author">
                  <el-avatar :size="26" :src="reply.avatarUrl || ''">{{ reply.username.slice(0, 1).toUpperCase() }}</el-avatar>
                  <div>
                    <strong>{{ reply.username }}</strong>
                    <p>{{ formatTime(reply.createdAt) }}</p>
                  </div>
                </div>
                <el-button text @click="likeComment(reply.id)">👍 {{ reply.likeCount }}</el-button>
              </div>
              <p>{{ reply.content }}</p>
            </div>
          </div>
        </div>
      </section>
    </section>

    <aside class="side-col">
      <section class="glass-card panel">
        <h4>同类攻略推荐</h4>
        <el-empty v-if="!relatedArticles.length" description="暂无推荐" :image-size="72" />
        <div v-else class="related-list">
          <a
            v-for="item in relatedArticles"
            :key="item.id"
            @click.prevent="openArticle(item.id)"
            href="#"
            class="related-item"
          >
            <h5>{{ item.title }}</h5>
            <p>{{ item.summary }}</p>
          </a>
        </div>
      </section>

      <section class="glass-card panel">
        <h4>分享统计</h4>
        <div class="share-list" v-if="detail.shareStats?.length">
          <div class="share-item" v-for="item in detail.shareStats" :key="item.platform">
            <span>{{ item.platform }}</span>
            <strong>{{ item.shareCount }}</strong>
          </div>
        </div>
        <el-empty v-else description="暂无分享数据" :image-size="72" />
      </section>
    </aside>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import dayjs from 'dayjs'
import http from '../api/http'
import { useAuthStore } from '../stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const loading = ref(false)
const detail = ref(null)
const comments = ref([])
const relatedArticles = ref([])

const commentLoading = ref(false)
const commentForm = reactive({ content: '' })
const replyParentId = ref(null)
const replyContent = ref('')

const formatTime = (value) => (value ? dayjs(value).format('YYYY-MM-DD HH:mm') : '-')

const loadDetail = async () => {
  loading.value = true
  try {
    const [detailRes, commentsRes, relatedRes] = await Promise.all([
      http.get(`/articles/${route.params.id}`),
      http.get(`/articles/${route.params.id}/comments`),
      http.get(`/articles/${route.params.id}/related`)
    ])
    detail.value = detailRes.data
    comments.value = commentsRes.data
    relatedArticles.value = relatedRes.data
  } catch (error) {
    ElMessage.error(error.message)
    router.push('/')
  } finally {
    loading.value = false
  }
}

const ensureLogin = () => {
  if (!auth.isAuthenticated) {
    ElMessage.warning('请先登录')
    router.push(`/login?redirect=${encodeURIComponent(route.fullPath)}`)
    return false
  }
  return true
}

const toggleFavorite = async () => {
  if (!ensureLogin()) return
  try {
    const res = await http.post(`/articles/${route.params.id}/favorite`)
    detail.value.favorited = res.data.favorited
    detail.value.article.favoriteCount = res.data.favoriteCount
    ElMessage.success(detail.value.favorited ? '点赞成功' : '取消点赞成功')
  } catch (error) {
    ElMessage.error(error.message)
  }
}

const shareArticle = async (platform) => {
  try {
    const res = await http.post(`/articles/${route.params.id}/share`, null, { params: { platform } })
    ElMessage.success(`已分享至 ${res.data.platform}`)
    await loadDetail()
  } catch (error) {
    ElMessage.error(error.message)
  }
}

const submitComment = async () => {
  if (!ensureLogin()) return
  if (!commentForm.content.trim()) {
    ElMessage.warning('请输入评论内容')
    return
  }

  commentLoading.value = true
  try {
    await http.post(`/articles/${route.params.id}/comments`, { content: commentForm.content })
    commentForm.content = ''
    await loadDetail()
    ElMessage.success('评论成功')
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    commentLoading.value = false
  }
}

const setReply = (commentId) => {
  replyParentId.value = commentId
  replyContent.value = ''
}

const submitReply = async (parentId) => {
  if (!ensureLogin()) return
  if (!replyContent.value.trim()) {
    ElMessage.warning('回复内容不能为空')
    return
  }

  try {
    await http.post(`/articles/${route.params.id}/comments`, {
      parentId,
      content: replyContent.value
    })
    replyContent.value = ''
    replyParentId.value = null
    await loadDetail()
    ElMessage.success('回复成功')
  } catch (error) {
    ElMessage.error(error.message)
  }
}

const likeComment = async (commentId) => {
  try {
    await http.post(`/comments/${commentId}/like`)
    await loadDetail()
  } catch (error) {
    ElMessage.error(error.message)
  }
}

const openArticle = (id) => {
  router.push(`/articles/${id}`)
}

watch(
  () => route.params.id,
  () => {
    loadDetail()
  }
)

onMounted(loadDetail)
</script>

<style scoped>
.loading-box {
  padding: 24px;
}

.detail-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 320px;
  gap: 16px;
}

.main-col {
  padding: 0;
  overflow: hidden;
}

.hero-img {
  width: 100%;
  height: 280px;
  object-fit: cover;
}

.article-head {
  padding: 18px 24px;
  border-bottom: 1px solid var(--card-border);
}

.article-head h1 {
  margin: 10px 0;
  font-size: 34px;
}

.meta {
  display: flex;
  gap: 12px;
  color: var(--text-secondary);
}

.content {
  padding: 24px;
  color: var(--text-primary);
  line-height: 1.75;
}

.content :deep(img) {
  max-width: 100%;
  border-radius: 10px;
}

.action-row {
  padding: 0 24px 20px;
  display: flex;
  gap: 10px;
}

.comment-section {
  padding: 0 24px 26px;
}

.comment-section h3 {
  margin-top: 4px;
}

.comment-actions {
  margin-top: 10px;
  text-align: right;
}

.comment-list {
  margin-top: 16px;
  display: grid;
  gap: 12px;
}

.comment-item {
  border: 1px solid var(--card-border);
  border-radius: var(--radius-md);
  padding: 12px;
  background: rgba(255, 255, 255, 0.65);
}

.comment-top {
  display: flex;
  justify-content: space-between;
  gap: 10px;
}

.author {
  display: flex;
  align-items: center;
  gap: 10px;
}

.author p {
  margin: 2px 0 0;
  font-size: 12px;
  color: var(--text-secondary);
}

.reply-item {
  margin-top: 10px;
  margin-left: 20px;
  padding: 10px;
  border-left: 2px solid var(--card-border);
  background: rgba(255, 255, 255, 0.45);
  border-radius: 8px;
}

.reply-box {
  margin-top: 8px;
}

.reply-actions {
  margin-top: 8px;
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

.side-col {
  display: grid;
  gap: 14px;
  align-content: start;
}

.panel {
  padding: 16px;
}

.panel h4 {
  margin: 4px 0 12px;
}

.related-list {
  display: grid;
  gap: 10px;
}

.related-item {
  padding: 10px;
  border-radius: 10px;
  border: 1px solid var(--card-border);
  transition: transform 0.2s ease;
}

.related-item:hover {
  transform: translateY(-2px);
}

.related-item h5 {
  margin: 0 0 6px;
}

.related-item p {
  margin: 0;
  font-size: 13px;
  color: var(--text-secondary);
}

.share-list {
  display: grid;
  gap: 8px;
}

.share-item {
  display: flex;
  justify-content: space-between;
  padding: 8px 10px;
  border-radius: 8px;
  border: 1px solid var(--card-border);
}

@media (max-width: 1000px) {
  .detail-layout {
    grid-template-columns: 1fr;
  }

  .hero-img {
    height: 220px;
  }
}
</style>

<template>
  <div class="profile-grid fade-in">
    <section class="glass-card profile-card">
      <div class="head">
        <el-avatar :size="68" :src="profile.avatarUrl || ''">{{ profile.username?.slice(0, 1)?.toUpperCase() }}</el-avatar>
        <div>
          <h2>{{ profile.username }}</h2>
          <p>{{ profile.email }}</p>
          <el-tag :type="profile.emailVerified ? 'success' : 'warning'">{{ profile.emailVerified ? '邮箱已验证' : '邮箱未验证' }}</el-tag>
        </div>
      </div>

      <el-tabs>
        <el-tab-pane label="资料设置">
          <el-form :model="profileForm" label-position="top" class="form">
            <el-form-item label="用户名">
              <el-input v-model="profileForm.username" />
            </el-form-item>
            <el-form-item label="个人简介">
              <el-input v-model="profileForm.bio" type="textarea" :rows="4" maxlength="1000" show-word-limit />
            </el-form-item>
            <el-button type="primary" :loading="loading" @click="updateProfile">保存资料</el-button>
          </el-form>
        </el-tab-pane>

        <el-tab-pane label="头像上传">
          <el-upload :auto-upload="false" :show-file-list="false" :on-change="onFileChange">
            <el-button type="primary">选择图片</el-button>
          </el-upload>
          <p class="tip">支持 JPG/PNG，建议 1:1 比例</p>
        </el-tab-pane>

        <el-tab-pane label="安全设置">
          <el-form :model="passwordForm" label-position="top" class="form">
            <el-form-item label="旧密码">
              <el-input v-model="passwordForm.oldPassword" type="password" show-password />
            </el-form-item>
            <el-form-item label="新密码">
              <el-input v-model="passwordForm.newPassword" type="password" show-password />
            </el-form-item>
            <el-button type="primary" :loading="passwordLoading" @click="updatePassword">修改密码</el-button>
          </el-form>
        </el-tab-pane>
      </el-tabs>
    </section>

    <section class="glass-card favorite-card">
      <h3>我的收藏</h3>
      <el-empty v-if="!favorites.length" description="暂无收藏" :image-size="70" />
      <div class="favorite-list" v-else>
        <article v-for="item in favorites" :key="item.id" class="fav-item" @click="openArticle(item.id)">
          <h4>{{ item.title }}</h4>
          <p>{{ item.summary }}</p>
          <div class="row">
            <span>{{ item.category }}</span>
            <span>💬 {{ item.commentCount }} · ⭐ {{ item.favoriteCount }}</span>
          </div>
        </article>
      </div>
    </section>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import http from '../api/http'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const auth = useAuthStore()

const profile = reactive({})
const profileForm = reactive({ username: '', bio: '' })
const passwordForm = reactive({ oldPassword: '', newPassword: '' })

const loading = ref(false)
const passwordLoading = ref(false)
const favorites = ref([])

const loadProfile = async () => {
  const res = await http.get('/users/me')
  Object.assign(profile, res.data)
  profileForm.username = res.data.username
  profileForm.bio = res.data.bio || ''
  auth.user = res.data
  localStorage.setItem('psyche_user', JSON.stringify(res.data))
}

const loadFavorites = async () => {
  const res = await http.get('/users/me/favorites')
  favorites.value = res.data
}

const updateProfile = async () => {
  loading.value = true
  try {
    await http.put('/users/me', profileForm)
    await loadProfile()
    ElMessage.success('资料已更新')
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    loading.value = false
  }
}

const updatePassword = async () => {
  passwordLoading.value = true
  try {
    await http.put('/users/me/password', passwordForm)
    passwordForm.oldPassword = ''
    passwordForm.newPassword = ''
    ElMessage.success('密码修改成功')
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    passwordLoading.value = false
  }
}

const onFileChange = async (uploadFile) => {
  const formData = new FormData()
  formData.append('file', uploadFile.raw)
  try {
    await http.post('/users/me/avatar', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    await loadProfile()
    ElMessage.success('头像上传成功')
  } catch (error) {
    ElMessage.error(error.message)
  }
}

const openArticle = (id) => {
  router.push(`/articles/${id}`)
}

onMounted(async () => {
  try {
    await Promise.all([loadProfile(), loadFavorites()])
  } catch (error) {
    ElMessage.error(error.message)
  }
})
</script>

<style scoped>
.profile-grid {
  display: grid;
  gap: 16px;
  grid-template-columns: minmax(0, 1fr) 340px;
}

.profile-card,
.favorite-card {
  padding: 20px;
}

.head {
  display: flex;
  gap: 12px;
  align-items: center;
  margin-bottom: 18px;
}

.head h2 {
  margin: 0;
}

.head p {
  margin: 4px 0 6px;
  color: var(--text-secondary);
}

.form {
  max-width: 560px;
}

.tip {
  margin-top: 10px;
  color: var(--text-secondary);
}

.favorite-card h3 {
  margin: 4px 0 14px;
}

.favorite-list {
  display: grid;
  gap: 10px;
}

.fav-item {
  padding: 12px;
  border-radius: 12px;
  border: 1px solid var(--card-border);
  cursor: pointer;
  transition: transform 0.2s ease;
}

.fav-item:hover {
  transform: translateY(-2px);
}

.fav-item h4 {
  margin: 0;
}

.fav-item p {
  color: var(--text-secondary);
  font-size: 13px;
}

.row {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: var(--text-secondary);
}

@media (max-width: 1000px) {
  .profile-grid {
    grid-template-columns: 1fr;
  }
}
</style>

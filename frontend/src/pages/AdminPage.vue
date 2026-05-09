<template>
  <section class="glass-card page-card fade-in">
    <div class="head">
      <div>
        <h2>管理员后台</h2>
        <p>统一管理全站文章与用户账号权限。</p>
      </div>
      <el-button @click="refreshAll">刷新数据</el-button>
    </div>

    <el-tabs v-model="activeTab">
      <el-tab-pane label="文章管理" name="articles">
        <el-table :data="articles" v-loading="articleLoading" border>
          <el-table-column prop="title" label="标题" min-width="220" />
          <el-table-column prop="author" label="作者" width="120" />
          <el-table-column prop="category" label="分类" width="120" />
          <el-table-column prop="status" label="状态" width="110">
            <template #default="scope">
              <el-tag :type="scope.row.status === 'PUBLISHED' ? 'success' : 'warning'">{{ scope.row.status }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="更新时间" width="170">
            <template #default="scope">
              {{ formatTime(scope.row.updatedAt) }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="230" fixed="right">
            <template #default="scope">
              <el-button size="small" @click="viewArticle(scope.row.id)">查看</el-button>
              <el-button size="small" @click="editArticle(scope.row.id)">编辑</el-button>
              <el-popconfirm title="确认删除该文章？" @confirm="removeArticle(scope.row.id)">
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
          :total="articleTotal"
          :page-size="articlePageSize"
          :current-page="articlePage + 1"
          @current-change="changeArticlePage"
        />
      </el-tab-pane>

      <el-tab-pane label="用户管理" name="users">
        <el-table :data="users" v-loading="userLoading" border>
          <el-table-column prop="username" label="用户名" min-width="130" />
          <el-table-column prop="email" label="邮箱" min-width="200" />
          <el-table-column label="角色" width="170">
            <template #default="scope">
              <el-select
                v-model="roleDrafts[scope.row.id]"
                size="small"
                :disabled="isCurrentUser(scope.row)"
                style="width: 140px"
              >
                <el-option v-for="item in roleOptions" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="邮箱验证" width="110">
            <template #default="scope">
              <el-tag :type="scope.row.emailVerified ? 'success' : 'info'">
                {{ scope.row.emailVerified ? '已验证' : '未验证' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="注册时间" width="170">
            <template #default="scope">
              {{ formatTime(scope.row.createdAt) }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="220" fixed="right">
            <template #default="scope">
              <el-button
                size="small"
                type="primary"
                plain
                :disabled="isCurrentUser(scope.row)"
                @click="updateRole(scope.row)"
              >
                保存角色
              </el-button>
              <el-popconfirm title="确认删除该用户？" @confirm="removeUser(scope.row)">
                <template #reference>
                  <el-button size="small" type="danger" plain :disabled="isCurrentUser(scope.row)">删除</el-button>
                </template>
              </el-popconfirm>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>
  </section>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import dayjs from 'dayjs'
import http from '../api/http'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const auth = useAuthStore()

const activeTab = ref('articles')

const articleLoading = ref(false)
const articles = ref([])
const articleTotal = ref(0)
const articlePage = ref(0)
const articlePageSize = 10

const userLoading = ref(false)
const users = ref([])
const roleDrafts = reactive({})
const roleOptions = [
  { label: '管理员', value: 'ROLE_ADMIN' },
  { label: '普通用户', value: 'ROLE_USER' }
]

const formatTime = (value) => (value ? dayjs(value).format('YYYY-MM-DD HH:mm') : '-')
const isCurrentUser = (user) => auth.user?.id === user.id

const loadArticles = async () => {
  articleLoading.value = true
  try {
    const res = await http.get('/admin/articles', {
      params: {
        page: articlePage.value,
        size: articlePageSize
      }
    })
    articles.value = res.data.content
    articleTotal.value = res.data.totalElements
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    articleLoading.value = false
  }
}

const loadUsers = async () => {
  userLoading.value = true
  try {
    const res = await http.get('/admin/users')
    users.value = res.data
    Object.keys(roleDrafts).forEach((key) => delete roleDrafts[key])
    res.data.forEach((user) => {
      roleDrafts[user.id] = user.role
    })
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    userLoading.value = false
  }
}

const refreshAll = async () => {
  await Promise.all([loadArticles(), loadUsers()])
}

const changeArticlePage = (value) => {
  articlePage.value = value - 1
  loadArticles()
}

const viewArticle = (id) => router.push(`/articles/${id}`)
const editArticle = (id) => router.push(`/editor/${id}`)

const removeArticle = async (id) => {
  try {
    await http.delete(`/articles/${id}`)
    ElMessage.success('文章删除成功')
    if (articles.value.length === 1 && articlePage.value > 0) {
      articlePage.value -= 1
    }
    await loadArticles()
  } catch (error) {
    ElMessage.error(error.message)
  }
}

const updateRole = async (user) => {
  const targetRole = roleDrafts[user.id]
  if (!targetRole) {
    ElMessage.warning('请选择角色')
    return
  }
  if (targetRole === user.role) {
    ElMessage.info('角色未发生变化')
    return
  }

  try {
    await http.put(`/admin/users/${user.id}/role`, { role: targetRole })
    ElMessage.success('用户角色更新成功')
    await loadUsers()
  } catch (error) {
    ElMessage.error(error.message)
  }
}

const removeUser = async (user) => {
  try {
    await http.delete(`/admin/users/${user.id}`)
    ElMessage.success('用户删除成功')
    await loadUsers()
  } catch (error) {
    ElMessage.error(error.message)
  }
}

onMounted(refreshAll)
</script>

<style scoped>
.page-card {
  padding: 18px;
}

.head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
  gap: 12px;
}

.head h2 {
  margin: 0;
}

.head p {
  margin: 4px 0 0;
  color: var(--text-secondary);
}

.pager {
  margin-top: 14px;
  justify-content: center;
}
</style>

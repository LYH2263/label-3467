<template>
  <div class="auth-wrapper">
    <div class="auth-card glass-card fade-in">
      <h2>欢迎回来</h2>
      <p>登录后可发布游戏攻略、参与评论互动并查看创作数据。</p>

      <el-form :model="form" :rules="rules" ref="formRef" label-position="top" @submit.prevent>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="form.email" placeholder="请输入邮箱" />
        </el-form-item>

        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" show-password placeholder="请输入密码" />
        </el-form-item>

        <div class="row">
          <el-checkbox v-model="form.rememberMe">7天记住登录</el-checkbox>
          <router-link to="/reset-password" class="link">忘记密码</router-link>
        </div>

        <el-button type="primary" class="submit" :loading="loading" @click="submit">登录</el-button>
      </el-form>

      <p class="foot">还没有账号？<router-link to="/register" class="link">去注册</router-link></p>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import http from '../api/http'
import { useAuthStore } from '../stores/auth'

const auth = useAuthStore()
const route = useRoute()
const router = useRouter()
const formRef = ref()
const loading = ref(false)

const form = reactive({
  email: '',
  password: '',
  rememberMe: true
})

const rules = {
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '邮箱格式错误', trigger: ['blur', 'change'] }
  ],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const submit = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    const res = await http.post('/auth/login', form)
    auth.setSession(res.data.token, res.data.user)
    ElMessage.success('登录成功')
    router.push(route.query.redirect || '/')
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.auth-wrapper {
  min-height: calc(100vh - 140px);
  display: flex;
  align-items: center;
  justify-content: center;
}

.auth-card {
  width: min(460px, 100%);
  padding: 32px;
}

.auth-card h2 {
  margin: 0;
  font-size: 30px;
}

.auth-card p {
  color: var(--text-secondary);
}

.row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 14px;
}

.submit {
  width: 100%;
}

.link {
  color: var(--brand);
}

.foot {
  margin-top: 16px;
  text-align: center;
}
</style>

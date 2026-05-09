<template>
  <div class="auth-wrapper">
    <div class="auth-card glass-card fade-in">
      <h2>{{ hasToken ? '重置密码' : '找回密码' }}</h2>
      <p>{{ hasToken ? '请输入新密码以完成重置。' : '输入注册邮箱，系统会发送重置链接。' }}</p>

      <el-form v-if="!hasToken" :model="forgotForm" ref="forgotRef" :rules="forgotRules" label-position="top">
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="forgotForm.email" placeholder="请输入邮箱" />
        </el-form-item>
        <el-button type="primary" class="submit" :loading="loading" @click="submitForgot">发送重置链接</el-button>
      </el-form>

      <el-form v-else :model="resetForm" ref="resetRef" :rules="resetRules" label-position="top">
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="resetForm.newPassword" type="password" show-password placeholder="至少8位，包含大小写字母和数字" />
        </el-form-item>
        <el-button type="primary" class="submit" :loading="loading" @click="submitReset">确认重置</el-button>
      </el-form>

      <p class="foot"><router-link to="/login" class="link">返回登录</router-link></p>
    </div>
  </div>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import http from '../api/http'

const route = useRoute()
const router = useRouter()
const loading = ref(false)

const forgotRef = ref()
const resetRef = ref()

const forgotForm = reactive({ email: '' })
const resetForm = reactive({ newPassword: '' })

const hasToken = computed(() => Boolean(route.query.token))

const forgotRules = {
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '邮箱格式错误', trigger: ['blur', 'change'] }
  ]
}

const resetRules = {
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 8, message: '密码至少8位', trigger: 'blur' }
  ]
}

const submitForgot = async () => {
  const valid = await forgotRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    await http.post('/auth/forgot-password', forgotForm)
    ElMessage.success('若邮箱存在，重置链接已发送')
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    loading.value = false
  }
}

const submitReset = async () => {
  const valid = await resetRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    await http.post('/auth/reset-password', { token: route.query.token, newPassword: resetForm.newPassword })
    ElMessage.success('密码重置成功，请重新登录')
    router.push('/login')
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

.submit {
  width: 100%;
}

.foot {
  margin-top: 16px;
  text-align: center;
}

.link {
  color: var(--brand);
}
</style>

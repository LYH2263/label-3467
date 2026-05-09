<template>
  <div class="auth-wrapper">
    <div class="auth-card glass-card fade-in">
      <h2>创建账号</h2>
      <p>注册后完成邮箱验证，即可进入 Psyche Game 创作者社区。</p>

      <el-form :model="form" :rules="rules" ref="formRef" label-position="top" @submit.prevent>
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="至少3位" />
        </el-form-item>

        <el-form-item label="邮箱" prop="email">
          <el-input v-model="form.email" placeholder="请输入邮箱" />
        </el-form-item>

        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" show-password placeholder="至少8位，包含大小写和数字" @input="checkStrength" />
        </el-form-item>

        <el-progress :percentage="(strength.score || 0) * 16" :status="strength.level === 'STRONG' ? 'success' : strength.level === 'MEDIUM' ? 'warning' : 'exception'" :show-text="false" />
        <p class="hint">强度: {{ strength.level || 'WEAK' }} ｜ {{ strength.suggestion || '请输入密码' }}</p>

        <el-button type="primary" class="submit" :loading="loading" @click="submit">注册</el-button>
      </el-form>

      <el-alert
        v-if="verificationToken"
        type="success"
        show-icon
        :closable="false"
        title="注册成功（开发演示）"
        :description="`验证令牌: ${verificationToken}`"
      />

      <div class="verify-row" v-if="verificationToken">
        <el-button plain @click="verifyEmail">一键验证邮箱</el-button>
      </div>

      <p class="foot">已有账号？<router-link to="/login" class="link">去登录</router-link></p>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import http from '../api/http'

const router = useRouter()
const formRef = ref()
const loading = ref(false)
const verificationToken = ref('')

const form = reactive({
  username: '',
  email: '',
  password: ''
})

const strength = reactive({
  score: 0,
  level: 'WEAK',
  suggestion: ''
})

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 30, message: '用户名长度需在3-30之间', trigger: 'blur' }
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '邮箱格式错误', trigger: ['blur', 'change'] }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 8, message: '至少8位', trigger: 'blur' }
  ]
}

const checkStrength = async () => {
  if (!form.password) return
  try {
    const res = await http.get('/auth/password-strength', { params: { password: form.password } })
    Object.assign(strength, res.data)
  } catch (error) {
    strength.level = 'WEAK'
    strength.suggestion = error.message
  }
}

const submit = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    const res = await http.post('/auth/register', form)
    verificationToken.value = res.data.verificationToken || ''
    ElMessage.success('注册成功，请验证邮箱')
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    loading.value = false
  }
}

const verifyEmail = async () => {
  try {
    await http.post('/auth/verify-email', null, { params: { token: verificationToken.value } })
    ElMessage.success('邮箱验证成功，请登录')
    router.push('/login')
  } catch (error) {
    ElMessage.error(error.message)
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
  width: min(500px, 100%);
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
  margin-top: 8px;
}

.hint {
  margin: 10px 0 14px;
  font-size: 13px;
}

.foot {
  margin-top: 16px;
  text-align: center;
}

.link {
  color: var(--brand);
}

.verify-row {
  margin-top: 12px;
}
</style>

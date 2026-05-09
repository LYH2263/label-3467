<template>
  <div>
    <header class="topbar">
      <div class="topbar-inner">
        <router-link to="/" class="brand">
          <span class="brand-dot"></span>
          <div>
            <strong>Psyche Game</strong>
            <small>Arcade & Creator Hub</small>
          </div>
        </router-link>

        <nav class="desktop-nav">
          <router-link to="/">游戏广场</router-link>
          <router-link to="/">攻略社区</router-link>
          <router-link to="/my-articles" v-if="auth.isAuthenticated">创作分析</router-link>
          <router-link to="/editor" v-if="auth.isAuthenticated">发布文章</router-link>
          <router-link to="/admin" v-if="auth.isAdmin">运营后台</router-link>
        </nav>

        <div class="actions">
          <el-select v-model="theme.currentTheme" class="theme-select" size="small" @change="theme.applyTheme">
            <el-option label="冰川白" value="theme-glacier" />
            <el-option label="暗夜碳" value="theme-carbon" />
            <el-option label="日出暖" value="theme-sunrise" />
          </el-select>

          <template v-if="auth.isAuthenticated">
            <el-dropdown>
              <span class="user-chip">
                <el-avatar :size="30" :src="auth.user?.avatarUrl || ''">{{ (auth.user?.username || 'U').slice(0, 1).toUpperCase() }}</el-avatar>
                <span>{{ auth.user?.username }}</span>
              </span>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item @click="goProfile">个人主页</el-dropdown-item>
                  <el-dropdown-item v-if="auth.isAdmin" @click="goAdmin">运营后台</el-dropdown-item>
                  <el-dropdown-item @click="logout">退出登录</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
          <template v-else>
            <el-button text @click="router.push('/login')">登录</el-button>
            <el-button type="primary" @click="router.push('/register')">注册</el-button>
          </template>
        </div>
      </div>
    </header>

    <main class="page-shell fade-in">
      <router-view />
    </main>
  </div>
</template>

<script setup>
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '../stores/auth'
import { useThemeStore } from '../stores/theme'

const router = useRouter()
const auth = useAuthStore()
const theme = useThemeStore()

onMounted(async () => {
  theme.applyTheme(theme.currentTheme)
  if (auth.isAuthenticated) {
    try {
      await auth.fetchProfile()
    } catch (error) {
      auth.clearSession()
    }
  }
})

const goProfile = () => {
  router.push('/profile')
}

const goAdmin = () => {
  router.push('/admin')
}

const logout = () => {
  auth.clearSession()
  ElMessage.success('已退出登录')
  router.push('/')
}
</script>

<style scoped>
.topbar {
  position: sticky;
  top: 0;
  z-index: 30;
  background: var(--nav-bg);
  backdrop-filter: blur(16px);
  border-bottom: 1px solid rgba(255, 255, 255, 0.14);
}

.topbar-inner {
  max-width: 1240px;
  margin: 0 auto;
  padding: 12px 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.brand {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  color: #fff;
}

.brand-dot {
  width: 14px;
  height: 14px;
  border-radius: 999px;
  background: radial-gradient(circle at 30% 30%, #fff 0%, #7ec6ff 35%, #0e62ff 100%);
  box-shadow: 0 0 16px rgba(58, 154, 255, 0.8);
}

.brand strong {
  display: block;
  line-height: 1.1;
  font-size: 19px;
}

.brand small {
  display: block;
  opacity: 0.8;
  font-size: 11px;
}

.desktop-nav {
  display: flex;
  gap: 20px;
}

.desktop-nav a {
  color: rgba(255, 255, 255, 0.82);
  font-weight: 500;
  position: relative;
  padding: 4px 0;
}

.desktop-nav a::after {
  content: '';
  position: absolute;
  left: 0;
  bottom: 0;
  width: 0;
  height: 2px;
  background: rgba(255, 255, 255, 0.9);
  transition: width 0.22s ease;
}

.desktop-nav a:hover::after,
.desktop-nav a.router-link-active::after {
  width: 100%;
}

.actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.theme-select {
  width: 110px;
}

.user-chip {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: #fff;
  cursor: pointer;
}

@media (max-width: 880px) {
  .desktop-nav {
    display: none;
  }

  .brand strong {
    font-size: 17px;
  }

  .theme-select {
    width: 98px;
  }
}
</style>

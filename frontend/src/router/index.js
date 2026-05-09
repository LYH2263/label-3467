import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      component: () => import('../layouts/MainLayout.vue'),
      children: [
        { path: '', name: 'home', component: () => import('../pages/HomePage.vue') },
        { path: 'articles/:id', name: 'article-detail', component: () => import('../pages/ArticleDetailPage.vue') },
        { path: 'profile', name: 'profile', component: () => import('../pages/ProfilePage.vue'), meta: { requiresAuth: true } },
        { path: 'editor', name: 'create-article', component: () => import('../pages/ArticleEditorPage.vue'), meta: { requiresAuth: true } },
        { path: 'editor/:id', name: 'edit-article', component: () => import('../pages/ArticleEditorPage.vue'), meta: { requiresAuth: true } },
        { path: 'my-articles', name: 'my-articles', component: () => import('../pages/MyArticlesPage.vue'), meta: { requiresAuth: true } },
        { path: 'admin', name: 'admin', component: () => import('../pages/AdminPage.vue'), meta: { requiresAuth: true, requiresAdmin: true } }
      ]
    },
    { path: '/login', name: 'login', component: () => import('../pages/LoginPage.vue') },
    { path: '/register', name: 'register', component: () => import('../pages/RegisterPage.vue') },
    { path: '/reset-password', name: 'reset-password', component: () => import('../pages/ResetPasswordPage.vue') },
    { path: '/:pathMatch(.*)*', redirect: '/' }
  ]
})

router.beforeEach(async (to) => {
  const auth = useAuthStore()
  if (auth.isAuthenticated && !auth.user) {
    try {
      await auth.fetchProfile()
    } catch (error) {
      auth.clearSession()
    }
  }

  if (to.meta.requiresAuth && !auth.isAuthenticated) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }
  if (to.meta.requiresAdmin && !auth.isAdmin) {
    return { name: 'home' }
  }
  return true
})

export default router

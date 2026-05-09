import { defineStore } from 'pinia'
import http from '../api/http'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem('psyche_token') || '',
    user: JSON.parse(localStorage.getItem('psyche_user') || 'null')
  }),
  getters: {
    isAuthenticated: (state) => Boolean(state.token),
    isAdmin: (state) => state.user?.role === 'ROLE_ADMIN'
  },
  actions: {
    setSession(token, user) {
      this.token = token
      this.user = user
      localStorage.setItem('psyche_token', token)
      localStorage.setItem('psyche_user', JSON.stringify(user))
    },
    clearSession() {
      this.token = ''
      this.user = null
      localStorage.removeItem('psyche_token')
      localStorage.removeItem('psyche_user')
    },
    async fetchProfile() {
      if (!this.token) return null
      const res = await http.get('/users/me')
      this.user = res.data
      localStorage.setItem('psyche_user', JSON.stringify(this.user))
      return this.user
    }
  }
})

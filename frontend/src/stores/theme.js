import { defineStore } from 'pinia'

const DEFAULT_THEME = localStorage.getItem('psyche_theme') || 'theme-glacier'

export const useThemeStore = defineStore('theme', {
  state: () => ({
    currentTheme: DEFAULT_THEME
  }),
  actions: {
    applyTheme(theme) {
      this.currentTheme = theme
      document.body.classList.remove('theme-glacier', 'theme-carbon', 'theme-sunrise')
      document.body.classList.add(theme)
      localStorage.setItem('psyche_theme', theme)
    }
  }
})

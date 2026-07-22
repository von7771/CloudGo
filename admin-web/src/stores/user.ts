import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('admin_token') || '')

  const isLoggedIn = computed(() => !!token.value)

  function setToken(value: string) {
    token.value = value
    localStorage.setItem('admin_token', value)
  }

  function logout() {
    token.value = ''
    localStorage.removeItem('admin_token')
  }

  return { token, isLoggedIn, setToken, logout }
})

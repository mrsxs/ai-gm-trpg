import { defineStore } from 'pinia'
import { apiLogin, apiMe } from '../api'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: '',
    userInfo: null // {userId,username,nickname,roles[]}
  }),
  getters: {
    isLogin: (s) => !!s.token,
    roles: (s) => s.userInfo?.roles || [],
    username: (s) => s.userInfo?.username || '',
    nickname: (s) => s.userInfo?.nickname || s.userInfo?.username || ''
  },
  actions: {
    hasRole(role) {
      return (this.userInfo?.roles || []).includes(role)
    },
    hasAnyRole(...rs) {
      const mine = this.userInfo?.roles || []
      return rs.some((r) => mine.includes(r))
    },
    async login(username, password) {
      const data = await apiLogin({ username, password })
      this.token = data.token
      this.userInfo = data.userInfo
      return data.userInfo
    },
    async refreshMe() {
      this.userInfo = await apiMe()
    },
    clear() {
      this.token = ''
      this.userInfo = null
    },
    logout() {
      this.clear()
    }
  },
  persist: true
})

import { createRouter, createWebHistory } from 'vue-router'
import { ElMessage } from 'element-plus'

const routes = [
  { path: '/', redirect: '/hall' },
  { path: '/login', component: () => import('../views/Login.vue'), meta: { public: true } },
  { path: '/register', component: () => import('../views/Register.vue'), meta: { public: true } },
  { path: '/hall', component: () => import('../views/GameHall.vue') },
  { path: '/play/:sessionId', component: () => import('../views/GamePlay.vue'), meta: { roles: ['PLAYER', 'ADMIN'] } },
  { path: '/scenarios', component: () => import('../views/ScenarioList.vue'), meta: { roles: ['AUTHOR', 'ADMIN'] } },
  { path: '/scenarios/:id/edit', component: () => import('../views/ScenarioEditor.vue'), meta: { roles: ['AUTHOR', 'ADMIN'] } },
  { path: '/admin/users', component: () => import('../views/UserManage.vue'), meta: { roles: ['ADMIN'] } }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to) => {
  // 延迟取 store，避免 pinia 未初始化
  const raw = localStorage.getItem('user')
  let token = ''
  let roles = []
  if (raw) {
    try {
      const u = JSON.parse(raw)
      token = u.token
      roles = u.userInfo?.roles || []
    } catch (e) { /* ignore */ }
  }
  if (to.meta.public) return true
  if (!token) return '/login'
  if (to.meta.roles && !to.meta.roles.some((r) => roles.includes(r))) {
    ElMessage.warning('无权限访问该页面')
    return '/hall'
  }
  return true
})

export default router

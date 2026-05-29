<template>
  <header class="topbar">
    <div class="topbar-inner">
      <div class="brand" @click="go('/hall')">
        <span class="logo">🎭</span>
        <span class="title">AI 跑团 GM</span>
      </div>
      <nav class="nav">
        <a :class="{ active: isActive('/hall') }" @click="go('/hall')">游戏大厅</a>
        <a v-if="store.hasAnyRole('AUTHOR','ADMIN')" :class="{ active: isActive('/scenarios') }" @click="go('/scenarios')">剧本工作台</a>
        <a v-if="store.hasRole('ADMIN')" :class="{ active: isActive('/admin/users') }" @click="go('/admin/users')">用户管理</a>
      </nav>
      <div class="user">
        <el-tag v-for="r in store.roles" :key="r" size="small" effect="light" class="role-tag">{{ roleName(r) }}</el-tag>
        <span class="nick">{{ store.nickname }}</span>
        <el-button size="small" text @click="logout">登出</el-button>
      </div>
    </div>
  </header>
</template>

<script setup>
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '../store/user'

const store = useUserStore()
const router = useRouter()
const route = useRoute()
const go = (p) => router.push(p)
const isActive = (p) => route.path.startsWith(p)
const roleName = (r) => ({ PLAYER: '玩家', AUTHOR: '编剧', ADMIN: '管理员' }[r] || r)
const logout = () => { store.logout(); router.push('/login') }
</script>

<style scoped>
.topbar { background: var(--surface); border-bottom: 1px solid var(--border); position: sticky; top: 0; z-index: 10; }
.topbar-inner { max-width: 1120px; margin: 0 auto; height: 60px; display: flex; align-items: center; gap: 28px; padding: 0 24px; }
.brand { display: flex; align-items: center; gap: 8px; cursor: pointer; }
.logo { font-size: 22px; }
.title { font-weight: 700; font-size: 18px; color: var(--text); }
.nav { display: flex; gap: 20px; flex: 1; }
.nav a { color: var(--text-2); cursor: pointer; font-size: 15px; padding: 4px 0; border-bottom: 2px solid transparent; }
.nav a.active { color: var(--primary); border-bottom-color: var(--primary); font-weight: 600; }
.user { display: flex; align-items: center; gap: 8px; }
.nick { color: var(--text); font-weight: 500; }
.role-tag { background: var(--primary-soft); color: var(--primary); border: none; }
</style>

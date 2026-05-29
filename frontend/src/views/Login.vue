<template>
  <div class="auth-page">
    <div class="auth-card card">
      <div class="auth-head">
        <div class="logo">🎭</div>
        <h1>AI 跑团 GM</h1>
        <p class="muted">单人 AI 互动叙事 · 状态机约束的开放冒险</p>
      </div>
      <el-form :model="form" @submit.prevent>
        <el-form-item>
          <el-input v-model="form.username" size="large" placeholder="用户名" :prefix-icon="User" />
        </el-form-item>
        <el-form-item>
          <el-input v-model="form.password" size="large" type="password" show-password placeholder="密码"
                    :prefix-icon="Lock" @keyup.enter="onLogin" />
        </el-form-item>
        <el-button type="primary" size="large" style="width:100%" :loading="loading" @click="onLogin">登录</el-button>
      </el-form>
      <div class="auth-foot">
        <span class="muted">还没有账号？</span><router-link to="/register">注册</router-link>
      </div>
      <div class="seed muted">演示账号：admin / author / player（密码均 123456）</div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { useUserStore } from '../store/user'

const store = useUserStore()
const router = useRouter()
const form = ref({ username: '', password: '' })
const loading = ref(false)

const onLogin = async () => {
  if (!form.value.username || !form.value.password) return ElMessage.warning('请输入用户名和密码')
  loading.value = true
  try {
    const info = await store.login(form.value.username, form.value.password)
    ElMessage.success('登录成功')
    const roles = info.roles || []
    if (roles.includes('PLAYER')) router.push('/hall')
    else if (roles.includes('AUTHOR')) router.push('/scenarios')
    else if (roles.includes('ADMIN')) router.push('/admin/users')
    else router.push('/hall')
  } catch (e) { /* handled by interceptor */ }
  finally { loading.value = false }
}
</script>

<style scoped>
.auth-page { min-height: 100vh; display: flex; align-items: center; justify-content: center;
  background: linear-gradient(0deg, var(--bg), var(--bg)); }
.auth-card { width: 380px; padding: 36px 32px; box-shadow: 0 8px 30px rgba(44,47,58,.08); }
.auth-head { text-align: center; margin-bottom: 24px; }
.auth-head .logo { font-size: 40px; }
.auth-head h1 { margin: 8px 0 4px; font-size: 24px; color: var(--text); }
.auth-foot { text-align: center; margin-top: 12px; }
.seed { text-align: center; margin-top: 16px; font-size: 12px; }
</style>

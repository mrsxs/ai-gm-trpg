<template>
  <div class="auth-page">
    <div class="auth-card card">
      <div class="auth-head">
        <div class="logo">🎭</div>
        <h1>注册账号</h1>
        <p class="muted">注册后默认获得「玩家」角色</p>
      </div>
      <el-form :model="form" @submit.prevent>
        <el-form-item>
          <el-input v-model="form.username" size="large" placeholder="用户名（3-50位）" :prefix-icon="User" />
        </el-form-item>
        <el-form-item>
          <el-input v-model="form.nickname" size="large" placeholder="昵称（可选）" :prefix-icon="Avatar" />
        </el-form-item>
        <el-form-item>
          <el-input v-model="form.password" size="large" type="password" show-password placeholder="密码（6-50位）"
                    :prefix-icon="Lock" @keyup.enter="onRegister" />
        </el-form-item>
        <el-button type="primary" size="large" style="width:100%" :loading="loading" @click="onRegister">注册</el-button>
      </el-form>
      <div class="auth-foot">
        <span class="muted">已有账号？</span><router-link to="/login">去登录</router-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock, Avatar } from '@element-plus/icons-vue'
import { apiRegister } from '../api'

const router = useRouter()
const form = ref({ username: '', password: '', nickname: '' })
const loading = ref(false)

const onRegister = async () => {
  if (!form.value.username || !form.value.password) return ElMessage.warning('请输入用户名和密码')
  loading.value = true
  try {
    await apiRegister(form.value)
    ElMessage.success('注册成功，请登录')
    router.push('/login')
  } catch (e) { /* handled */ }
  finally { loading.value = false }
}
</script>

<style scoped>
.auth-page { min-height: 100vh; display: flex; align-items: center; justify-content: center; background: var(--bg); }
.auth-card { width: 380px; padding: 36px 32px; box-shadow: 0 8px 30px rgba(44,47,58,.08); }
.auth-head { text-align: center; margin-bottom: 24px; }
.auth-head .logo { font-size: 40px; }
.auth-head h1 { margin: 8px 0 4px; font-size: 22px; color: var(--text); }
.auth-foot { text-align: center; margin-top: 12px; }
</style>

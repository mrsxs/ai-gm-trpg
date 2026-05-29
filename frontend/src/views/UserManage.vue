<template>
  <div class="page">
    <TopBar />
    <div class="container">
      <h2 class="section-title">用户管理</h2>
      <div class="toolbar">
        <el-input v-model="kw" placeholder="按用户名搜索" style="width:240px" clearable @keyup.enter="load" />
        <el-button @click="load">搜索</el-button>
      </div>
      <el-table :data="list" stripe style="margin-top:12px">
        <el-table-column prop="userId" label="ID" width="80" />
        <el-table-column prop="username" label="用户名" width="160" />
        <el-table-column prop="nickname" label="昵称" width="160" />
        <el-table-column label="角色">
          <template #default="{ row }">
            <el-tag v-for="r in row.roles" :key="r" size="small" effect="light" style="margin-right:4px">{{ roleName(r) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-switch :model-value="row.status === 1" @change="(v) => toggleStatus(row, v)" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220">
          <template #default="{ row }">
            <el-button size="small" @click="openRoles(row)">分配角色</el-button>
            <el-button size="small" type="danger" plain @click="del(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        style="margin-top:16px;justify-content:flex-end"
        layout="prev, pager, next, total"
        :total="total" :page-size="size" :current-page="page"
        @current-change="(p) => { page = p; load() }"
      />
    </div>

    <el-dialog v-model="roleDlg" title="分配角色" width="360px">
      <el-checkbox-group v-model="chosenRoles">
        <el-checkbox label="PLAYER">玩家</el-checkbox>
        <el-checkbox label="AUTHOR">编剧</el-checkbox>
        <el-checkbox label="ADMIN">管理员</el-checkbox>
      </el-checkbox-group>
      <template #footer><el-button @click="roleDlg=false">取消</el-button><el-button type="primary" @click="saveRoles">保存</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import TopBar from '../components/TopBar.vue'
import { apiAdminUsers, apiUpdateUserStatus, apiAssignRoles, apiDeleteUser } from '../api'

const list = ref([]); const total = ref(0); const page = ref(1); const size = 10; const kw = ref('')
const roleName = (r) => ({ PLAYER: '玩家', AUTHOR: '编剧', ADMIN: '管理员' }[r] || r)

const load = async () => {
  const d = await apiAdminUsers({ page: page.value, size, username: kw.value || undefined })
  list.value = d.list || []; total.value = d.total || 0
}
onMounted(load)

const toggleStatus = async (row, v) => {
  await apiUpdateUserStatus(row.userId, v ? 1 : 0)
  ElMessage.success('已更新状态'); load()
}

const roleDlg = ref(false); const chosenRoles = ref([]); const curUser = ref(null)
const openRoles = (row) => { curUser.value = row; chosenRoles.value = [...(row.roles || [])]; roleDlg.value = true }
const saveRoles = async () => {
  if (!chosenRoles.value.length) return ElMessage.warning('至少选择一个角色')
  await apiAssignRoles(curUser.value.userId, chosenRoles.value)
  ElMessage.success('已分配'); roleDlg.value = false; load()
}
const del = async (row) => {
  await apiDeleteUser(row.userId)
  ElMessage.success('已删除'); load()
}
</script>

<style scoped>
.toolbar { display: flex; gap: 8px; }
</style>

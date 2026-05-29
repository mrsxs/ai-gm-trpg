<template>
  <div class="page">
    <TopBar />
    <div class="container">
      <div class="head">
        <h2 class="section-title" style="margin:0">我的剧本</h2>
        <el-button type="primary" :icon="Plus" @click="openCreate">新建剧本</el-button>
      </div>
      <el-table :data="list" stripe style="margin-top:16px">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="title" label="标题" />
        <el-table-column prop="genre" label="题材" width="100" />
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : (row.status === 2 ? 'info' : 'warning')" effect="light">
              {{ statusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="320">
          <template #default="{ row }">
            <el-button size="small" @click="$router.push(`/scenarios/${row.id}/edit`)">编辑状态机</el-button>
            <el-button v-if="row.status !== 1" size="small" type="success" plain @click="publish(row, 1)">发布</el-button>
            <el-button v-else size="small" type="warning" plain @click="publish(row, 2)">下架</el-button>
            <el-button size="small" type="danger" plain @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!list.length" description="还没有剧本，新建一个开始创作" />
    </div>

    <el-dialog v-model="dialog" title="新建剧本" width="460px">
      <el-form :model="form" label-width="64px">
        <el-form-item label="标题"><el-input v-model="form.title" /></el-form-item>
        <el-form-item label="题材"><el-input v-model="form.genre" placeholder="悬疑 / 奇幻 …" /></el-form-item>
        <el-form-item label="简介"><el-input v-model="form.intro" type="textarea" :rows="3" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog = false">取消</el-button>
        <el-button type="primary" @click="create">创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import TopBar from '../components/TopBar.vue'
import { apiMine, apiCreateScenario, apiPublishScenario, apiDeleteScenario } from '../api'

const list = ref([])
const dialog = ref(false)
const form = ref({ title: '', genre: '', intro: '' })

const statusText = (s) => ({ 0: '草稿', 1: '已发布', 2: '已下架' }[s] || s)
const load = async () => { list.value = (await apiMine({ page: 1, size: 100 })).list || [] }
onMounted(load)

const openCreate = () => { form.value = { title: '', genre: '', intro: '' }; dialog.value = true }
const create = async () => {
  if (!form.value.title) return ElMessage.warning('请输入标题')
  await apiCreateScenario(form.value)
  ElMessage.success('已创建草稿')
  dialog.value = false
  load()
}
const publish = async (row, status) => {
  try {
    await apiPublishScenario(row.id, status)
    ElMessage.success(status === 1 ? '已发布' : '已下架')
    load()
  } catch (e) { /* 结构校验失败信息已由拦截器提示 */ }
}
const remove = async (row) => {
  await ElMessageBox.confirm(`删除剧本「${row.title}」？`, '提示', { type: 'warning' })
  await apiDeleteScenario(row.id)
  ElMessage.success('已删除')
  load()
}
</script>

<style scoped>
.head { display: flex; align-items: center; justify-content: space-between; }
</style>

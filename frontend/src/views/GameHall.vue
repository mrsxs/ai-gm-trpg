<template>
  <div class="page">
    <TopBar />
    <div class="container">
      <h2 class="section-title">可玩剧本</h2>
      <div class="grid">
        <div v-for="s in scenarios" :key="s.id" class="scenario card">
          <div class="cover">{{ (s.genre || '剧本')[0] }}</div>
          <div class="body">
            <div class="row1">
              <span class="name">{{ s.title }}</span>
              <el-tag size="small" effect="plain">{{ s.genre }}</el-tag>
            </div>
            <p class="intro">{{ s.intro }}</p>
            <el-button type="primary" :loading="startingId === s.id" @click="start(s)">开始游戏</el-button>
          </div>
        </div>
        <el-empty v-if="!scenarios.length" description="暂无已发布剧本" />
      </div>

      <h2 class="section-title" style="margin-top:32px">我的存档</h2>
      <div class="saves">
        <div v-for="g in sessions" :key="g.sessionId" class="save card">
          <div class="save-main">
            <div class="save-title">{{ g.title }}</div>
            <div class="save-meta muted">回合 {{ g.turnCount }} · {{ g.createdAt }}</div>
          </div>
          <StatusTag :status="g.status" />
          <div class="save-actions">
            <el-button size="small" type="primary" plain @click="resume(g)">
              {{ g.status === 1 ? '继续' : '回顾' }}
            </el-button>
            <el-button v-if="g.status === 1" size="small" type="danger" plain @click="abandon(g)">弃局</el-button>
          </div>
        </div>
        <el-empty v-if="!sessions.length" description="还没有存档，去开一局吧" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import TopBar from '../components/TopBar.vue'
import StatusTag from '../components/StatusTag.vue'
import { apiPublished, apiSessions, apiStartSession, apiAbandon } from '../api'

const router = useRouter()
const scenarios = ref([])
const sessions = ref([])
const startingId = ref(null)

const load = async () => {
  scenarios.value = (await apiPublished({ page: 1, size: 50 })).list || []
  sessions.value = (await apiSessions({ page: 1, size: 50 })).list || []
}
onMounted(load)

const start = async (s) => {
  startingId.value = s.id
  try {
    const data = await apiStartSession(s.id)
    router.push(`/play/${data.sessionId}`)
  } catch (e) { /* handled */ }
  finally { startingId.value = null }
}
const resume = (g) => router.push(`/play/${g.sessionId}`)
const abandon = async (g) => {
  await ElMessageBox.confirm('确定弃局？该对局将标记为已弃局。', '提示', { type: 'warning' })
  await apiAbandon(g.sessionId)
  ElMessage.success('已弃局')
  load()
}
</script>

<style scoped>
.grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(320px, 1fr)); gap: 16px; }
.scenario { overflow: hidden; display: flex; flex-direction: column; }
.cover { height: 96px; background: var(--primary-soft); color: var(--primary); display: flex; align-items: center; justify-content: center; font-size: 40px; font-weight: 700; }
.body { padding: 16px; display: flex; flex-direction: column; gap: 10px; }
.row1 { display: flex; align-items: center; justify-content: space-between; }
.name { font-weight: 600; font-size: 16px; }
.intro { color: var(--text-2); font-size: 13px; line-height: 1.6; min-height: 42px; margin: 0; }
.saves { display: flex; flex-direction: column; gap: 10px; }
.save { display: flex; align-items: center; gap: 16px; padding: 14px 18px; }
.save-main { flex: 1; }
.save-title { font-weight: 600; }
.save-meta { font-size: 12px; margin-top: 2px; }
.save-actions { display: flex; gap: 8px; }
</style>

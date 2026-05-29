<template>
  <div class="imm">
    <div class="imm-top">
      <a class="back" @click="$router.push('/hall')">← 返回大厅</a>
      <div class="imm-title">{{ sessionTitle }}</div>
      <div class="spacer" />
    </div>

    <div v-if="finished" class="banner" :class="endingClass">
      {{ endingText }}
    </div>

    <div class="imm-body">
      <div class="flow" ref="flowEl">
        <div v-for="(t, i) in turns" :key="i" class="turn">
          <div v-if="t.playerInput" class="bubble player">
            <span class="who">你</span>
            <div class="text">{{ t.playerInput }}</div>
          </div>
          <div class="narrative">{{ t.aiOutput?.narrative }}</div>
          <div v-for="(d, j) in t.aiOutput?.npcDialogues || []" :key="j" class="bubble npc">
            <span class="who">{{ npcName(d.npcId) }}</span>
            <div class="text">{{ d.line }}</div>
          </div>
        </div>
        <div v-if="thinking" class="thinking">GM 正在叙事…</div>
      </div>

      <aside class="side">
        <StateBar :state="state" />
      </aside>
    </div>

    <div class="imm-input">
      <el-input
        v-model="input"
        :disabled="finished || thinking"
        size="large"
        placeholder="输入你的行动…（回车提交）"
        @keyup.enter="submit"
      />
      <el-button type="primary" size="large" :loading="thinking" :disabled="finished || !input.trim()" @click="submit">
        {{ finished ? '已结束' : '行动' }}
      </el-button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import StateBar from '../components/StateBar.vue'
import { apiSessionDetail, apiSubmitTurn } from '../api'

const route = useRoute()
const sessionId = route.params.sessionId

const turns = ref([])
const state = ref({})
const session = ref({})
const npcMap = ref({})
const input = ref('')
const thinking = ref(false)
const flowEl = ref(null)

const sessionTitle = computed(() => session.value?.title || '对局')
const finished = computed(() => session.value?.status && session.value.status !== 1)
const endingClass = computed(() => (session.value?.status === 2 ? 'win' : 'lose'))
const endingText = computed(() => (session.value?.status === 2 ? '🏆 真相大白 · 通关' : '🥀 真相蒙尘 · 失败'))

const npcName = (id) => npcMap.value[id] || `NPC#${id}`

const scrollBottom = () => nextTick(() => { if (flowEl.value) flowEl.value.scrollTop = flowEl.value.scrollHeight })

const load = async () => {
  const d = await apiSessionDetail(sessionId)
  session.value = d.session
  state.value = d.state
  turns.value = d.turns || []
  // 从 aiOutput 收集 npc 名（若无则用 id）
  scrollBottom()
}
onMounted(load)

const submit = async () => {
  if (!input.value.trim() || thinking.value || finished.value) return
  const text = input.value
  input.value = ''
  thinking.value = true
  try {
    const r = await apiSubmitTurn(sessionId, text)
    turns.value.push(r.turn)
    state.value = r.state
    if (r.finished) {
      // 刷新 session 状态以显示结局
      const d = await apiSessionDetail(sessionId)
      session.value = d.session
      ElMessage.success('故事抵达结局')
    }
    scrollBottom()
  } catch (e) { input.value = text }
  finally { thinking.value = false }
}
</script>

<style scoped>
.imm { min-height: 100vh; background: var(--imm-bg); color: var(--imm-text); display: flex; flex-direction: column; }
.imm-top { display: flex; align-items: center; height: 56px; padding: 0 24px; border-bottom: 1px solid var(--imm-border); }
.back { color: var(--imm-text-2); cursor: pointer; width: 120px; }
.imm-title { flex: 1; text-align: center; font-family: var(--font-serif); font-size: 18px; color: var(--imm-accent); }
.spacer { width: 120px; }
.banner { text-align: center; padding: 14px; font-size: 18px; font-weight: 700; font-family: var(--font-serif); }
.banner.win { background: rgba(240,178,92,.15); color: var(--imm-accent); }
.banner.lose { background: rgba(217,83,79,.15); color: #ff8a85; }
.imm-body { flex: 1; display: flex; gap: 20px; max-width: 1120px; width: 100%; margin: 0 auto; padding: 20px 24px; overflow: hidden; }
.flow { flex: 1; overflow-y: auto; padding-right: 8px; }
.turn { margin-bottom: 26px; }
.narrative { font-family: var(--font-serif); font-size: 17px; line-height: 1.9; color: var(--imm-narrative); margin: 12px 0; white-space: pre-wrap; }
.bubble { margin: 10px 0; padding: 12px 16px; border-radius: 14px; max-width: 80%; }
.bubble .who { font-size: 12px; color: var(--imm-text-2); display: block; margin-bottom: 4px; }
.bubble .text { line-height: 1.7; }
.bubble.player { background: var(--imm-player-bubble); margin-left: auto; border-bottom-right-radius: 4px; }
.bubble.npc { background: var(--imm-npc-bubble); border-bottom-left-radius: 4px; }
.thinking { color: var(--imm-text-2); font-style: italic; padding: 8px 0; }
.side { width: 300px; overflow-y: auto; }
.imm-input { display: flex; gap: 12px; max-width: 1120px; width: 100%; margin: 0 auto; padding: 16px 24px 24px; }
.imm-input :deep(.el-input__wrapper) { background: var(--imm-surface); box-shadow: none; border: 1px solid var(--imm-border); }
.imm-input :deep(.el-input__inner) { color: var(--imm-text); }
</style>

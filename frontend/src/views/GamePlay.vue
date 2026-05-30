<template>
  <div class="imm">
    <div class="imm-top">
      <a class="back" @click="$router.push('/hall')">← 返回大厅</a>
      <div class="imm-title">{{ sessionTitle }}</div>
      <div class="spacer">
        <el-button link class="help-btn" @click="showIntro = true">玩法 / 背景</el-button>
      </div>
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

    <div class="imm-foot">
      <div v-if="!finished && suggestions.length" class="suggests">
        <span class="loc" v-if="currentNodeTitle">📍 {{ currentNodeTitle }}</span>
        <span class="sug-tip">试试：</span>
        <button
          v-for="(s, i) in suggestions"
          :key="i"
          class="chip"
          :disabled="thinking"
          @click="pick(s)"
        >{{ s }}</button>
        <span class="sug-hint">点击填入，可自由修改后发送</span>
      </div>

      <div class="imm-input">
        <el-input
          v-model="input"
          ref="inputEl"
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

    <!-- 开局背景 / 玩法 / 通关条件 -->
    <el-dialog v-model="showIntro" :title="introTitle" width="560px" class="intro-dialog" align-center>
      <div class="intro">
        <div class="sec">
          <h4>📖 背景</h4>
          <p>{{ scenario.intro || '一场即兴的互动叙事冒险，由 AI 担任游戏主持人（GM）。' }}</p>
        </div>
        <div class="sec">
          <h4>🎯 通关条件</h4>
          <p>探索场景、与角色周旋、收集线索；在关键节点满足条件即可走向<b>胜利结局{{ winTitles ? '「' + winTitles + '」' : '' }}</b>，否则可能落入失败结局。</p>
        </div>
        <div class="sec">
          <h4>🎮 怎么玩</h4>
          <ul>
            <li>在下方输入框<b>自由描述你的行动</b>（如「走进大厅」「向管家追问真相」「搜查抽屉」），回车发送。</li>
            <li>不知道做什么时，点输入框上方的<b>动作提示</b>，会自动填入，你可以改了再发。</li>
            <li>AI 会即兴叙事并分饰 NPC，但<b>剧情只能沿编剧设定的轨道推进</b>——任你怎么输入都跳不出故事线。</li>
          </ul>
        </div>
      </div>
      <template #footer>
        <el-button type="primary" @click="showIntro = false">开始游戏</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import StateBar from '../components/StateBar.vue'
import { apiSessionDetail, apiSubmitTurn, apiScenarioDetail } from '../api'

const route = useRoute()
const sessionId = route.params.sessionId

const turns = ref([])
const state = ref({})
const session = ref({})
const scenario = ref({})
const npcMap = ref({})
const nodeMap = ref({})
const transitions = ref([])
const input = ref('')
const thinking = ref(false)
const flowEl = ref(null)
const inputEl = ref(null)
const showIntro = ref(false)

// 通用探索动作（任何节点都给一两个兜底提示）
const GENERIC = ['仔细环顾四周', '与在场的人交谈']

const sessionTitle = computed(() => session.value?.title || '对局')
const finished = computed(() => session.value?.status && session.value.status !== 1)
const endingClass = computed(() => (session.value?.status === 2 ? 'win' : 'lose'))
const endingText = computed(() => (session.value?.status === 2 ? '🏆 真相大白 · 通关' : '🥀 真相蒙尘 · 失败'))

const introTitle = computed(() => scenario.value?.title ? `${scenario.value.title}` : '玩法说明')
const winTitles = computed(() =>
  Object.values(nodeMap.value)
    .filter((n) => n.isEnding === 1 && (n.endingType || '').toUpperCase() === 'WIN')
    .map((n) => n.title)
    .join('、'),
)

const currentNode = computed(() => nodeMap.value[state.value?.currentNodeId] || null)
const currentNodeTitle = computed(() => currentNode.value?.title || '')

// 当前节点的出边描述 = 剧情动作提示；不足则补通用探索动作
const suggestions = computed(() => {
  const cur = state.value?.currentNodeId
  const fromCur = transitions.value
    .filter((t) => t.fromNodeId === cur && t.description)
    .sort((a, b) => (b.priority || 0) - (a.priority || 0))
    .map((t) => t.description)
  const list = [...new Set([...fromCur, ...GENERIC])]
  return list.slice(0, 5)
})

const npcName = (id) => npcMap.value[id] || `NPC#${id}`

const scrollBottom = () => nextTick(() => { if (flowEl.value) flowEl.value.scrollTop = flowEl.value.scrollHeight })

const pick = (text) => {
  input.value = text
  nextTick(() => inputEl.value?.focus())
}

const load = async () => {
  const d = await apiSessionDetail(sessionId)
  session.value = d.session
  state.value = d.state
  turns.value = d.turns || []

  // 一次拉剧本静态资料复合体：{scenario, nodes, npcs, transitions}（对外只读接口）
  const sid = d.session?.scenarioId
  if (sid) {
    try {
      const sc = await apiScenarioDetail(sid)
      scenario.value = sc?.scenario || {}
      npcMap.value = Object.fromEntries((sc?.npcs || []).map((n) => [n.id, n.name]))
      nodeMap.value = Object.fromEntries((sc?.nodes || []).map((n) => [n.id, n]))
      transitions.value = sc?.transitions || []
    } catch (e) { /* 静态资料拉取失败不阻断对局 */ }
  }

  // 新开局（仅一个回合且未结束）自动弹出背景说明
  if (!finished.value && turns.value.length <= 1) showIntro.value = true
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
.spacer { width: 120px; text-align: right; }
.help-btn { color: var(--imm-text-2); }
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
.imm-foot { max-width: 1120px; width: 100%; margin: 0 auto; }
.suggests { display: flex; flex-wrap: wrap; align-items: center; gap: 8px; padding: 6px 24px 0; }
.loc { font-family: var(--font-serif); color: var(--imm-accent); font-size: 13px; margin-right: 6px; }
.sug-tip { color: var(--imm-text-2); font-size: 13px; }
.chip { background: var(--imm-surface); color: var(--imm-text); border: 1px solid var(--imm-border); border-radius: 14px; padding: 5px 12px; font-size: 13px; cursor: pointer; transition: all .15s; }
.chip:hover:not(:disabled) { border-color: var(--imm-accent); color: var(--imm-accent); }
.chip:disabled { opacity: .5; cursor: default; }
.sug-hint { color: var(--imm-text-2); font-size: 12px; opacity: .7; margin-left: 4px; }
.imm-input { display: flex; gap: 12px; padding: 12px 24px 24px; }
.imm-input :deep(.el-input__wrapper) { background: var(--imm-surface); box-shadow: none; border: 1px solid var(--imm-border); }
.imm-input :deep(.el-input__inner) { color: var(--imm-text); }
.intro :deep(.sec) { margin-bottom: 16px; }
.intro h4 { margin: 0 0 6px; font-size: 15px; }
.intro p { margin: 0; line-height: 1.8; color: #444; }
.intro ul { margin: 0; padding-left: 20px; line-height: 1.9; color: #444; }
</style>

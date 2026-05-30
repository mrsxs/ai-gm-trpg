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
        <div v-if="pendingInput" class="turn">
          <div class="bubble player">
            <span class="who">你</span>
            <div class="text">{{ pendingInput }}</div>
          </div>
        </div>
        <div v-for="(q, i) in queue" :key="'q' + i" class="turn">
          <div class="bubble player queued">
            <span class="who">你 · 待发送</span>
            <div class="text">{{ q }}</div>
          </div>
        </div>
        <div v-if="thinking" class="thinking">GM 正在叙事…</div>
      </div>

      <aside class="side">
        <InvestigationLog :flag-defs="flagDefs" :flags="state.flags || {}" :hint="objective" />
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
          :disabled="finished"
          size="large"
          :placeholder="thinking ? 'GM 叙事中，回车可排队下一步…' : '输入你的行动…（回车提交）'"
          @keyup.enter="submit"
        />
        <el-button type="primary" size="large" :loading="thinking && !input.trim()" :disabled="finished || !input.trim()" @click="submit">
          {{ finished ? '已结束' : thinking ? '排队' : '行动' }}
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

    <!-- 结局大图 -->
    <el-dialog v-model="showEnding" :show-close="false" width="460px" class="ending-dialog" align-center>
      <div class="ending" :class="endingClass">
        <div class="ending-emoji">{{ session.status === 2 ? '🏆' : '🥀' }}</div>
        <div class="ending-title">{{ session.status === 2 ? '真相大白 · 通关' : '真相蒙尘 · 失败' }}</div>
        <div class="ending-sub">{{ endingSub }}</div>
        <div class="ending-stats">
          <span>共 {{ session.turnCount }} 回合</span>
          <span>证据 {{ state.attributes?.evidence ?? 0 }}</span>
          <span>理智 {{ state.attributes?.sanity ?? 0 }}</span>
        </div>
        <div v-if="state.inventory?.length" class="ending-items">收集物证：{{ state.inventory.join('、') }}</div>
      </div>
      <template #footer>
        <el-button @click="showEnding = false">留在此页回顾</el-button>
        <el-button type="primary" @click="$router.push('/hall')">返回大厅</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import StateBar from '../components/StateBar.vue'
import InvestigationLog from '../components/InvestigationLog.vue'
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
const flagDefs = ref([])
const input = ref('')
const thinking = ref(false)
const pendingInput = ref('')   // 当前在途回合的玩家消息，AI 回复到达前占位展示
const queue = ref([])          // 思考时排队的后续行动，按序串行发送（不并发）
const flowEl = ref(null)
const inputEl = ref(null)
const showIntro = ref(false)
const showEnding = ref(false)

// 通用探索动作（任何节点都给一两个兜底提示）
const GENERIC = ['仔细环顾四周', '与在场的人交谈']

const sessionTitle = computed(() => session.value?.title || '对局')
const finished = computed(() => session.value?.status && session.value.status !== 1)
const endingClass = computed(() => (session.value?.status === 2 ? 'win' : 'lose'))
const endingText = computed(() => (session.value?.status === 2 ? '🏆 真相大白 · 通关' : '🥀 真相蒙尘 · 失败'))
const endingSub = computed(() =>
  session.value?.status === 2
    ? '你拼齐了散落的线索，当众揭穿真凶，沉冤得雪。'
    : '真相从指缝溜走，凶手隐入夜色——再来一局或许会不同。',
)

const introTitle = computed(() => scenario.value?.title ? `${scenario.value.title}` : '玩法说明')
const winTitles = computed(() =>
  Object.values(nodeMap.value)
    .filter((n) => n.isEnding === 1 && (n.endingType || '').toUpperCase() === 'WIN')
    .map((n) => n.title)
    .join('、'),
)

const currentNode = computed(() => nodeMap.value[state.value?.currentNodeId] || null)
const currentNodeTitle = computed(() => currentNode.value?.title || '')

// 是否身处「对峙/抉择」节点：当前节点有通往结局节点的出边
const atShowdown = computed(() => {
  const cur = state.value?.currentNodeId
  return transitions.value.some(
    (t) => t.fromNodeId === cur && nodeMap.value[t.toNodeId]?.isEnding === 1,
  )
})

// 对峙厅在场 NPC（用于"指认某人"动作）
const presentNpcNames = computed(() =>
  (currentNode.value?.npcIds || []).map((id) => npcMap.value[id]).filter(Boolean),
)

// 动作提示：对峙厅 → 指认在场每个人；普通节点 → 出边描述 + 通用探索
const suggestions = computed(() => {
  const cur = state.value?.currentNodeId
  if (atShowdown.value && presentNpcNames.value.length) {
    return presentNpcNames.value.map((n) => `我指认「${n}」就是凶手`)
  }
  const fromCur = transitions.value
    .filter((t) => t.fromNodeId === cur && t.description && nodeMap.value[t.toNodeId]?.isEnding !== 1)
    .sort((a, b) => (b.priority || 0) - (a.priority || 0))
    .map((t) => t.description)
  const list = [...new Set([...fromCur, ...GENERIC])]
  return list.slice(0, 6)
})

// 调查目标提示（防迷路）：从【当前剧本的旗标定义】推断下一步，剧本无关
const objective = computed(() => {
  if (atShowdown.value) return '当面指认你认定的真凶——指错或证据不足，对方会狡辩脱身，不会结束。'
  const f = state.value?.flags || {}
  const clean = (s) => (s || '').replace(/^【[^】]+】/, '')
  // 证据/证词类旗标：flag_name 以【证据】或【证词】开头（与调查手记同约定）
  const evidence = (flagDefs.value || []).filter((d) => /^【证据】|^【证词】/.test(d.flagName || ''))
  if (!evidence.length) return '四处走走，留意可疑的人和物，盘问在场的每一个人。'
  const missing = evidence.filter((d) => f[d.flagKey] !== true).map((d) => clean(d.flagName))
  if (!missing.length) return '关键线索已齐！找当事人当面对质，正式指认你认定的真凶。'
  return '继续搜证，还需找到：' + missing.join('、') + '。'
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
      flagDefs.value = sc?.flags || []
    } catch (e) { /* 静态资料拉取失败不阻断对局 */ }
  }

  // 新开局（仅一个回合且未结束）自动弹出背景说明；已结束则弹结局
  if (finished.value) showEnding.value = true
  else if (turns.value.length <= 1) showIntro.value = true
  scrollBottom()
}
onMounted(load)

// 后端回合不可取消：HTTP abort 拦不住已落库的事务，且并发会撞唯一键产生竞态。
// 故采用串行排队——思考时回车把行动排进 queue，当前回合返回后按序自动发送。
const submit = async () => {
  if (finished.value) return
  const text = input.value.trim()
  if (!text) return
  input.value = ''
  if (thinking.value) {
    queue.value.push(text) // 在途回合未结束，排队待发，不并发
    scrollBottom()
    return
  }
  await drain(text)
}

// 串行执行：先跑首条，再按序消费队列；任一回合失败或对局结束则停止并清队。
const drain = async (first) => {
  let text = first
  while (text != null) {
    const ok = await runOne(text)
    if (!ok || finished.value) break
    text = queue.value.length ? queue.value.shift() : null
  }
  if (queue.value.length && finished.value) {
    queue.value = []
    ElMessage.info('对局已结束，排队中的行动已取消')
  }
}

const runOne = async (text) => {
  pendingInput.value = text
  thinking.value = true
  scrollBottom()
  try {
    const r = await apiSubmitTurn(sessionId, text)
    pendingInput.value = ''
    turns.value.push(r.turn)
    state.value = r.state
    if (r.finished) {
      const d = await apiSessionDetail(sessionId)
      session.value = d.session
      scrollBottom()
      setTimeout(() => { showEnding.value = true }, 600) // 让结局叙事先渲染再弹收尾
    }
    scrollBottom()
    return true
  } catch (e) {
    input.value = text     // 恢复失败的输入，便于重试
    pendingInput.value = ''
    queue.value = []       // 剧情依赖上一回合，失败后不盲目续发排队
    return false
  } finally {
    thinking.value = false
  }
}
</script>

<style scoped>
.imm { height: 100vh; overflow: hidden; background: var(--imm-bg); color: var(--imm-text); display: flex; flex-direction: column; }
.imm-top { display: flex; align-items: center; height: 56px; padding: 0 24px; border-bottom: 1px solid var(--imm-border); }
.back { color: var(--imm-text-2); cursor: pointer; width: 120px; }
.imm-title { flex: 1; text-align: center; font-family: var(--font-serif); font-size: 18px; color: var(--imm-accent); }
.spacer { width: 120px; text-align: right; }
.help-btn { color: var(--imm-text-2); }
.banner { text-align: center; padding: 14px; font-size: 18px; font-weight: 700; font-family: var(--font-serif); }
.banner.win { background: rgba(240,178,92,.15); color: var(--imm-accent); }
.banner.lose { background: rgba(217,83,79,.15); color: #ff8a85; }
.imm-body { flex: 1; min-height: 0; display: flex; gap: 20px; max-width: 1120px; width: 100%; margin: 0 auto; padding: 20px 24px; overflow: hidden; }
.flow { flex: 1; min-width: 0; overflow-y: auto; padding-right: 8px; }
.turn { margin-bottom: 26px; }
.narrative { font-family: var(--font-serif); font-size: 17px; line-height: 1.9; color: var(--imm-narrative); margin: 12px 0; white-space: pre-wrap; }
.bubble { margin: 10px 0; padding: 12px 16px; border-radius: 14px; max-width: 80%; }
.bubble .who { font-size: 12px; color: var(--imm-text-2); display: block; margin-bottom: 4px; }
.bubble .text { line-height: 1.7; }
.bubble.player { background: var(--imm-player-bubble); margin-left: auto; border-bottom-right-radius: 4px; }
.bubble.player.queued { opacity: .55; border: 1px dashed var(--imm-border); }
.bubble.npc { background: var(--imm-npc-bubble); border-bottom-left-radius: 4px; }
.thinking { color: var(--imm-text-2); font-style: italic; padding: 8px 0; }
.side { width: 300px; flex-shrink: 0; align-self: flex-start; max-height: 100%; overflow-y: auto; position: sticky; top: 0; display: flex; flex-direction: column; gap: 16px; }
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
.ending { text-align: center; padding: 8px 4px; }
.ending-emoji { font-size: 56px; line-height: 1; }
.ending-title { font-family: var(--font-serif); font-size: 24px; font-weight: 700; margin: 12px 0 6px; }
.ending.win .ending-title { color: #c8881f; }
.ending.lose .ending-title { color: #c0392b; }
.ending-sub { color: #666; line-height: 1.7; margin-bottom: 16px; }
.ending-stats { display: flex; justify-content: center; gap: 18px; font-size: 14px; color: #333; }
.ending-stats span { background: #f3f4f8; border-radius: 10px; padding: 5px 14px; }
.ending-items { margin-top: 12px; font-size: 13px; color: #888; }
</style>

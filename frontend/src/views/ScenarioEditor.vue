<template>
  <div class="page">
    <TopBar />
    <div class="container">
      <a class="muted" @click="$router.push('/scenarios')">← 返回剧本列表</a>
      <h2 class="section-title" style="margin-top:8px">编辑状态机 · 剧本 #{{ sid }}</h2>

      <el-tabs v-model="tab">
        <!-- ============ 工作流画布 ============ -->
        <el-tab-pane label="状态机画布" name="flow">
          <div class="canvas" :class="{ full: isFull }" ref="canvasEl">
            <VueFlow
              v-model:nodes="flowNodes"
              v-model:edges="flowEdges"
              :default-viewport="{ zoom: 0.8 }"
              :min-zoom="0.15"
              :max-zoom="2"
              :default-edge-options="{ type: 'smoothstep', animated: false }"
              fit-view-on-init
              @node-double-click="onNodeDbl"
              @node-drag-stop="onDragStop"
              @edge-click="onEdgeClick"
              @connect="onConnect"
            >
              <Background :gap="22" :size="1.4" pattern-color="#d3d7e0" />
              <MiniMap pannable zoomable :node-color="miniColor" position="top-right" />
              <template #node-scene="{ data }">
                <div class="cz-node" :class="data.kind">
                  <Handle type="target" :position="Position.Left" class="cz-handle" />
                  <span class="cz-ico" :class="data.kind">{{ data.icon }}</span>
                  <div class="cz-body">
                    <div class="cz-title">{{ data.node.title }}</div>
                    <div class="cz-sub">{{ data.sub }}</div>
                  </div>
                  <span v-if="data.node.isEnding" class="cz-badge" :class="data.kind">{{ data.node.endingType }}</span>
                  <Handle type="source" :position="Position.Right" class="cz-handle" />
                </div>
              </template>
            </VueFlow>

            <!-- ===== Coze 风格底部浮动工具条 ===== -->
            <div class="cz-dock" @click.stop>
              <div class="cz-seg">
                <button class="cz-icbtn" title="缩小" @click="zoomOut({ duration: 200 })">－</button>
                <span class="cz-zoom" title="点击适配画布" @click="fitView({ padding: 0.2, duration: 300 })">{{ zoomPct }}%</span>
                <button class="cz-icbtn" title="放大" @click="zoomIn({ duration: 200 })">＋</button>
              </div>
              <button class="cz-icbtn solo" title="适配视野" @click="fitView({ padding: 0.2, duration: 300 })">⛶</button>
              <button class="cz-icbtn solo" :class="{ on: isFull }" :title="isFull ? '退出全屏 (Esc)' : '全屏沉浸'" @click="toggleFull">{{ isFull ? '⤡' : '⤢' }}</button>
              <span class="cz-div"></span>
              <button class="cz-txtbtn" title="一键分层整理" @click="autoLayout">⟳ 整理</button>
              <div class="cz-seg">
                <button class="cz-icbtn" :class="{ on: layoutDir==='LR' }" title="横向布局" @click="setDir('LR')">横</button>
                <button class="cz-icbtn" :class="{ on: layoutDir==='TB' }" title="纵向布局" @click="setDir('TB')">纵</button>
              </div>
              <span class="cz-div"></span>
              <div class="cz-add-wrap">
                <transition name="cz-pop">
                  <div v-if="showAdd" class="cz-add-panel" @click.stop>
                    <div class="cz-add-cat">场景 / 结局</div>
                    <div class="cz-add-row" @click="addPick('scene')"><span class="cz-ai scene">◆</span><div><b>场景节点</b><i>玩家可探索的地点 / 情节</i></div></div>
                    <div class="cz-add-row" @click="addPick('win')"><span class="cz-ai win">★</span><div><b>结局 · 胜利</b><i>满足条件触发 WIN 通关</i></div></div>
                    <div class="cz-add-row" @click="addPick('lose')"><span class="cz-ai lose">☠</span><div><b>结局 · 失败</b><i>理智耗尽等触发 LOSE</i></div></div>
                    <div class="cz-add-cat">资源</div>
                    <div class="cz-add-row" @click="addPick('npc')"><span class="cz-ai npc">☻</span><div><b>NPC 角色</b><i>AI 分饰的剧中人物</i></div></div>
                    <div class="cz-add-row" @click="addPick('tr')"><span class="cz-ai tr">↦</span><div><b>分支连线</b><i>白名单跳转 + 条件</i></div></div>
                  </div>
                </transition>
                <button class="cz-primary" @click="showAdd = !showAdd"><b>＋</b> 添加节点</button>
              </div>
              <button class="cz-play" title="保存并立刻试玩此剧本" @click="tryPlay">▶ 试玩</button>
            </div>
          </div>
        </el-tab-pane>

        <!-- ============ 节点表 ============ -->
        <el-tab-pane label="场景节点" name="nodes">
          <el-button type="primary" :icon="Plus" size="small" @click="openNode()">新增节点</el-button>
          <el-table :data="nodes" stripe style="margin-top:12px">
            <el-table-column prop="id" label="ID" width="70" />
            <el-table-column prop="nodeKey" label="key" width="140" />
            <el-table-column prop="title" label="标题" width="140" />
            <el-table-column prop="narrativeBrief" label="叙事提示" show-overflow-tooltip />
            <el-table-column label="结局" width="100">
              <template #default="{ row }">
                <el-tag v-if="row.isEnding" :type="row.endingType === 'WIN' ? 'success' : 'danger'" size="small">{{ row.endingType }}</el-tag>
                <span v-else class="muted">—</span>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="140">
              <template #default="{ row }">
                <el-button size="small" @click="openNode(row)">改</el-button>
                <el-button size="small" type="danger" plain @click="delNode(row)">删</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <!-- ============ NPC 表 ============ -->
        <el-tab-pane label="NPC" name="npcs">
          <el-button type="primary" :icon="Plus" size="small" @click="openNpc()">新增 NPC</el-button>
          <el-table :data="npcs" stripe style="margin-top:12px">
            <el-table-column prop="id" label="ID" width="70" />
            <el-table-column prop="npcKey" label="key" width="140" />
            <el-table-column prop="name" label="名称" width="140" />
            <el-table-column prop="persona" label="人设" show-overflow-tooltip />
            <el-table-column label="操作" width="140">
              <template #default="{ row }">
                <el-button size="small" @click="openNpc(row)">改</el-button>
                <el-button size="small" type="danger" plain @click="delNpc(row)">删</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <!-- ============ 分支表 ============ -->
        <el-tab-pane label="分支(白名单)" name="transitions">
          <el-button type="primary" :icon="Plus" size="small" @click="openTr()">新增分支</el-button>
          <el-table :data="transitions" stripe style="margin-top:12px">
            <el-table-column prop="id" label="ID" width="70" />
            <el-table-column label="from → to" width="160">
              <template #default="{ row }">{{ row.fromNodeId }} → {{ row.toNodeId }}</template>
            </el-table-column>
            <el-table-column prop="conditionExpr" label="condition" width="220" />
            <el-table-column prop="priority" label="优先级" width="90" />
            <el-table-column prop="description" label="说明" show-overflow-tooltip />
            <el-table-column label="操作" width="120">
              <template #default="{ row }">
                <el-button size="small" @click="openTr(row)">改</el-button>
                <el-button size="small" type="danger" plain @click="delTr(row)">删</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </div>

    <!-- 节点编辑：右侧面板 -->
    <el-drawer v-model="nodeDlg" :title="nodeForm.id ? '编辑场景节点' : '新增场景节点'" direction="rtl" :size="330" :modal="false" class="cz-drawer">
      <el-form :model="nodeForm" label-width="68px">
        <el-form-item label="选择已有">
          <el-select v-model="pickNode" placeholder="新建，或选择已有场景来编辑" clearable filterable style="width:100%" @change="onPickNode">
            <el-option v-for="n in nodes" :key="n.id" :label="n.title" :value="n.id" />
          </el-select>
        </el-form-item>
        <el-divider style="margin:4px 0 16px" />
        <el-form-item label="标识键"><el-input v-model="nodeForm.nodeKey" :disabled="!!nodeForm.id" placeholder="node_xxx（英文，仅供内部）" /></el-form-item>
        <el-form-item label="标题"><el-input v-model="nodeForm.title" placeholder="中文场景名，如：雨夜花园" /></el-form-item>
        <el-form-item label="叙事提示"><el-input v-model="nodeForm.narrativeBrief" type="textarea" :rows="5" placeholder="给 AI 的导演提示：本场景发生什么、可获得什么线索…" /></el-form-item>
        <el-form-item label="结局节点"><el-switch v-model="nodeForm.isEnding" /></el-form-item>
        <el-form-item v-if="nodeForm.isEnding" label="结局类型">
          <el-select v-model="nodeForm.endingType"><el-option label="WIN 通关" value="WIN" /><el-option label="LOSE 失败" value="LOSE" /></el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button v-if="nodeForm.id" type="danger" plain style="float:left" @click="delNode(nodeForm)">删除</el-button>
        <el-button @click="nodeDlg=false">取消</el-button><el-button type="primary" @click="saveNode">保存</el-button>
      </template>
    </el-drawer>

    <!-- NPC 编辑：右侧面板 -->
    <el-drawer v-model="npcDlg" :title="npcForm.id ? '编辑 NPC' : '新增 NPC'" direction="rtl" :size="330" :modal="false" class="cz-drawer">
      <el-form :model="npcForm" label-width="68px">
        <el-form-item label="选择已有">
          <el-select v-model="pickNpc" placeholder="新建，或选择已有 NPC 来编辑" clearable filterable style="width:100%" @change="onPickNpc">
            <el-option v-for="n in npcs" :key="n.id" :label="n.name" :value="n.id" />
          </el-select>
        </el-form-item>
        <el-divider style="margin:4px 0 16px" />
        <el-form-item label="标识键"><el-input v-model="npcForm.npcKey" :disabled="!!npcForm.id" placeholder="npc_xxx（英文，仅供内部）" /></el-form-item>
        <el-form-item label="名称"><el-input v-model="npcForm.name" placeholder="中文人物名，如：管家霍金斯" /></el-form-item>
        <el-form-item label="人设"><el-input v-model="npcForm.persona" type="textarea" :rows="2" /></el-form-item>
        <el-form-item label="背景"><el-input v-model="npcForm.background" type="textarea" :rows="2" /></el-form-item>
        <el-form-item label="秘密"><el-input v-model="npcForm.secret" type="textarea" :rows="2" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button v-if="npcForm.id" type="danger" plain style="float:left" @click="delNpc(npcForm)">删除</el-button>
        <el-button @click="npcDlg=false">取消</el-button><el-button type="primary" @click="saveNpc">保存</el-button>
      </template>
    </el-drawer>

    <!-- 分支编辑：右侧面板 -->
    <el-drawer v-model="trDlg" :title="trForm.id ? '编辑分支' : '新增分支'" direction="rtl" :size="330" :modal="false" class="cz-drawer">
      <el-form :model="trForm" label-width="68px">
        <el-form-item label="源节点">
          <el-select v-model="trForm.fromNodeId" filterable :disabled="!!trForm.id" style="width:100%"><el-option v-for="n in nodes" :key="n.id" :label="n.title" :value="n.id" /></el-select>
        </el-form-item>
        <el-form-item label="目标节点">
          <el-select v-model="trForm.toNodeId" filterable :disabled="!!trForm.id" style="width:100%"><el-option v-for="n in nodes" :key="n.id" :label="n.title" :value="n.id" /></el-select>
        </el-form-item>
        <el-form-item label="触发条件">
          <el-input v-model="trForm.conditionExpr" placeholder="always" />
        </el-form-item>
        <el-form-item label=" ">
          <div class="muted" style="font-size:12px;line-height:1.6">
            语法：<code>always</code> / <code>flag.has_key==true</code> / <code>attr.evidence&gt;=2</code> /
            <code>item.钥匙</code>，用 <code>&amp;&amp;</code> 串联（不支持 ||）
          </div>
        </el-form-item>
        <el-form-item label="优先级"><el-input-number v-model="trForm.priority" :min="0" /></el-form-item>
        <el-form-item label="说明"><el-input v-model="trForm.description" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button v-if="trForm.id" type="danger" plain style="float:left" @click="delTr(trForm)">删除</el-button>
        <el-button @click="trDlg=false">取消</el-button><el-button type="primary" @click="saveTr">保存</el-button>
      </template>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, watch, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, MagicStick } from '@element-plus/icons-vue'
import { VueFlow, Handle, Position, useVueFlow } from '@vue-flow/core'
import { Background } from '@vue-flow/background'
import { MiniMap } from '@vue-flow/minimap'
import dagre from '@dagrejs/dagre'
import '@vue-flow/core/dist/style.css'
import '@vue-flow/core/dist/theme-default.css'
import '@vue-flow/controls/dist/style.css'
import '@vue-flow/minimap/dist/style.css'
import TopBar from '../components/TopBar.vue'
import {
  apiNodes, apiCreateNode, apiUpdateNode, apiDeleteNode,
  apiNpcs, apiCreateNpc, apiUpdateNpc, apiDeleteNpc,
  apiTransitions, apiCreateTransition, apiUpdateTransition, apiDeleteTransition,
  apiStartSession, apiScenarioDetail,
} from '../api'

const route = useRoute()
const sid = route.params.id
const tab = ref('flow')
const nodes = ref([]); const npcs = ref([]); const transitions = ref([])
const flowNodes = ref([]); const flowEdges = ref([])
const layoutDir = ref(localStorage.getItem(`aigm_flow_dir_${sid}`) || 'LR')
const router = useRouter()
const { fitView, zoomIn, zoomOut, viewport } = useVueFlow()

// 底部工具条：实时缩放百分比
const zoomPct = ref(80)
watch(viewport, (v) => { if (v?.zoom) zoomPct.value = Math.round(v.zoom * 100) }, { deep: true, immediate: true })

const setDir = (d) => { if (layoutDir.value === d) return; layoutDir.value = d; autoLayout() }

// 全屏沉浸：把画布 DOM 整块搬到 body 下 + CSS 铺满视口
// （不用原生 Fullscreen API——它强依赖真实手势、受控环境常静默失败；
//  搬到 body 后脱离一切祖先，position:fixed 必然生效，且无需任何权限）
const isFull = ref(false)
const canvasEl = ref(null)
let fsPlaceholder = null
const toggleFull = () => {
  const el = canvasEl.value
  if (!el) return
  if (!isFull.value) {
    fsPlaceholder = document.createComment('canvas-placeholder')
    el.parentNode.insertBefore(fsPlaceholder, el)
    document.body.appendChild(el)       // 搬到 body，移动节点不会重挂载 VueFlow 实例
    isFull.value = true
  } else if (fsPlaceholder) {
    fsPlaceholder.parentNode.insertBefore(el, fsPlaceholder)  // 放回原位
    fsPlaceholder.remove(); fsPlaceholder = null
    isFull.value = false
  }
  nextTick(() => setTimeout(() => fitView({ padding: 0.18, duration: 300 }), 130))
}
const onKey = (e) => { if (e.key === 'Escape' && isFull.value) toggleFull() }
onMounted(() => document.addEventListener('keydown', onKey))
onBeforeUnmount(() => {
  document.removeEventListener('keydown', onKey)
  if (fsPlaceholder && canvasEl.value) { fsPlaceholder.parentNode?.insertBefore(canvasEl.value, fsPlaceholder); fsPlaceholder.remove() }
})

// 添加节点弹面板
const showAdd = ref(false)
const addPick = (kind) => {
  showAdd.value = false
  if (kind === 'scene') openNode()
  else if (kind === 'win') openNode({ isEnding: true, endingType: 'WIN' })
  else if (kind === 'lose') openNode({ isEnding: true, endingType: 'LOSE' })
  else if (kind === 'npc') openNpc()
  else if (kind === 'tr') openTr()
}
const closeAdd = () => { showAdd.value = false }
onMounted(() => document.addEventListener('click', closeAdd))
onBeforeUnmount(() => document.removeEventListener('click', closeAdd))

// 试玩：保存当前剧本状态机 → 直接开局跳到游玩页
const tryPlay = async () => {
  try {
    const data = await apiStartSession(Number(sid))
    router.push(`/play/${data.sessionId}`)
  } catch (e) { /* 拦截器已提示 */ }
}

const posKey = `aigm_flow_pos_${sid}`
const loadPos = () => { try { return JSON.parse(localStorage.getItem(posKey)) || {} } catch { return {} } }
const savePos = (m) => localStorage.setItem(posKey, JSON.stringify(m))

const NODE_W = 200, NODE_H = 60
// 节点种类：起点(无入边) / 胜利 / 失败 / 普通场景
let incoming = {}
const kindOf = (n) => {
  if (n.isEnding) return n.endingType === 'WIN' ? 'win' : 'lose'
  return (incoming[n.id] || 0) === 0 ? 'start' : 'scene'
}
const iconOf = (k) => ({ start: '▶', win: '★', lose: '☠', scene: '◆' }[k] || '◆')
const labelOf = (k) => ({ start: '起点 · 开局', win: '结局 · 胜利', lose: '结局 · 失败', scene: '场景' }[k] || '场景')
// 副标题：用叙事提示首句让每个场景可区分；没有则回退类型名
const subOf = (n, k) => {
  const brief = (n.narrativeBrief || '').replace(/\s+/g, '').split(/[。.,，;；!！?？]/)[0]
  const tag = labelOf(k)
  if (!brief) return tag
  const snip = brief.length > 18 ? brief.slice(0, 18) + '…' : brief
  return k === 'scene' ? snip : `${tag} · ${snip}`
}
const miniColor = (n) => ({ start: '#22c55e', win: '#16a34a', lose: '#ef4444', scene: '#6366f1' }[n.data?.kind] || '#6366f1')

// 旗标/属性 中文名映射（条件标签翻成人话）
const flagMap = ref({})
const attrName = (k) => ({ sanity: '理智', evidence: '证据', trust: '信任' }[k] || k)
const flagName = (k) => (flagMap.value[k] || k).replace(/^【.*?】/, '')
// 把白名单小语法翻成中文：flag.x==true → 「已发现凶器」；attr.sanity<=0 → 「理智 ≤ 0」
const prettyCond = (expr) => {
  if (!expr || expr === 'always') return ''
  return expr.split('&&').map((raw) => {
    const t = raw.trim()
    let m
    if ((m = t.match(/^flag\.(.+?)==true$/))) return flagName(m[1])
    if ((m = t.match(/^flag\.(.+?)==false$/))) return '未' + flagName(m[1])
    if ((m = t.match(/^attr\.(.+?)(>=|<=|>|<|==)(-?\d+)$/))) {
      const op = { '>=': '≥', '<=': '≤', '>': '>', '<': '<', '==': '=' }[m[2]]
      return `${attrName(m[1])} ${op} ${m[3]}`
    }
    if ((m = t.match(/^item\.(.+)$/))) return `持有「${m[1]}」`
    return t
  }).join(' 且 ')
}

// dagre 分层布局，消除连线交叉
const dagreLayout = (dir) => {
  const g = new dagre.graphlib.Graph()
  g.setDefaultEdgeLabel(() => ({}))
  g.setGraph({ rankdir: dir, nodesep: dir === 'LR' ? 36 : 60, ranksep: dir === 'LR' ? 120 : 90, marginx: 24, marginy: 24 })
  nodes.value.forEach((n) => g.setNode(String(n.id), { width: NODE_W, height: NODE_H }))
  transitions.value.forEach((t) => g.setEdge(String(t.fromNodeId), String(t.toNodeId)))
  dagre.layout(g)
  const m = {}
  nodes.value.forEach((n) => { const p = g.node(String(n.id)); if (p) m[n.id] = { x: Math.round(p.x - NODE_W / 2), y: Math.round(p.y - NODE_H / 2) } })
  return m
}

const buildFlow = () => {
  incoming = {}
  transitions.value.forEach((t) => { incoming[t.toNodeId] = (incoming[t.toNodeId] || 0) + 1 })
  let pos = loadPos()
  if (!Object.keys(pos).length) pos = dagreLayout(layoutDir.value)  // 首次进入自动分层
  flowNodes.value = nodes.value.map((n, i) => {
    const k = kindOf(n)
    return {
      id: String(n.id), type: 'scene',
      position: pos[n.id] || { x: (i % 5) * 230 + 30, y: Math.floor(i / 5) * 130 + 20 },
      data: { node: n, kind: k, icon: iconOf(k), sub: subOf(n, k) },
    }
  })
  flowEdges.value = transitions.value.map((t) => ({
    id: 'e' + t.id,
    source: String(t.fromNodeId), target: String(t.toNodeId),
    label: prettyCond(t.conditionExpr),
    data: { t }, animated: false, type: 'smoothstep',
    markerEnd: 'arrowclosed',
    style: { stroke: '#9aa3c2', strokeWidth: 1.6 },
    labelBgStyle: { fill: '#eef0f7' }, labelBgPadding: [6, 3], labelBgBorderRadius: 6,
    labelStyle: { fill: '#5b6178', fontSize: 11 },
  }))
}

const autoLayout = () => {
  localStorage.setItem(`aigm_flow_dir_${sid}`, layoutDir.value)
  const m = dagreLayout(layoutDir.value)
  savePos(m); buildFlow()
  nextTick(() => setTimeout(() => fitView({ padding: 0.2, duration: 400 }), 60))
}

const load = async () => {
  try {
    const detail = await apiScenarioDetail(sid)
    const fm = {}; (detail?.flags || []).forEach((f) => { fm[f.flagKey] = f.flagName }); flagMap.value = fm
  } catch { /* 旗标名缺失时条件标签回退英文键 */ }
  nodes.value = await apiNodes(sid) || []
  npcs.value = await apiNpcs(sid) || []
  transitions.value = await apiTransitions(sid) || []
  buildFlow()
}
onMounted(load)

// ---- 画布事件 ----
const onDragStop = ({ node }) => {
  const m = loadPos(); m[node.id] = { x: Math.round(node.position.x), y: Math.round(node.position.y) }; savePos(m)
}
const onNodeDbl = ({ node }) => { const n = nodes.value.find((x) => String(x.id) === node.id); if (n) openNode(n) }
const onEdgeClick = ({ edge }) => { const t = edge.data?.t; if (t) openTr(t) }
const onConnect = async ({ source, target }) => {
  await apiCreateTransition({ scenarioId: Number(sid), fromNodeId: Number(source), toNodeId: Number(target), conditionExpr: 'always', priority: 10, description: '' })
  ElMessage.success('已新建分支，点连线可改条件'); await load()
}

// ---- 节点 ----
const nodeDlg = ref(false)
const nodeForm = ref({})
const pickNode = ref(null)
const openNode = (row) => { nodeForm.value = row ? { ...row } : { isEnding: false }; pickNode.value = row?.id ?? null; nodeDlg.value = true }
const onPickNode = (id) => { const n = nodes.value.find((x) => x.id === id); nodeForm.value = n ? { ...n } : { isEnding: false } }
const saveNode = async () => {
  const f = nodeForm.value
  if (f.id) await apiUpdateNode(f.id, f)
  else await apiCreateNode(sid, f)
  ElMessage.success('已保存'); nodeDlg.value = false; await load()
}
const delNode = async (row) => { await ElMessageBox.confirm('删除该节点？将级联删相关分支', '提示', { type: 'warning' }); await apiDeleteNode(row.id); nodeDlg.value = false; await load() }

// ---- NPC ----
const npcDlg = ref(false)
const npcForm = ref({})
const pickNpc = ref(null)
const openNpc = (row) => { npcForm.value = row ? { ...row } : {}; pickNpc.value = row?.id ?? null; npcDlg.value = true }
const onPickNpc = (id) => { const n = npcs.value.find((x) => x.id === id); npcForm.value = n ? { ...n } : {} }
const saveNpc = async () => {
  const f = npcForm.value
  if (f.id) await apiUpdateNpc(f.id, f)
  else await apiCreateNpc(sid, f)
  ElMessage.success('已保存'); npcDlg.value = false; await load()
}
const delNpc = async (row) => { await ElMessageBox.confirm('删除该 NPC？', '提示', { type: 'warning' }); await apiDeleteNpc(row.id); npcDlg.value = false; await load() }

// ---- 分支 ----
const trDlg = ref(false)
const trForm = ref({})
const openTr = (row) => { trForm.value = row ? { ...row } : { scenarioId: Number(sid), conditionExpr: 'always', priority: 10 }; trDlg.value = true }
const saveTr = async () => {
  const f = trForm.value
  if (f.id) await apiUpdateTransition(f.id, f)
  else await apiCreateTransition({ ...f, scenarioId: Number(sid) })
  ElMessage.success('已保存'); trDlg.value = false; await load()
}
const delTr = async (row) => {
  await ElMessageBox.confirm('删除该分支？', '提示', { type: 'warning' })
  await apiDeleteTransition(row.id); trDlg.value = false; await load()
}
</script>

<style scoped>
.canvas { position: relative; height: 76vh; border: 1px solid #e6e8ef; border-radius: 14px; overflow: hidden; background: #f7f8fb; }
.canvas.full { position: fixed; inset: 0; height: 100vh; width: 100vw; border-radius: 0; border: none; z-index: 1998; }

/* ===== Coze 底部浮动工具条 ===== */
.cz-dock {
  position: absolute; left: 50%; bottom: 18px; transform: translateX(-50%); z-index: 6;
  display: flex; align-items: center; gap: 8px; white-space: nowrap;
  padding: 7px 9px; border-radius: 14px; background: #fff;
  border: 1px solid #e6e8ef; box-shadow: 0 8px 28px rgba(28,35,80,.16);
}
.cz-dock button { white-space: nowrap; flex: none; }
.cz-div { width: 1px; height: 22px; background: #e6e8ef; }
.cz-seg { display: flex; align-items: center; gap: 2px; background: #f3f4f9; border-radius: 9px; padding: 3px; }
.cz-icbtn { border: none; background: transparent; cursor: pointer; min-width: 30px; height: 28px; padding: 0 8px; border-radius: 7px; font-size: 14px; color: #4b5168; display: grid; place-items: center; transition: background .12s, color .12s; }
.cz-icbtn:hover { background: #e7e9f3; }
.cz-icbtn.on { background: #fff; color: #6366f1; box-shadow: 0 1px 3px rgba(28,35,80,.12); font-weight: 600; }
.cz-icbtn.solo { background: #f3f4f9; }
.cz-zoom { min-width: 46px; text-align: center; font-size: 13px; color: #4b5168; cursor: pointer; user-select: none; }
.cz-zoom:hover { color: #6366f1; }
.cz-txtbtn { border: none; background: transparent; cursor: pointer; height: 32px; padding: 0 10px; border-radius: 8px; font-size: 13px; color: #4b5168; transition: background .12s; }
.cz-txtbtn:hover { background: #f3f4f9; color: #6366f1; }

.cz-add-wrap { position: relative; }
.cz-primary { border: none; cursor: pointer; height: 34px; padding: 0 16px; border-radius: 9px; font-size: 13px; font-weight: 600; color: #fff; background: linear-gradient(135deg,#6a6ff5,#7c5cff); box-shadow: 0 3px 10px rgba(108,99,255,.32); display: inline-flex; align-items: center; gap: 5px; }
.cz-primary:hover { filter: brightness(1.06); }
.cz-primary b { font-size: 16px; line-height: 1; }
.cz-play { border: none; cursor: pointer; height: 34px; padding: 0 18px; border-radius: 9px; font-size: 13px; font-weight: 600; color: #fff; background: linear-gradient(135deg,#22c55e,#16a34a); box-shadow: 0 3px 10px rgba(22,163,74,.3); }
.cz-play:hover { filter: brightness(1.06); }

/* 添加节点弹出面板 */
.cz-add-panel { position: absolute; bottom: 46px; left: 50%; transform: translateX(-50%); width: 268px; background: #fff; border: 1px solid #e6e8ef; border-radius: 14px; box-shadow: 0 12px 34px rgba(28,35,80,.18); padding: 8px; }
.cz-add-cat { font-size: 11px; color: #9aa0b4; font-weight: 600; padding: 8px 8px 4px; }
.cz-add-row { display: flex; align-items: center; gap: 10px; padding: 8px; border-radius: 10px; cursor: pointer; transition: background .12s; }
.cz-add-row:hover { background: #f3f4f9; }
.cz-add-row b { display: block; font-size: 13px; color: #1f2330; font-weight: 600; }
.cz-add-row i { display: block; font-size: 11px; color: #9aa0b4; font-style: normal; margin-top: 1px; }
.cz-ai { flex: none; width: 30px; height: 30px; border-radius: 8px; display: grid; place-items: center; color: #fff; font-size: 14px; background: #6366f1; }
.cz-ai.win { background: #16a34a; } .cz-ai.lose { background: #ef4444; }
.cz-ai.npc { background: #f59e0b; } .cz-ai.tr { background: #0ea5e9; }
.cz-pop-enter-active, .cz-pop-leave-active { transition: opacity .15s, transform .15s; }
.cz-pop-enter-from, .cz-pop-leave-to { opacity: 0; transform: translateX(-50%) translateY(6px); }

/* ===== Coze 风格节点 ===== */
.cz-node {
  position: relative; display: flex; align-items: center; gap: 10px;
  width: 200px; box-sizing: border-box; padding: 12px 14px;
  border-radius: 12px; background: #fff; border: 1px solid #e6e8ef;
  box-shadow: 0 2px 10px rgba(28,35,80,.06); transition: box-shadow .15s, border-color .15s, transform .1s;
  cursor: grab;
}
.cz-node:hover { box-shadow: 0 6px 20px rgba(28,35,80,.13); border-color: #c5cbe0; }
.cz-node.start { border-color: #b7ecc7; }
.cz-node.win { border-color: #b7ecc7; background: #f4fcf6; }
.cz-node.lose { border-color: #f6c2c0; background: #fef6f5; }

.cz-ico {
  flex: none; width: 30px; height: 30px; border-radius: 8px;
  display: grid; place-items: center; font-size: 14px; color: #fff;
  background: #6366f1;
}
.cz-ico.start { background: #22c55e; }
.cz-ico.win { background: #16a34a; }
.cz-ico.lose { background: #ef4444; }

.cz-body { min-width: 0; flex: 1; }
.cz-title { font-size: 14px; font-weight: 600; color: #1f2330; line-height: 1.25; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.cz-sub { font-size: 11px; color: #9aa0b4; margin-top: 2px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }

.cz-badge { position: absolute; top: -8px; right: 10px; font-size: 10px; font-weight: 700; padding: 1px 7px; border-radius: 999px; color: #fff; }
.cz-badge.win { background: #16a34a; }
.cz-badge.lose { background: #ef4444; }

/* 端口：Coze 风格的小圆点，悬停放大 */
:deep(.cz-handle) { width: 10px; height: 10px; background: #fff; border: 2px solid #b3b9d4; transition: transform .1s, border-color .1s; }
:deep(.cz-node:hover .cz-handle) { border-color: #6366f1; }
:deep(.cz-handle:hover) { transform: scale(1.35); border-color: #6366f1; }

:deep(.vue-flow__edge-text) { fill: #5b6178; font-size: 11px; }
:deep(.vue-flow__edge.selected .vue-flow__edge-path),
:deep(.vue-flow__edge:hover .vue-flow__edge-path) { stroke: #6366f1 !important; stroke-width: 2.2 !important; }
:deep(.vue-flow__controls) { box-shadow: 0 2px 10px rgba(28,35,80,.12); border-radius: 8px; overflow: hidden; }
:deep(.vue-flow__minimap) { border-radius: 10px; box-shadow: 0 2px 10px rgba(28,35,80,.1); }

/* 右侧编辑面板：紧凑精致 */
.cz-drawer :deep(.el-drawer) { box-shadow: -8px 0 30px rgba(28,35,80,.12); }
.cz-drawer :deep(.el-drawer__header) { margin-bottom: 4px; padding: 16px 18px 10px; font-size: 15px; font-weight: 700; color: #1f2330; border-bottom: 1px solid #f0f1f5; }
.cz-drawer :deep(.el-drawer__body) { padding: 14px 18px; }
.cz-drawer :deep(.el-drawer__footer) { padding: 12px 18px; border-top: 1px solid #f0f1f5; }
.cz-drawer :deep(.el-form-item) { margin-bottom: 14px; }
.cz-drawer :deep(.el-form-item__label) { font-size: 12px; color: #8a90a6; padding-right: 8px; }
.cz-drawer :deep(.el-input__wrapper),
.cz-drawer :deep(.el-textarea__inner) { border-radius: 8px; }
.cz-drawer :deep(.el-input) { font-size: 13px; }
.cz-drawer :deep(.el-divider--horizontal) { margin: 2px 0 14px; }
.cz-drawer :deep(.el-button) { border-radius: 8px; }
</style>

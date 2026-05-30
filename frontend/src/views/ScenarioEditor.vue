<template>
  <div class="page">
    <TopBar />
    <div class="container">
      <a class="muted" @click="$router.push('/scenarios')">← 返回剧本列表</a>
      <h2 class="section-title" style="margin-top:8px">编辑状态机 · 剧本 #{{ sid }}</h2>

      <el-tabs v-model="tab">
        <!-- ============ 工作流画布 ============ -->
        <el-tab-pane label="状态机画布" name="flow">
          <div class="canvas-toolbar">
            <el-button type="primary" :icon="Plus" size="small" @click="openNode()">新增场景</el-button>
            <el-button size="small" @click="autoLayout">自动布局</el-button>
            <span class="muted hint">拖动=移动 · 双击节点=编辑 · 从节点右侧小圆点拉到另一个节点=新建分支 · 点连线=改条件</span>
          </div>
          <div class="canvas">
            <VueFlow
              v-model:nodes="flowNodes"
              v-model:edges="flowEdges"
              :default-viewport="{ zoom: 0.85 }"
              :min-zoom="0.2"
              :max-zoom="2"
              fit-view-on-init
              @node-double-click="onNodeDbl"
              @node-drag-stop="onDragStop"
              @edge-click="onEdgeClick"
              @connect="onConnect"
            >
              <Background pattern-color="#2a2f45" :gap="20" />
              <Controls />
              <template #node-scene="{ data }">
                <div class="vf-node" :class="data.cls">
                  <Handle type="target" :position="Position.Left" />
                  <div class="vf-node-id">#{{ data.node.id }}</div>
                  <div class="vf-node-title">{{ data.node.title }}</div>
                  <div v-if="data.node.isEnding" class="vf-node-tag">{{ data.node.endingType }}</div>
                  <Handle type="source" :position="Position.Right" />
                </div>
              </template>
            </VueFlow>
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

    <!-- 节点对话框 -->
    <el-dialog v-model="nodeDlg" :title="nodeForm.id ? '编辑节点' : '新增节点'" width="500px">
      <el-form :model="nodeForm" label-width="90px">
        <el-form-item label="nodeKey"><el-input v-model="nodeForm.nodeKey" :disabled="!!nodeForm.id" placeholder="node_xxx" /></el-form-item>
        <el-form-item label="标题"><el-input v-model="nodeForm.title" /></el-form-item>
        <el-form-item label="叙事提示"><el-input v-model="nodeForm.narrativeBrief" type="textarea" :rows="3" /></el-form-item>
        <el-form-item label="结局节点"><el-switch v-model="nodeForm.isEnding" /></el-form-item>
        <el-form-item v-if="nodeForm.isEnding" label="结局类型">
          <el-select v-model="nodeForm.endingType"><el-option label="WIN" value="WIN" /><el-option label="LOSE" value="LOSE" /></el-select>
        </el-form-item>
      </el-form>
      <template #footer><el-button @click="nodeDlg=false">取消</el-button><el-button type="primary" @click="saveNode">保存</el-button></template>
    </el-dialog>

    <!-- NPC 对话框 -->
    <el-dialog v-model="npcDlg" :title="npcForm.id ? '编辑NPC' : '新增NPC'" width="500px">
      <el-form :model="npcForm" label-width="90px">
        <el-form-item label="npcKey"><el-input v-model="npcForm.npcKey" :disabled="!!npcForm.id" placeholder="npc_xxx" /></el-form-item>
        <el-form-item label="名称"><el-input v-model="npcForm.name" /></el-form-item>
        <el-form-item label="人设"><el-input v-model="npcForm.persona" type="textarea" :rows="2" /></el-form-item>
        <el-form-item label="背景"><el-input v-model="npcForm.background" type="textarea" :rows="2" /></el-form-item>
        <el-form-item label="秘密"><el-input v-model="npcForm.secret" type="textarea" :rows="2" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="npcDlg=false">取消</el-button><el-button type="primary" @click="saveNpc">保存</el-button></template>
    </el-dialog>

    <!-- 分支对话框（新增/编辑） -->
    <el-dialog v-model="trDlg" :title="trForm.id ? '编辑分支' : '新增分支'" width="500px">
      <el-form :model="trForm" label-width="90px">
        <el-form-item label="源节点">
          <el-select v-model="trForm.fromNodeId" filterable :disabled="!!trForm.id"><el-option v-for="n in nodes" :key="n.id" :label="`${n.id} ${n.title}`" :value="n.id" /></el-select>
        </el-form-item>
        <el-form-item label="目标节点">
          <el-select v-model="trForm.toNodeId" filterable :disabled="!!trForm.id"><el-option v-for="n in nodes" :key="n.id" :label="`${n.id} ${n.title}`" :value="n.id" /></el-select>
        </el-form-item>
        <el-form-item label="condition">
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
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { VueFlow, Handle, Position } from '@vue-flow/core'
import { Background } from '@vue-flow/background'
import { Controls } from '@vue-flow/controls'
import '@vue-flow/core/dist/style.css'
import '@vue-flow/core/dist/theme-default.css'
import '@vue-flow/controls/dist/style.css'
import TopBar from '../components/TopBar.vue'
import {
  apiNodes, apiCreateNode, apiUpdateNode, apiDeleteNode,
  apiNpcs, apiCreateNpc, apiUpdateNpc, apiDeleteNpc,
  apiTransitions, apiCreateTransition, apiUpdateTransition, apiDeleteTransition,
} from '../api'

const route = useRoute()
const sid = route.params.id
const tab = ref('flow')
const nodes = ref([]); const npcs = ref([]); const transitions = ref([])
const flowNodes = ref([]); const flowEdges = ref([])

const posKey = `aigm_flow_pos_${sid}`
const loadPos = () => { try { return JSON.parse(localStorage.getItem(posKey)) || {} } catch { return {} } }
const savePos = (m) => localStorage.setItem(posKey, JSON.stringify(m))

const nodeCls = (n) => (n.isEnding ? (n.endingType === 'WIN' ? 'win' : 'lose') : '')

const buildFlow = () => {
  const pos = loadPos()
  flowNodes.value = nodes.value.map((n, i) => ({
    id: String(n.id),
    type: 'scene',
    position: pos[n.id] || { x: (i % 4) * 240 + 30, y: Math.floor(i / 4) * 150 + 20 },
    data: { node: n, cls: nodeCls(n) },
  }))
  flowEdges.value = transitions.value.map((t) => ({
    id: 'e' + t.id,
    source: String(t.fromNodeId),
    target: String(t.toNodeId),
    label: t.conditionExpr === 'always' ? '' : t.conditionExpr,
    data: { t },
    animated: false,
    markerEnd: 'arrowclosed',
    style: { stroke: '#5b6ee1' },
  }))
}

const autoLayout = () => {
  const m = {}
  nodes.value.forEach((n, i) => { m[n.id] = { x: (i % 4) * 240 + 30, y: Math.floor(i / 4) * 150 + 20 } })
  savePos(m); buildFlow()
}

const load = async () => {
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
const openNode = (row) => { nodeForm.value = row ? { ...row } : { isEnding: false }; nodeDlg.value = true }
const saveNode = async () => {
  const f = nodeForm.value
  if (f.id) await apiUpdateNode(f.id, f)
  else await apiCreateNode(sid, f)
  ElMessage.success('已保存'); nodeDlg.value = false; await load()
}
const delNode = async (row) => { await ElMessageBox.confirm('删除该节点？将级联删相关分支', '提示', { type: 'warning' }); await apiDeleteNode(row.id); await load() }

// ---- NPC ----
const npcDlg = ref(false)
const npcForm = ref({})
const openNpc = (row) => { npcForm.value = row ? { ...row } : {}; npcDlg.value = true }
const saveNpc = async () => {
  const f = npcForm.value
  if (f.id) await apiUpdateNpc(f.id, f)
  else await apiCreateNpc(sid, f)
  ElMessage.success('已保存'); npcDlg.value = false; await load()
}
const delNpc = async (row) => { await ElMessageBox.confirm('删除该 NPC？', '提示', { type: 'warning' }); await apiDeleteNpc(row.id); await load() }

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
.canvas-toolbar { display: flex; align-items: center; gap: 12px; margin-bottom: 10px; }
.canvas-toolbar .hint { font-size: 12px; }
.canvas { height: 70vh; border: 1px solid var(--border, #dcdfe6); border-radius: 10px; overflow: hidden; background: #161a2b; }
.vf-node { position: relative; min-width: 130px; padding: 10px 12px; border-radius: 10px; background: #232842; border: 1px solid #3a4163; color: #e8ebf5; box-shadow: 0 2px 8px rgba(0,0,0,.25); }
.vf-node.win { border-color: #6ad08a; background: #20322a; }
.vf-node.lose { border-color: #d9534f; background: #33232a; }
.vf-node-id { font-size: 11px; color: #8a93b5; }
.vf-node-title { font-size: 14px; font-weight: 600; margin-top: 2px; }
.vf-node-tag { position: absolute; top: 6px; right: 8px; font-size: 10px; color: #f0b25c; }
:deep(.vue-flow__edge-text) { fill: #aab6ff; font-size: 11px; }
:deep(.vue-flow__handle) { width: 9px; height: 9px; background: #5b6ee1; border: none; }
</style>

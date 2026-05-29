<template>
  <div class="page">
    <TopBar />
    <div class="container">
      <a class="muted" @click="$router.push('/scenarios')">← 返回剧本列表</a>
      <h2 class="section-title" style="margin-top:8px">编辑状态机 · 剧本 #{{ sid }}</h2>

      <el-tabs v-model="tab">
        <!-- 节点 -->
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

        <!-- NPC -->
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

        <!-- 分支 -->
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
            <el-table-column label="操作" width="90">
              <template #default="{ row }">
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

    <!-- 分支对话框 -->
    <el-dialog v-model="trDlg" title="新增分支" width="500px">
      <el-form :model="trForm" label-width="90px">
        <el-form-item label="源节点">
          <el-select v-model="trForm.fromNodeId" filterable><el-option v-for="n in nodes" :key="n.id" :label="`${n.id} ${n.title}`" :value="n.id" /></el-select>
        </el-form-item>
        <el-form-item label="目标节点">
          <el-select v-model="trForm.toNodeId" filterable><el-option v-for="n in nodes" :key="n.id" :label="`${n.id} ${n.title}`" :value="n.id" /></el-select>
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
      <template #footer><el-button @click="trDlg=false">取消</el-button><el-button type="primary" @click="saveTr">保存</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import TopBar from '../components/TopBar.vue'
import {
  apiNodes, apiCreateNode, apiUpdateNode, apiDeleteNode,
  apiNpcs, apiCreateNpc, apiUpdateNpc, apiDeleteNpc,
  apiTransitions, apiCreateTransition, apiDeleteTransition
} from '../api'

const route = useRoute()
const sid = route.params.id
const tab = ref('nodes')
const nodes = ref([]); const npcs = ref([]); const transitions = ref([])

const load = async () => {
  nodes.value = await apiNodes(sid) || []
  npcs.value = await apiNpcs(sid) || []
  transitions.value = await apiTransitions(sid) || []
}
onMounted(load)

// 节点
const nodeDlg = ref(false)
const nodeForm = ref({})
const openNode = (row) => { nodeForm.value = row ? { ...row } : { isEnding: false }; nodeDlg.value = true }
const saveNode = async () => {
  const f = nodeForm.value
  if (f.id) await apiUpdateNode(f.id, f)
  else await apiCreateNode(sid, f)
  ElMessage.success('已保存'); nodeDlg.value = false; load()
}
const delNode = async (row) => { await ElMessageBox.confirm('删除该节点？将级联删相关分支', '提示', { type: 'warning' }); await apiDeleteNode(row.id); load() }

// NPC
const npcDlg = ref(false)
const npcForm = ref({})
const openNpc = (row) => { npcForm.value = row ? { ...row } : {}; npcDlg.value = true }
const saveNpc = async () => {
  const f = npcForm.value
  if (f.id) await apiUpdateNpc(f.id, f)
  else await apiCreateNpc(sid, f)
  ElMessage.success('已保存'); npcDlg.value = false; load()
}
const delNpc = async (row) => { await ElMessageBox.confirm('删除该 NPC？', '提示', { type: 'warning' }); await apiDeleteNpc(row.id); load() }

// 分支
const trDlg = ref(false)
const trForm = ref({})
const openTr = () => { trForm.value = { scenarioId: Number(sid), conditionExpr: 'always', priority: 10 }; trDlg.value = true }
const saveTr = async () => {
  await apiCreateTransition({ ...trForm.value, scenarioId: Number(sid) })
  ElMessage.success('已保存'); trDlg.value = false; load()
}
const delTr = async (row) => { await ElMessageBox.confirm('删除该分支？', '提示', { type: 'warning' }); await apiDeleteTransition(row.id); load() }
</script>

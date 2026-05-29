<template>
  <div class="statebar">
    <div class="sb-block">
      <div class="sb-title">当前节点</div>
      <div class="sb-node">#{{ state?.currentNodeId ?? '-' }}</div>
    </div>

    <div class="sb-block">
      <div class="sb-title">属性</div>
      <div v-for="(v, k) in state?.attributes || {}" :key="k" class="attr-row">
        <span class="attr-key">{{ attrName(k) }}</span>
        <el-progress
          v-if="k === 'sanity'"
          :percentage="Math.max(0, Math.min(100, v))"
          :stroke-width="10"
          :show-text="false"
          color="#f0b25c"
          style="flex:1"
        />
        <span class="attr-val">{{ v }}</span>
      </div>
      <div v-if="!hasAttrs" class="sb-empty">暂无</div>
    </div>

    <div class="sb-block">
      <div class="sb-title">旗标</div>
      <div class="chips">
        <span v-for="(v, k) in activeFlags" :key="k" class="chip flag">{{ k }}</span>
        <span v-if="Object.keys(activeFlags).length === 0" class="sb-empty">暂无</span>
      </div>
    </div>

    <div class="sb-block">
      <div class="sb-title">物品</div>
      <div class="chips">
        <span v-for="it in state?.inventory || []" :key="it" class="chip item">{{ it }}</span>
        <span v-if="(state?.inventory || []).length === 0" class="sb-empty">空</span>
      </div>
    </div>

    <div class="sb-block" v-if="state?.recentSummary">
      <div class="sb-title">近况</div>
      <div class="summary">{{ state.recentSummary }}</div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
const props = defineProps({ state: { type: Object, default: () => ({}) } })
const hasAttrs = computed(() => Object.keys(props.state?.attributes || {}).length > 0)
const activeFlags = computed(() => {
  const f = props.state?.flags || {}
  return Object.fromEntries(Object.entries(f).filter(([, v]) => v === true))
})
const attrName = (k) => ({ sanity: '理智', evidence: '证据' }[k] || k)
</script>

<style scoped>
.statebar { display: flex; flex-direction: column; gap: 18px; }
.sb-block { background: var(--imm-surface-2); border: 1px solid var(--imm-border); border-radius: 12px; padding: 14px; }
.sb-title { color: var(--imm-text-2); font-size: 12px; letter-spacing: 1px; margin-bottom: 8px; }
.sb-node { color: var(--imm-accent); font-size: 20px; font-weight: 700; }
.attr-row { display: flex; align-items: center; gap: 10px; margin-bottom: 8px; }
.attr-key { color: var(--imm-text); width: 40px; font-size: 13px; }
.attr-val { color: var(--imm-accent); font-weight: 600; width: 30px; text-align: right; }
.chips { display: flex; flex-wrap: wrap; gap: 6px; }
.chip { padding: 3px 10px; border-radius: 999px; font-size: 12px; }
.chip.flag { background: rgba(91,110,225,.25); color: #aab6ff; }
.chip.item { background: rgba(240,178,92,.2); color: var(--imm-accent); }
.sb-empty { color: var(--imm-text-2); font-size: 13px; }
.summary { color: var(--imm-text-2); font-size: 13px; line-height: 1.6; max-height: 160px; overflow-y: auto; }
</style>

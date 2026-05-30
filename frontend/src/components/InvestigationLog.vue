<template>
  <div class="ilog">
    <div class="ilog-head">
      <span class="ilog-title">🔎 调查手记</span>
      <span class="ilog-prog">{{ got.length }}/{{ evidence.length }}</span>
    </div>
    <el-progress
      :percentage="evidence.length ? Math.round((got.length / evidence.length) * 100) : 0"
      :stroke-width="8"
      :show-text="false"
      color="#f0b25c"
    />

    <div class="ilog-list">
      <div v-for="e in evidence" :key="e.flagKey" class="ev" :class="{ got: isOn(e.flagKey) }">
        <span class="tick">{{ isOn(e.flagKey) ? '✓' : '○' }}</span>
        <span class="ev-name">{{ isOn(e.flagKey) ? clean(e.flagName) : '？未获取' }}</span>
      </div>
      <div v-if="!evidence.length" class="ilog-empty">本剧本暂无证据线索</div>
    </div>

    <div v-if="hint" class="ilog-hint">
      <span class="hint-label">下一步</span>{{ hint }}
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
const props = defineProps({
  flagDefs: { type: Array, default: () => [] },
  flags: { type: Object, default: () => ({}) },
  hint: { type: String, default: '' },
})
// 证据/证词类旗标：flag_name 以【证据】或【证词】开头（约定）
const evidence = computed(() =>
  props.flagDefs.filter((f) => /^【证据】|^【证词】/.test(f.flagName || '')),
)
const got = computed(() => evidence.value.filter((e) => isOn(e.flagKey)))
const isOn = (k) => props.flags?.[k] === true
const clean = (name) => (name || '').replace(/^【[^】]+】/, '')
</script>

<style scoped>
.ilog { background: var(--imm-surface-2); border: 1px solid var(--imm-border); border-radius: 12px; padding: 14px; }
.ilog-head { display: flex; justify-content: space-between; align-items: baseline; margin-bottom: 10px; }
.ilog-title { color: var(--imm-accent); font-family: var(--font-serif); font-size: 15px; }
.ilog-prog { color: var(--imm-text-2); font-size: 13px; }
.ilog-list { margin-top: 12px; display: flex; flex-direction: column; gap: 7px; }
.ev { display: flex; align-items: flex-start; gap: 8px; font-size: 13px; color: var(--imm-text-2); }
.ev.got { color: var(--imm-text); }
.tick { color: var(--imm-text-2); width: 14px; flex-shrink: 0; }
.ev.got .tick { color: #6ad08a; }
.ev-name { line-height: 1.45; }
.ilog-empty { color: var(--imm-text-2); font-size: 13px; }
.ilog-hint { margin-top: 12px; padding-top: 10px; border-top: 1px dashed var(--imm-border); font-size: 13px; line-height: 1.6; color: var(--imm-narrative); }
.hint-label { display: inline-block; background: rgba(240,178,92,.18); color: var(--imm-accent); border-radius: 6px; padding: 1px 7px; margin-right: 6px; font-size: 12px; }
</style>

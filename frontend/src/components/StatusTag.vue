<template>
  <el-tag :type="info.type" effect="light" size="small">{{ info.text }}</el-tag>
</template>

<script setup>
import { computed } from 'vue'
const props = defineProps({ status: { type: Number, default: 1 } })
const MAP = {
  1: { text: '进行中', type: 'primary' },
  2: { text: '已通关', type: 'success' },
  3: { text: '已失败', type: 'danger' },
  4: { text: '已弃局', type: 'info' }
}
const info = computed(() => MAP[props.status] || { text: '未知', type: 'info' })
</script>

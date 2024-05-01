<script setup>
import { computed } from 'vue'

const props = defineProps({
  isLocked: Boolean,
  totalProgress: {
    type: Number,
    default: 0
  },
  totalProgressBeforeToday: {
    type: Number,
    default: 0
  },
  beforeTodayBarColor: {
    type: String,
    default: 'bg-teal-600'
  },
  totalProgressBarColor: {
    type: String,
    default: 'bg-teal-300'
  },
  barSize: {
    type: Number,
    default: 22
  }
})

const overallProgressColor = computed(() => {
  if (props.totalProgress === 100) {
    return 'bg-green-400'
  }
  return props.totalProgressBeforeToday > 0 ? props.totalProgressBarColor : props.beforeTodayBarColor
})
const computedTotalProgressBeforeToday = computed(() => props.totalProgress < 100 ? props.totalProgressBeforeToday : 0)

const styleObject = computed(() => {
  return {
    height: `${props.barSize}px`
  }
})
</script>

<template>
  <div class="user-skill-progress-layers" :style="`height: ${props.barSize+2}px`">
    <ProgressBar :value="totalProgress"
                 :pt="{ value: { class: overallProgressColor }}"
                 class="today-progress"
                 :show-value="false"
                 :style="styleObject"></ProgressBar>
    <ProgressBar :value="computedTotalProgressBeforeToday"
                 :pt="{ value: { class: beforeTodayBarColor },
               root: { class: 'opacity-100 remove-background' }}"
                 class="total-progress"
                 :show-value="false"
                 :style="styleObject"></ProgressBar>
  </div>
</template>

<style>
.remove-background {
  background: transparent !important;
}
</style>
<style scoped>
.total-progress {
  position: absolute;
  width: 100%;
  height: 100%;
  top: 0px;
  left: 0px;
}

.today-progress {
  position: absolute;
  width: 100%;
  height: 100%;
  top: 0px;
  left: 0px;
}

.user-skill-progress-layers {
  position: relative;
}
</style>
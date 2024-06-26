/*
Copyright 2024 SkillTree

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    https://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
<script setup>
import { computed } from 'vue'

const props = defineProps({
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
  },
  ariaLabel: {
    type: String,
    default: 'Progress bar'
  },
  isLocked: {
    type: Boolean,
    default: false
  }
})

const isCompleted = computed(() => props.totalProgress >= 100)
const overallProgressColor = computed(() => {
  if (isCompleted.value) {
    return 'bg-green-400'
  }
  return props.totalProgressBeforeToday > 0 ? props.totalProgressBarColor : props.beforeTodayBarColor
})
const computedTotalProgressBeforeToday = computed(() => !isCompleted.value ? props.totalProgressBeforeToday : 0)

const styleObject = computed(() => {
  return {
    height: `${props.barSize}px`
  }
})
const ariaLabelFullMsg = computed(() => {
  let res = props.ariaLabel
  if (props.isLocked) {
    res += ' This skill is locked.'
  }
  return res
})
</script>

<template>
  <div class="user-skill-progress-layers" :style="`height: ${props.barSize+2}px`">
<!--    <pre> totalProgress:{{ totalProgress }}</pre>-->
<!--    <pre> totalProgressBeforeToday: {{ totalProgressBeforeToday }}</pre>-->
<!--    <pre> computedTotalProgressBeforeToday: {{ computedTotalProgressBeforeToday }}</pre>-->
    <ProgressBar :value="totalProgress"
                 :pt="{ value: { class: overallProgressColor }}"
                 class="today-progress"
                 :class="{ 'is-completed': isCompleted, 'is-not-completed': !isCompleted }"
                 :show-value="false"
                 :ariaLabel="ariaLabelFullMsg"
                 :style="styleObject"></ProgressBar>
    <ProgressBar :value="computedTotalProgressBeforeToday"
                 :pt="{ value: { class: beforeTodayBarColor },
               root: { class: 'opacity-100 remove-background' }}"
                 class="total-progress"
                 :class="{ 'is-completed': isCompleted, 'is-not-completed': !isCompleted }"
                 :show-value="false"
                 :ariaLabel="ariaLabelFullMsg"
                 :style="styleObject"></ProgressBar>
    <div v-if="isLocked" class="absolute left-0 right-0" data-cy="progressBarWithLock">
      <div class="flex justify-content-center">
        <div class="text-center" style="z-index: 1000 !important;">
          <i class="fas fa-lock" aria-hidden="true"/>
        </div>
      </div>
    </div>
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
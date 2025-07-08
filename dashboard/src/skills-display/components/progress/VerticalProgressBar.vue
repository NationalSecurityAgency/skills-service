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
import {computed} from 'vue'
import {useSkillsDisplayThemeState} from "@/skills-display/stores/UseSkillsDisplayThemeState.js";
import {useSkillsDisplayAttributesState} from "@/skills-display/stores/UseSkillsDisplayAttributesState.js";

const props = defineProps({
  totalProgress: {
    type: Number,
    default: 0
  },
  totalProgressBeforeToday: {
    type: Number,
    default: 0
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
  },
  disableDailyColor: {
    type: Boolean,
    default: false
  }
})

const themeState = useSkillsDisplayThemeState()
const isCompleted = computed(() => props.totalProgress >= 100)
const computedTotalProgressBeforeToday = computed(() => !isCompleted.value ? props.totalProgressBeforeToday : 0)
const attributes = useSkillsDisplayAttributesState()

const styleObject = computed(() => {
  return {
    height: `${props.barSize}px`
  }
})
const ariaLabelFullMsg = computed(() => {
  let res = props.ariaLabel
  if (props.isLocked) {
    res += ` This ${attributes.skillDisplayNameLower} is locked.`
  }
  return res
})

const rootPtObjectWithOptionalIncompleteColor = computed(() => {
  const incompleteColor = themeState.theme.progressIndicators?.incompleteColor
  return incompleteColor ? { root: { style: `background-color: ${incompleteColor}`} } : {}
})

const firstProgressBarPt = computed(() => {
      const rootObj = rootPtObjectWithOptionalIncompleteColor.value

      const bgColor = themeState.theme.progressIndicators?.earnedTodayColor
      if (bgColor) {
        return {...rootObj, value: {style: `background-color: ${bgColor}`}}
      }
      return {...rootObj, value: {class: 'bg-teal-300!'}}
    }
)
const secondProgressBarPt = computed(() => {
      const rootPt = { root: { class: 'opacity-100! bg-transparent!' } }
      const bgColor = themeState.theme.progressIndicators?.beforeTodayColor
      if (bgColor) {
        return { ...rootPt, value: {style: `background-color: ${bgColor}`} }
      }
      return { ...rootPt, value: {class: 'bg-teal-600!'} }
    }
)
const thirdProgressBarPt = computed(() => {
      const rootPt = props.disableDailyColor ? rootPtObjectWithOptionalIncompleteColor.value : {}
      const bgColor = themeState.theme.progressIndicators?.completeColor
      if (bgColor) {
        return { ...rootPt, value: {style: `background-color: ${bgColor}`}}
      }
      return {...rootPt, value: {class: 'bg-green-700!'}}
    }
)
</script>

<template>
  <div class="user-skill-progress-layers" :style="`height: ${props.barSize+2}px`">
    <ProgressBar v-if="!isCompleted && !disableDailyColor" :value="totalProgress"
                 :pt="firstProgressBarPt"
                 class="progress-bar sd-theme-today-progress is-not-completed"
                 :class="{ 'is-completed': isCompleted, 'is-not-completed': !isCompleted }"
                 :show-value="false"
                 :ariaLabel="ariaLabelFullMsg"
                 :style="styleObject"></ProgressBar>
    <ProgressBar v-if="!isCompleted && !disableDailyColor" :value="computedTotalProgressBeforeToday"
                 :pt="secondProgressBarPt"
                 class="progress-bar sd-theme-total-progress  is-not-completed"
                 :class="{ 'is-completed': isCompleted, 'is-not-completed': !isCompleted }"
                 :show-value="false"
                 :ariaLabel="ariaLabelFullMsg"
                 :style="styleObject"></ProgressBar>
    <ProgressBar v-if="isCompleted || disableDailyColor" :value="totalProgress"
                 :pt="thirdProgressBarPt"
                 class="is-completed progress-bar"
                 :show-value="false"
                 :ariaLabel="ariaLabelFullMsg"
                 :style="styleObject"></ProgressBar>
    <div v-if="isLocked" class="absolute left-0 right-0" data-cy="progressBarWithLock">
      <div class="flex justify-center">
        <div class="text-center" style="z-index: 1000 !important;">
          <i class="fas fa-lock" :class="{ 'text-orange-200': isCompleted }" aria-hidden="true"/>
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
.progress-bar {
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
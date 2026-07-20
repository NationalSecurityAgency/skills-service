/*
Copyright 2026 SkillTree

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
import { useNumberFormat } from '@/common-components/filter/UseNumberFormat.js'

const nF = useNumberFormat()
defineProps({
  completedNum: Number,
  totalNum: Number,
  label: String,
  iconClass: {
    type: String,
    default: 'fa-solid fa-graduation-cap'
  }
})
</script>

<template>
  <div class="flex items-center gap-2 px-3 py-1 bg-surface-50 dark:bg-surface-800 sd-theme-tile-background-color rounded-lg border border-surface-200 dark:border-surface-700">
    <i class="text-surface-600 dark:text-surface-400" :class="iconClass"
       aria-hidden="true"></i>
    <div class="flex gap-1 items-center">
      <div data-cy="completedNum" class="font-bold text-green-800">{{ nF.pretty(completedNum) }}</div>
      <div class="text-surface-500">/</div>
      <div data-cy="totalNum" class="font-semibold text-surface-600 dark:text-surface-400">{{
          nF.pretty(totalNum)
        }}
      </div>
    </div>
    <div class="text-surface-600 dark:text-surface-400 items-center uppercase">{{
        label
      }}
    </div>
  </div>
</template>

<style scoped>

</style>
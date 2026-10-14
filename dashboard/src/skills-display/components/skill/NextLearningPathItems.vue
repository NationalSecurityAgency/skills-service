/*
 * Copyright 2026 SkillTree
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
<script setup>
import { useDependencyNavigation } from '@/common-components/dependency/UseDependencyNavigation.js'
import { useSkillsDisplayInfo } from '@/skills-display/UseSkillsDisplayInfo.js'

defineProps({
  nextItems: { type: Array, default: () => [] },
})
const navigation = useDependencyNavigation()
const skillsDisplayInfo = useSkillsDisplayInfo()
</script>

<template>
  <Card v-if="nextItems.length" class="mt-4" :pt="{ body: { class: 'p-4!' } }" data-cy="nextLearningPathItems">
    <template #content>
      <div class="flex items-center justify-between gap-3">
        <h2 class="text-xl font-semibold sd-theme-primary-color">Next in <router-link
          :to="{ name: skillsDisplayInfo.getContextSpecificRouteName('projectLearningPathPage') }"
          class="sd-theme-primary-color underline focus-visible:outline-2 focus-visible:outline-offset-2"
          data-cy="nextLearningPathLink">Learning Path</router-link></h2>
        <span class="text-xs font-medium uppercase sd-theme-primary-color shrink-0" aria-hidden="true">Up next</span>
      </div>
      <ul class="mt-3 mb-0 flex flex-col gap-2 list-none p-0">
        <li v-for="item in nextItems" :key="item.id">
          <router-link
            :to="navigation.getRoute(item, true)"
            class="group flex w-full items-center gap-3 rounded-lg border border-surface-300 dark:border-surface-600 bg-surface-50 dark:bg-surface-900 p-3 text-surface-900! dark:text-surface-100! no-underline! hover:bg-surface-100 dark:hover:bg-surface-800 hover:border-green-700 dark:hover:border-green-300 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-green-700 dark:focus-visible:outline-green-300 motion-safe:transition-colors"
            :aria-label="`Continue to ${item.type === 'Badge' ? 'badge' : 'skill'}: ${item.name}`"
            :data-cy="`nextLearningPathItem_${item.skillId}`">
            <span class="flex size-12 shrink-0 items-center justify-center rounded-lg border border-green-200 dark:border-green-800 bg-green-50 dark:bg-green-950 text-green-700 dark:text-green-300 text-xl" aria-hidden="true">
              <i class="fas" :class="item.type === 'Badge' ? 'fa-award' : 'fa-graduation-cap'" />
            </span>
            <span class="min-w-0 flex-1">
              <span class="block font-semibold break-words group-hover:underline">{{ item.name }}</span>
              <span class="block mt-1 text-sm text-surface-600 dark:text-surface-300">Continue to the next {{ item.type === 'Badge' ? 'badge' : 'skill' }}</span>
            </span>
            <span class="flex size-9 shrink-0 items-center justify-center rounded-full border border-surface-300 dark:border-surface-600 bg-surface-0 dark:bg-surface-800 text-green-700 dark:text-green-300" aria-hidden="true">
              <i class="fas fa-arrow-right" />
            </span>
          </router-link>
        </li>
      </ul>
    </template>
  </Card>
</template>

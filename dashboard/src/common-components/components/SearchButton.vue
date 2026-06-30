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
import {computed, ref} from 'vue'
import { watchDebounced, useMagicKeys } from '@vueuse/core'
import {useUserPreferences} from '@/stores/UseUserPreferences.js'
import {useLog} from '@/components/utils/misc/useLog.js'
import SkillsDisplaySearch from "@/skills-display/components/SkillsDisplaySearch.vue";
import {useRoute} from "vue-router";

const props = defineProps({
  navToSkillFn: {
    type: Function,
    required: true,
  },
  loadProjPagesInfoFn: {
    type: Function,
    required: true,
  },
  showLabel: {
    type: Boolean,
    default: true
  },
  keyboardShortcutEnabled: {
    type: Boolean,
    default: true
  }
});
const emits = defineEmits(['click'])

const log = useLog()
const userPreferences = useUserPreferences()
const route = useRoute()

const projectId = route.params.projectId
const showSkillsDisplaySearchDialog = ref(false)
const searchTrainingButtonShortcut = ref('ctrl+k')

if (props.keyboardShortcutEnabled) {
  const keys = useMagicKeys({
    passive: false,
    onEventFired(e) {
      if (searchTrainingButtonShortcut.value?.toLowerCase() === 'ctrl+k' && e.ctrlKey && e.key === 'k' && e.type === 'keydown') {
        e.preventDefault()
      }
    },
  })

  userPreferences.afterUserPreferencesLoaded().then((options) => {
    const debounceOptions = {debounce: 250, maxWait: 1000}
    if (options.sd_search_training_keyboard_shortcut) {
      searchTrainingButtonShortcut.value = options.sd_search_training_keyboard_shortcut?.toLowerCase().replace(/ /g, '')
    }

    log.debug(`Search training shortcut is : ${searchTrainingButtonShortcut.value}`)
    watchDebounced(
        keys[searchTrainingButtonShortcut.value],
        () => {
          showSkillsDisplaySearchDialog.value = true
        },
        debounceOptions
    )
  })
}

const toTitleCase = (str) => {
  return str.toLowerCase().split('+').map(word => word.charAt(0).toUpperCase() + word.slice(1)).join(' ');
}
const searchTrainingButtonShortcutForDisplay = computed(() => {
  return toTitleCase(searchTrainingButtonShortcut.value)
})

const onClick = () => {
  showSkillsDisplaySearchDialog.value = true
}
</script>

<template>
  <SkillsButton
      id="skillsDisplaySearchBtn"
      :track-for-focus="true"
      @click="onClick"
      data-cy="skillsDisplaySearchBtn"
      aria-label="Search training shortcut"
      :title="`Search Project (${searchTrainingButtonShortcutForDisplay})`">
    <div class="flex gap-1 items-center">
      <i class="fa-solid fa-magnifying-glass"></i>
      <div v-if="showLabel">Search</div>
      <div v-if="keyboardShortcutEnabled" class="ml-1 inline-flex items-center rounded border border-gray-200 bg-gray-50 dark:bg-gray-800 px-1.5 font-sans text-sm font-medium text-gray-500 dark:text-gray-200">
        {{  searchTrainingButtonShortcutForDisplay }}
      </div>
    </div>

    <skills-display-search v-if="showSkillsDisplaySearchDialog"
                           ref="skillsDisplaySearch"
                           v-model="showSkillsDisplaySearchDialog"
                           :navToSkillFn="navToSkillFn"
                           :loadProjPagesInfoFn="loadProjPagesInfoFn"
                           :project-id="projectId" />

  </SkillsButton>
</template>

<style scoped>

</style>
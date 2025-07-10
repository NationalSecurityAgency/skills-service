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
import { onMounted, ref } from 'vue'
import AutoComplete from 'primevue/autocomplete'
import { useSkillsDisplayService } from '@/skills-display/services/UseSkillsDisplayService.js'
import { useSkillsAnnouncer } from '@/common-components/utilities/UseSkillsAnnouncer.js'
import HighlightedValue from '@/components/utils/table/HighlightedValue.vue'
import { useSkillsDisplayInfo } from '@/skills-display/UseSkillsDisplayInfo.js'
import {useSkillsDisplayAttributesState} from "@/skills-display/stores/UseSkillsDisplayAttributesState.js";

const skillsDisplayService = useSkillsDisplayService()
const announcer = useSkillsAnnouncer()
const skillDisplayInfo = useSkillsDisplayInfo()
const attributes = useSkillsDisplayAttributesState()

const selected = ref('')
const query = ref('')
const searchRes = ref([])
const isSearching = ref(false)
const search = (event) => {
  query.value = event?.query || ''
  isSearching.value = true
  return skillsDisplayService.searchSkills(query.value)
    .then((res) => {
      let results = res.data
      if (results && query.value && query.value.trim().length > 0) {
        results = results.map((item) => {
          const skillNameHtml = null //StringHighlighter.highlight(item.skillName, query.value);
          return ({ ...item, skillNameHtml })
        })
      }
      searchRes.value = results
      if (results && results.length > 0) {
        const firstRes = results[0]
        // eslint-disable-next-line max-len
        announcer.polite(`Showing ${results.length} ${attributes.skillDisplayNamePlural} for ${query.value ? query.value : 'an empty'} search string. Selected ${firstRes.skillName} ${attributes.skillDisplayName} from ${firstRes.subjectName} ${attributes.subjectDisplayName}. You have earned ${firstRes.userCurrentPoints} ${attributes.pointDisplayNamePlural} out of ${firstRes.totalPoints} for this ${attributes.skillDisplayName}. Click to navigate to the ${attributes.skillDisplayName}.`)
      } else {
        announcer.assertive(`No ${attributes.skillDisplayNamePlural} found for ${query.value} search string. Consider changing the search query.`)
      }

    }).finally(() => {
      isSearching.value = false
    })
}

const navToSkill = (event) => {
  const skill = event.value
  skillDisplayInfo.routerPush(
    'skillDetails',
    {
      subjectId: skill.subjectId,
      skillId: skill.skillId
    })
}

</script>

<template>
  <div class="card p-fluid">
    <AutoComplete
      v-model="selected"
      :loading="isSearching"
      :suggestions="searchRes"
      :completeOnFocus="true"
      :delay="500"
      @complete="search"
      @item-select="navToSkill"
      fluid
      data-cy="searchSkillsAcrossSubjects"
      :pt="{ dropdown: { 'aria-label': 'click to select a skill' } }"
      :placeholder="`Search for a ${attributes.skillDisplayName} across ${attributes.subjectDisplayNamePlural}...`"
      optionLabel="skillName"
    >
      <template #option="slotProps">
        <div class="py-1 skill-res-row w-full sd-theme-primary-color" :data-cy="`searchRes-${slotProps.option.skillId}`">
          <div class="flex gap-2">
            <div
              class="flex-1 flex items-center"
              data-cy="skillName"
              :aria-label="`Selected ${slotProps.option.skillName} ${attributes.skillDisplayNameLower} from ${slotProps.option.subjectName} ${attributes.subjectDisplayName}. You have earned ${slotProps.option.userCurrentPoints} ${attributes.pointDisplayNamePlural} out of ${slotProps.option.totalPoints} for this ${attributes.skillDisplayNameLower}. Click to navigate to the ${attributes.skillDisplayNameLower}. Type to search for a ${attributes.skillDisplayNameLower} across all ${attributes.subjectDisplayNamePlural}.`">
              <i class="fas fa-graduation-cap mr-1 text-xl text-green-800"
                 aria-hidden="true" />
              <highlighted-value :value="slotProps.option.skillName" :filter="query" class="text-xl" />
            </div>
            <div
              class=""
              data-cy="points"
              :class="{'font-green-300': slotProps.option.userAchieved}" aria-hidden="true">
              <i option v-if="slotProps.option.userAchieved" class="fas fa-check mr-1" aria-hidden="" />
              <span class="text-orange-600 font-medium">{{ slotProps.option.userCurrentPoints }}</span> / {{ slotProps.option.totalPoints }} <span class="italic">Points</span>
            </div>
          </div>

          <div data-cy="subjectName" aria-hidden="true" class="mt-1">
            <span class="italic ">Subject:</span> <span
            class="text-info skills-theme-primary-color alt-color-handle-hover">{{ slotProps.option.subjectName
            }}</span>
          </div>
        </div>
      </template>
      <template #empty>
        <div class="p-4">No Results Found</div>
      </template>
    </AutoComplete>
  </div>
</template>

<style scoped>

</style>
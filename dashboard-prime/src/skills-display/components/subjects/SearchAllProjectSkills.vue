<script setup>
import { onMounted, ref } from 'vue'
import AutoComplete from 'primevue/autocomplete'
import { useSkillsDisplayService } from '@/skills-display/services/UseSkillsDisplayService.js'
import { useSkillsAnnouncer } from '@/common-components/utilities/UseSkillsAnnouncer.js'
import HighlightedValue from '@/components/utils/table/HighlightedValue.vue'
import { useSkillsDisplayInfo } from '@/skills-display/UseSkillsDisplayInfo.js'

const skillsDisplayService = useSkillsDisplayService()
const announcer = useSkillsAnnouncer()
const skillDisplayInfo = useSkillsDisplayInfo()

const selected = ({})
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
        announcer.polite(`Showing ${results.length} skills for ${query.value ? query.value : 'an empty'} search string. Selected ${firstRes.skillName} skill from ${firstRes.subjectName} subject. You have earned ${firstRes.userCurrentPoints} points out of ${firstRes.totalPoints} for this skill. Click to navigate to the skill.`)
      } else {
        announcer.assertive(`No skills found for ${query.value} search string. Consider changing the search query.`)
      }

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
      data-cy="searchSkillsAcrossSubjects"
      placeholder="Search for a skill across subjects..."
    >
      <template #option="slotProps">
        <div class="py-1 skill-res-row" :data-cy="`searchRes-${slotProps.option.skillId}`">
          <div class="flex">
            <div
              class="flex-1 flex align-items-center"
              data-cy="skillName"
              :aria-label="`Selected ${slotProps.option.skillName} skill from ${slotProps.option.subjectName} subject. You have earned ${slotProps.option.userCurrentPoints} points out of ${slotProps.option.totalPoints} for this skill. Click to navigate to the skill. Type to search for a skill across all subjects.`">
              <i class="fas fa-graduation-cap skills-theme-primary-color mr-1 text-xl text-green-800"
                 aria-hidden="true" />
              <highlighted-value :value="slotProps.option.skillName" :filter="query" class="text-xl" />
            </div>
            <div
              class=" skills-theme-primary-color"
              data-cy="points"
              :class="{'font-green-300': slotProps.option.userAchieved}" aria-hidden="true">
              <i option v-if="slotProps.option.userAchieved" class="fas fa-check mr-1" aria-hidden="" />
              <span class="text-orange-600 font-medium">{{ slotProps.option.userCurrentPoints }}</span> / {{ slotProps.option.totalPoints }} <span class="font-italic">Points</span>
            </div>
          </div>

          <div data-cy="subjectName" aria-hidden="true" class="mt-1">
            <span class="font-italic ">Subject:</span> <span
            class="text-info skills-theme-primary-color alt-color-handle-hover">{{ slotProps.option.subjectName
            }}</span>
          </div>
        </div>
      </template>
      <template #empty>
        <div class="p-3">No Results Found</div>
      </template>
    </AutoComplete>
  </div>
</template>

<style scoped>

</style>
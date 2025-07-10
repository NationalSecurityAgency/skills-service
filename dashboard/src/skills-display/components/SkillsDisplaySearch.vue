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
import { nextTick, onMounted, ref } from 'vue'
import SkillsDialog from '@/components/utils/inputForm/SkillsDialog.vue'
import { useSkillsDisplayService } from '@/skills-display/services/UseSkillsDisplayService.js'
import { useSkillsAnnouncer } from '@/common-components/utilities/UseSkillsAnnouncer.js'
import { useSkillsDisplayInfo } from '@/skills-display/UseSkillsDisplayInfo.js'
import HighlightedValue from '@/components/utils/table/HighlightedValue.vue'
import { useFocusState } from '@/stores/UseFocusState.js'
import SkillsSpinner from '@/components/utils/SkillsSpinner.vue'
import { useSkillsDisplayAttributesState } from '@/skills-display/stores/UseSkillsDisplayAttributesState.js'

const emit = defineEmits(['level-changed', 'hidden']);
const props = defineProps({
  projectId: {
    type: String,
    required: true,
  },
});

const model = defineModel()

const skillsDisplayService = useSkillsDisplayService()
const skillDisplayInfo = useSkillsDisplayInfo()
const announcer = useSkillsAnnouncer()
const focusState = useFocusState()
const attributes = useSkillsDisplayAttributesState()

const selected = ref('')
const query = ref('')
const searchRes = ref([])
const isSearching = ref(false)
const trainingSearchListBoxRef = ref(null);

onMounted(() => {
  loadSkillsSubjectsAndBadges()
  focusOnFilterInput()
});

const loadSkillsSubjectsAndBadges = (event) => {
  query.value = event?.query || ''
  isSearching.value = true
  return skillsDisplayService.getAllProjectSkillsSubjectsAndBadges()
      .then((res) => {
        let results = res.data
        searchRes.value = results
        if (results && results.length > 0) {
          announcer.polite(`Showing ${results.length} items.  Type to search for a ${attributes.subjectDisplayNamePlural}, ${attributes.skillDisplayNamePlural} or Badges. Use arrow keys to select and enter or click to navigate to the ${attributes.subjectDisplayName}, ${attributes.skillDisplayName} or Badge.`)
        } else {
          announcer.assertive(`No ${attributes.subjectDisplayNamePlural}, ${attributes.skillDisplayNamePlural} or badges found.`)
        }

      }).finally(() => {
        isSearching.value = false
      })
}
const focusOnFilterInput = () => {
  if (document.activeElement?.id) {
    focusState.setElementId(document.activeElement?.id)
  }
  nextTick(() => {
    const filterInput = trainingSearchListBoxRef.value?.$el?.querySelector('.p-listbox-filter');
    if (filterInput) {
      filterInput.focus();
    }
  });
}
const navToSkill = (skill) => {
  const { skillType } = skill
  if (skillType === 'Subject') {
    skillDisplayInfo.routerPush(
        'SubjectDetailsPage',
        {
          subjectId: skill.skillId
        })
  } else if (skillType === 'Badge') {
    skillDisplayInfo.routerPush(
        'badgeDetails',
        {
          badgeId: skill.skillId
        })
  } else {
    skillDisplayInfo.routerPush(
        'skillDetails',
        {
          subjectId: skill.subjectId,
          skillId: skill.skillId
        })
  }
  closeMe()
}

const closeMe = () => {
  model.value = false
  publishHidden();
};

const publishHidden = () => {
  emit('hidden', { projectId: props.projectId });
};

const filterEvent = (event) => {
  query.value = event.value
}

const isSkill = (skill) => {
  return skill.skillType === 'Skill'
}
const getIconClass = (skill) => {
  const { skillType } = skill
  if (skillType === 'Subject') {
    return 'fa-solid fa-cubes skills-color-subjects text-blue-500'
  }
  if (skillType === 'Badge') {
    return 'fas fa-award skills-color-badges text-indigo-500'
  }
  return 'fas fa-graduation-cap skills-color-skills text-green-500'
}
const getUserProgress = (skill) => {
  return isSkill(skill) ? skill.userCurrentPoints : skill.childAchievementCount
}
const getTotalProgress = (skill) => {
  return isSkill(skill) ? skill.totalPoints : skill.totalChildCount
}
const getProgressLabel = (skill) => {
  return isSkill(skill) ? attributes.pointDisplayNamePlural : attributes.skillDisplayNamePlural
}
</script>

<template>
  <SkillsDialog
      :pt="{ content: { class: 'p-5!' } }"
      v-model="model"
      data-cy="trainingSearchDialog"
      :aria-label="`Search for ${attributes.subjectDisplayName}s, ${attributes.skillDisplayName}s or Badges`"
      :enable-return-focus="true"
      @on-cancel="closeMe"
      :show-header="false"
      :show-ok-button="false"
      :show-cancel-button="false"
      :maximizable="false"
      :style="{ width: '40rem !important' }">

    <div class="card flex justify-center">
      <Listbox class="w-full border-none!"
               data-cy="trainingSearchListBox"
               :aria-label="`Search for ${attributes.subjectDisplayName}s, ${attributes.skillDisplayName}s or Badges`"
               :pt="{ listContainer: { tabindex: '0'}, header: { class: 'p-0! pb-1!' }, list: { class: 'p-0!' } }"
               @filter="filterEvent"
               @update:modelValue="navToSkill"
               ref="trainingSearchListBoxRef"
               v-model="selected"
               :options="searchRes"
               optionLabel="skillName"
               :autofocus="true"
               :auto-option-focus="false"
               filter
               :filterPlaceholder="`Search for ${attributes.subjectDisplayName}s, ${attributes.skillDisplayName}s or Badges`"
      >
        <template #option="slotProps">
          <div class="py-0 w-full sd-theme-primary-color" :data-cy="`searchRes-${slotProps.option.skillId}`">
            <div class="flex flex-nowrap">
              <div class="flex-1 flex items-center inline"
                   data-cy="skillName"
                   :aria-label="`Selected ${slotProps.option.skillName} ${slotProps.option.skillType}${slotProps.option.subjectName ? ` from ${slotProps.option.subjectName} ${attributes.subjectDisplayName}` : ''}. You have earned ${getUserProgress(slotProps.option)} ${getProgressLabel(slotProps.option)} out of ${getTotalProgress(slotProps.option)} for this ${slotProps.option.skillType}. Click to navigate to the ${slotProps.option.skillType}.`">
                <i :class="`${getIconClass(slotProps.option)} mr-1 text-xl`" style="min-width: 25px" aria-hidden="true" />
                <highlighted-value :value="slotProps.option.skillName" :filter="query" class="text-xl" />
              </div>
              <div
                  class=""
                  data-cy="points"
                  :class="{'text-green-700 dark:text-green-500': slotProps.option.userAchieved}" aria-hidden="true">
                <i v-if="slotProps.option.userAchieved" class="fas fa-check mr-1" aria-hidden="" />
                <span class="text-orange-700 font-medium">{{ getUserProgress(slotProps.option) }}</span> / {{ getTotalProgress(slotProps.option) }} <span class="italic">{{ getProgressLabel(slotProps.option) }}</span>
              </div>
            </div>

            <div v-if="isSkill(slotProps.option)" data-cy="subjectName" aria-hidden="true" class="mt-1">
              <span class="italic ">{{ attributes.subjectDisplayName }}:</span> <span
                class="text-info skills-theme-primary-color alt-color-handle-hover">{{ slotProps.option.subjectName
              }}</span>
            </div>
          </div>
        </template>
        <template #empty>
          <div v-if="!isSearching" class="p-4">No results found</div>
          <div v-else>
            <SkillsSpinner :is-loading="isSearching" class="mt-4"/>
          </div>
        </template>
      </Listbox>
    </div>
    <div class="flex flex-row gap-5 mt-4 text-sm">
      <span><i class="fa-solid fa-arrow-up mr-2"></i><i class="fa-solid fa-arrow-down mr-3"></i>to navigate</span>
      <span><span class="font-bold mr-2">enter</span>to select</span>
      <div><span class="font-bold mr-2">esc</span>to close</div>
    </div>
  </SkillsDialog>
</template>

<style scoped>

</style>
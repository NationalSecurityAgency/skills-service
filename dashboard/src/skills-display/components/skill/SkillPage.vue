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
import {computed, onMounted, ref, watch} from 'vue'
import SkillsTitle from '@/skills-display/components/utilities/SkillsTitle.vue'
import {useRoute} from 'vue-router'
import {useSkillsDisplayService} from '@/skills-display/services/UseSkillsDisplayService.js'
import {useSkillsDisplayInfo} from '@/skills-display/UseSkillsDisplayInfo.js'
import SkillProgress from '@/skills-display/components/progress/SkillProgress.vue'
import {useScrollSkillsIntoViewState} from '@/skills-display/stores/UseScrollSkillsIntoViewState.js'
import {useSkillsDisplaySubjectState} from '@/skills-display/stores/UseSkillsDisplaySubjectState.js'
import {useSkillsDisplayAttributesState} from '@/skills-display/stores/UseSkillsDisplayAttributesState.js'
import Prerequisites from '@/skills-display/components/skill/prerequisites/Prerequisites.vue'
import SkillAchievementMsg from "@/skills-display/components/progress/celebration/SkillAchievementMsg.vue";
import MarkdownText from "@/common-components/utilities/markdown/MarkdownText.vue";
import {useMagicKeys, watchDebounced} from "@vueuse/core";
import {useUserPreferences} from "@/stores/UseUserPreferences.js";

const attributes = useSkillsDisplayAttributesState()
const skillsDisplayService = useSkillsDisplayService()
const skillsDisplayInfo = useSkillsDisplayInfo()
const scrollIntoViewState = useScrollSkillsIntoViewState()
const route = useRoute()
const skillState = useSkillsDisplaySubjectState()
const keys = useMagicKeys()
const userPreferences = useUserPreferences()
const skill = computed(() => skillState.skillSummary)
const loadingSkill = ref(true)
const displayGroupDescription = ref(false);
const groupDescription = ref(null);
const loadingDescription = ref(true);

onMounted(() => {
  loadSkillSummary()
})
watch( () => route.params.skillId, () => {
  loadSkillSummary()
});
watch(() => skill.value.groupSkillId, () => {
  groupDescription.value = null;
  displayGroupDescription.value = false;
})
const loadSkillSummary = () => {
  const skillId = skillsDisplayInfo.isDependency() ? route.params.dependentSkillId : route.params.skillId
  skillState.loadSkillSummary(skillId, route.params.crossProjectId, route.params.subjectId)
    .then(() => {
      loadingSkill.value = false
      if (skillId && skill.value.projectId && !skillsDisplayInfo.isCrossProject()) {
        skillsDisplayService.updateSkillHistory(skill.value.projectId, skillId)
      }
      scrollIntoViewState.setLastViewedSkillId(skillId)

      if(!groupDescription.value && skill.value.groupSkillId && attributes.groupDescriptionsOn) {
        descriptionToggled();
      }
    })
}
const nextButtonShortcut = ref('Ctrl+Alt+N')
const previousButtonShortcut = ref('Ctrl+Alt+P')
userPreferences.afterUserPreferencesLoaded().then((options) => {
  const debounceOptions = { debounce: 250, maxWait: 1000 }
  if (options.sd_next_skill_keyboard_shortcut) {
    nextButtonShortcut.value = options.sd_next_skill_keyboard_shortcut
  }
  if (options.sd_previous_skill_keyboard_shortcut) {
    previousButtonShortcut.value = options.sd_previous_skill_keyboard_shortcut
  }
  watchDebounced(
      keys[nextButtonShortcut.value],
      () => {
        nextButtonClicked()
      },
      debounceOptions
  )
  watchDebounced(
      keys[previousButtonShortcut.value],
      () => {
        prevButtonClicked()
      },
      debounceOptions
  )
})



const prevButtonClicked = () => {
  const params = { skillId: skillState.skillSummary.prevSkillId, projectId: route.params.projectId }
  skillsDisplayInfo.routerPush(
    'skillDetails',
    params
  )
}
const nextButtonClicked = () => {
  const params = { skillId: skillState.skillSummary.nextSkillId, projectId: route.params.projectId }
  skillsDisplayInfo.routerPush(
    'skillDetails',
    params
  )
}

const isLoading = computed(() => loadingSkill.value || skillState.loadingSkillSummary)
const descriptionToggled = () => {
  if(!groupDescription.value && skill.value.groupSkillId) {
    loadingDescription.value = true;
    skillsDisplayService.getDescriptionForSkill(skill.value.groupSkillId).then((res) => {
      groupDescription.value = res.description;
      loadingDescription.value = false;
    })
  }
}
</script>

<template>
  <div>
    <div v-if="!isLoading">
      <skills-title>{{ attributes.skillDisplayName }} Overview</skills-title>
      <Card class="mt-4" :pt="{ content: { class: 'p-0' }}">
        <template #content>
          <div class="flex-col sm:flex-row items-center flex gap-2 mb-6" v-if="skill && (skill.prevSkillId || skill.nextSkillId) && !skillsDisplayInfo.isCrossProject()">
            <div class="w-28">
              <SkillsButton
                @click="prevButtonClicked" v-if="skill.prevSkillId"
                outlined
                size="small"
                :title="`Previous Skills (${previousButtonShortcut})`"
                class="skills-theme-btn"
                data-cy="prevSkill"
                aria-label="previous skill">
                <i class="fas fa-arrow-alt-circle-left mr-1" aria-hidden="true"></i> Previous
              </SkillsButton>
            </div>
            <div class="flex-1 text-center " style="font-size: 0.9rem;" data-cy="skillOrder"><span
              class="italic">{{ attributes.skillDisplayName }}</span> <span class="font-semibold">{{ skill.orderInGroup
              }}</span> <span class="italic">of</span> <span class="font-semibold">{{ skill.totalSkills }}</span>
            </div>
            <div class="w-28 text-right">
              <SkillsButton
                @click="nextButtonClicked"
                v-if="skill.nextSkillId"
                class="skills-theme-btn"
                data-cy="nextSkill"
                outlined
                size="small"
                :title="`Next Skills (${nextButtonShortcut})`"
                aria-label="next skill">
                Next
                <i class="fas fa-arrow-alt-circle-right ml-1" aria-hidden="true"></i>
              </SkillsButton>
            </div>
          </div>
          <div v-if="!attributes.groupInfoOnSkillPage && skill.groupName" class="mt-4 p-1 mb-4" data-cy="groupInformationSection">
            <div class="flex">
              <div class="mr-2 mt-1 text-xl">
                <i class="fas fa-layer-group" aria-hidden="true"></i>
              </div>
              <div class="flex flex-1">
                <span class="text-2xl sd-theme-primary-color font-medium flex">{{ skill.groupName }}</span>
              </div>
              <div v-if="!attributes.groupDescriptionsOn">
                <div class="flex flex-row content-center">
                  <label for="groupDescriptionToggleSwitch">
                    <span class="text-muted pr-1 content-center">Group Description:</span>
                  </label>
                  <ToggleSwitch v-model="displayGroupDescription" data-cy="toggleGroupDescription" @change="descriptionToggled" inputId="groupDescriptionToggleSwitch" />
                </div>
              </div>
            </div>
            <div class="mt-2 ml-6" v-if="displayGroupDescription || attributes.groupDescriptionsOn" data-cy="groupDescriptionSection">
              <skills-spinner :is-loading="loadingDescription" :size-in-rem="1"/>
              <markdown-text v-if="groupDescription" :text="groupDescription" />
            </div>
          </div>
          <skill-achievement-msg :skill="skill" />
          <div class="card-body text-center text-sm-left">
            <skill-progress :skill="skill" />
          </div>
        </template>
      </Card>

      <prerequisites />
    </div>
    <skills-spinner v-if="isLoading" :is-loading="isLoading" class="mt-8" />
  </div>
</template>

<style scoped>

</style>
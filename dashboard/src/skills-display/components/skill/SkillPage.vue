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
import {computed, defineAsyncComponent, onMounted, ref, watch} from 'vue'
import SkillsTitle from '@/skills-display/components/utilities/SkillsTitle.vue'
import {useRoute} from 'vue-router'
import {useSkillsDisplayService} from '@/skills-display/services/UseSkillsDisplayService.js'
import {useSkillsDisplayInfo} from '@/skills-display/UseSkillsDisplayInfo.js'
import SkillProgress from '@/skills-display/components/progress/SkillProgress.vue'
import {useScrollSkillsIntoViewState} from '@/skills-display/stores/UseScrollSkillsIntoViewState.js'
import {useSkillsDisplaySubjectState} from '@/skills-display/stores/UseSkillsDisplaySubjectState.js'
import {useSkillsDisplayAttributesState} from '@/skills-display/stores/UseSkillsDisplayAttributesState.js'
const Prerequisites = defineAsyncComponent(() => import('@/skills-display/components/skill/prerequisites/Prerequisites.vue'))
import SkillAchievementMsg from "@/skills-display/components/progress/celebration/SkillAchievementMsg.vue";
import MarkdownText from "@/common-components/utilities/markdown/MarkdownText.vue";
import SkillNavigation from "@/skills-display/components/utilities/SkillNavigation.vue";
import {useOpenaiService} from "@/common-components/utilities/learning-conent-gen/UseOpenaiService.js";
import SkillsSpinner from "@/components/utils/SkillsSpinner.vue";

const attributes = useSkillsDisplayAttributesState()
const skillsDisplayService = useSkillsDisplayService()
const skillsDisplayInfo = useSkillsDisplayInfo()
const scrollIntoViewState = useScrollSkillsIntoViewState()
const route = useRoute()
const skillState = useSkillsDisplaySubjectState()
const skill = computed(() => skillState.skillSummary)
const loadingSkill = ref(true)
const displayGroupDescription = ref(false);
const groupDescription = ref(null);
const loadingDescription = ref(true);

onMounted(() => {
  loadSkillSummary()
  loadSuggestions()
})
watch( () => route.params.skillId, () => {
  loadSkillSummary()
  loadSuggestions()
});
watch(() => skill.value.groupSkillId, () => {
  groupDescription.value = null;
  displayGroupDescription.value = false;
})
const loadSkillSummary = () => {
  const skillId = skillsDisplayInfo.isDependency() ? route.params.dependentSkillId : route.params.skillId
  skillState.loadSkillSummary(skillId, route.params.crossProjectId || route.query.externalProjectId, route.params.subjectId)
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

const openaiService = useOpenaiService()
const suggestions = ref([])
const loadingSuggestions = ref(true)
const loadSuggestions = () => {
  loadingSuggestions.value = true;
  openaiService.getNextSkillsRecommendations(route.params.projectId, route.params.skillId).then((res) => {
    console.log(res)
    suggestions.value = res;
  }).finally(() => {
    loadingSuggestions.value = false;
  })
}
</script>

<template>
  <div>
    <div v-if="!isLoading">
      <skills-title>{{ attributes.skillDisplayName }} Overview</skills-title>
      <Card class="mt-4">
        <template #title>
          <h5 class="font-bold pb-2"><i class="fa-solid fa-stairs text-green-500"></i> Next Skills Recommendations (Prototype)</h5>
        </template>
        <template #content>
          <skills-spinner :is-loading="loadingSuggestions" />
          <div v-if="!loadingSuggestions" >
            <div v-for="(suggestion, index) in suggestions" :key="suggestion.skillId" class="flex flex-col gap-1">
              <div class="flex gap-3 align-center items-center">
                <div class="font-bold text-lg">{{ suggestion.skillId }}</div>
                <div class="flex gap-2 align-center items-center border p-2 rounded"><i class="fa-solid fa-lightbulb text-yellow-400 text-lg"></i>{{ suggestion.reason }}</div>
              </div>
              <div v-if="index < suggestions.length - 1" class="flex my-2 ml-3">
                <i class="fa-solid fa-arrow-down text-gray-400 text-lg"></i>
              </div>
            </div>
          </div>
        </template>
      </Card>
      <Card class="mt-4" :pt="{ content: { class: 'p-0' }}">
        <template #content>
          <skill-navigation class="mb-6" :skill="skill" @prevButtonClicked="prevButtonClicked" @nextButtonClicked="nextButtonClicked" v-if="skill && (skill.prevSkillId || skill.nextSkillId) && !skillsDisplayInfo.isCrossProject()" />
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
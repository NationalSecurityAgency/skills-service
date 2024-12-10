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
import { computed, onMounted, ref, watch } from 'vue'
import SkillsTitle from '@/skills-display/components/utilities/SkillsTitle.vue'
import { useRoute } from 'vue-router'
import { useSkillsDisplayService } from '@/skills-display/services/UseSkillsDisplayService.js'
import { useSkillsDisplayInfo } from '@/skills-display/UseSkillsDisplayInfo.js'
import SkillProgress from '@/skills-display/components/progress/SkillProgress.vue'
import {useScrollSkillsIntoViewState} from '@/skills-display/stores/UseScrollSkillsIntoViewState.js'
import {useSkillsDisplaySubjectState} from '@/skills-display/stores/UseSkillsDisplaySubjectState.js'
import {useSkillsDisplayAttributesState} from '@/skills-display/stores/UseSkillsDisplayAttributesState.js'
import Prerequisites from '@/skills-display/components/skill/prerequisites/Prerequisites.vue'
import SkillAchievementMsg from "@/skills-display/components/progress/celebration/SkillAchievementMsg.vue";

const attributes = useSkillsDisplayAttributesState()
const skillsDisplayService = useSkillsDisplayService()
const skillsDisplayInfo = useSkillsDisplayInfo()
const scrollIntoViewState = useScrollSkillsIntoViewState()
const route = useRoute()
const skillState = useSkillsDisplaySubjectState()
const skill = computed(() => skillState.skillSummary)
const loadingSkill = ref(true)

onMounted(() => {
  loadSkillSummary()
})
watch( () => route.params.skillId, () => {
  loadSkillSummary()
});
const loadSkillSummary = () => {
  const skillId = skillsDisplayInfo.isDependency() ? route.params.dependentSkillId : route.params.skillId
  skillState.loadSkillSummary(skillId, route.params.crossProjectId, route.params.subjectId)
    .then(() => {
      loadingSkill.value = false
      if (skillId && skill.value.projectId && !skillsDisplayInfo.isCrossProject()) {
        skillsDisplayService.updateSkillHistory(skill.value.projectId, skillId)
      }
      scrollIntoViewState.setLastViewedSkillId(skillId)
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

</script>

<template>
  <div>
    <div v-if="!isLoading">
      <skills-title>{{ attributes.skillDisplayName }} Overview</skills-title>
      <Card class="mt-3" :pt="{ content: { class: 'p-0' }}">
        <template #content>
          <div class="flex-column sm:flex-row align-items-center flex gap-2 mb-4" v-if="skill && (skill.prevSkillId || skill.nextSkillId) && !skillsDisplayInfo.isCrossProject()">
            <div class="w-7rem">
              <SkillsButton
                @click="prevButtonClicked" v-if="skill.prevSkillId"
                outlined
                size="small"
                class="skills-theme-btn"
                data-cy="prevSkill"
                aria-label="previous skill">
                <i class="fas fa-arrow-alt-circle-left mr-1" aria-hidden="true"></i> Previous
              </SkillsButton>
            </div>
            <div class="flex-1 text-center " style="font-size: 0.9rem;" data-cy="skillOrder"><span
              class="font-italic">{{ attributes.skillDisplayName }}</span> <span class="font-semibold">{{ skill.orderInGroup
              }}</span> <span class="font-italic">of</span> <span class="font-semibold">{{ skill.totalSkills }}</span>
            </div>
            <div class="w-7rem text-right">
              <SkillsButton
                @click="nextButtonClicked"
                v-if="skill.nextSkillId"
                class="skills-theme-btn"
                data-cy="nextSkill"
                outlined
                size="small"
                aria-label="next skill">
                Next
                <i class="fas fa-arrow-alt-circle-right ml-1" aria-hidden="true"></i>
              </SkillsButton>
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
    <skills-spinner v-if="isLoading" :is-loading="isLoading" class="mt-5" />
  </div>
</template>

<style scoped>

</style>
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
import { computed } from 'vue'
import { useSkillsDisplayAttributesState } from '@/skills-display/stores/UseSkillsDisplayAttributesState.js'
import VerticalProgressBar from '@/skills-display/components/progress/VerticalProgressBar.vue'

const props = defineProps({
  badge: Object
})
const attributes = useSkillsDisplayAttributesState()

const projectSummaries = computed(() => {
  if (!props.badge.projectLevelsAndSkillsSummaries){
    return []
  }

  const projectsWithLevels = props.badge.projectLevelsAndSkillsSummaries.filter((item) => item.projectLevel)
  if (!projectsWithLevels) {
    return []
  }
  return projectsWithLevels.map((item) => {
    const percentComplete = 0 // calculatePercent(item.projectLevel)

    return {
      badge: props.badge,
      badgeId: props.badge.badgeId,
      projectId: item.projectId,
      projectName: item.projectName,
      skills: item.skills,
      projectLevel: item.projectLevel,
      percentComplete,
      isFullyComplete: percentComplete === 100
    }
  });
})

const calculatePercent = (projectLevel) => {
  if (projectLevel.achievedLevel >= projectLevel.requiredLevel) {
    return 100;
  }
  return Math.trunc((projectLevel.achievedLevel / projectLevel.requiredLevel) * 100);
}

</script>

<template>
  <Card v-if="projectSummaries && projectSummaries.length > 0" class="mt-3" data-cy="globalBadgeProjectLevels">
    <template #content>
      <div v-for="projectSummary in projectSummaries"
           :key="projectSummary.projectId" class="mt-1"
           :data-cy="'gb_'+projectSummary.projectId">

        <div class="text-2xl"><span class="font-italic text-color-secondary">{{ attributes.projectDisplayName }}:</span> {{ projectSummary.projectName }}</div>
        <div class="flex mt-2">
          <div class="text-xl flex-1 mb-1"> Requires {{ attributes.levelDisplayName }} {{ projectSummary.projectLevel?.requiredLevel }}</div>
          <div :class="{ 'text-green-600': projectSummary.isFullyComplete }">
            <i v-if="projectSummary.isFullyComplete" class="fa fa-check"/> {{ projectSummary.percentComplete }}% Complete
          </div>
        </div>

        <vertical-progress-bar :total-progress="projectSummary.percentComplete">
        </vertical-progress-bar>
      </div>
    </template>
  </Card>
</template>

<style scoped>

</style>
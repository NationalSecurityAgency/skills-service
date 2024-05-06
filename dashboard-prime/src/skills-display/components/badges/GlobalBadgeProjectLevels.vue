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

  return props.badge.projectLevelsAndSkillsSummaries.map((item) => {
    const percentComplete = calculatePercent(item.projectLevel)

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
  <Card>
    <template #content>
      <div v-for="projectSummary in projectSummaries"
           :key="projectSummary.projectId" class="mt-1"
           :data-cy="'gb_'+projectSummary.projectId">
        <div class="text-2xl"><span class="font-italic text-color-secondary">{{ attributes.projectDisplayName }}:</span> {{ projectSummary.projectName }}</div>
        <div class="flex mt-2">
          <div class="text-xl flex-1 mb-1"> Requires {{ attributes.levelDisplayName }} {{ projectSummary.projectLevel.requiredLevel }}</div>
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
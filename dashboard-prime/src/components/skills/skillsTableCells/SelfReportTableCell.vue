<script setup>
import { computed } from 'vue'

const props = defineProps({
  skill: Object
})
const selfReportTypePretty = computed(() => {
  return props.skill.selfReportingType === 'HonorSystem' ? 'Honor System' : props.skill.selfReportingType
})
</script>

<template>
  <div>
    <div v-if="skill.selfReportingType === 'Quiz'" :data-cy="`selfReportCell-${skill.skillId}-quiz`">
      <div>
        {{ skill.quizType }}-Based Validation
      </div>
      <div v-if="!skill.isCatalogSkill" class="text-secondary">
        via
      </div>
      <div v-if="!skill.isCatalogSkill">
        <router-link :to="{ name:'Questions', params: { quizId: skill.quizId } }">
          {{ skill.quizName }}
        </router-link>
      </div>
    </div>
    <div v-else>
      <span v-if="skill.isSkillType"
            :data-cy="`selfReportCell-${skill.skillId}`">{{ selfReportTypePretty }}</span>
      <span v-if="skill.isGroupType" class="text-secondary">N/A</span>
    </div>
  </div>
</template>

<style scoped></style>

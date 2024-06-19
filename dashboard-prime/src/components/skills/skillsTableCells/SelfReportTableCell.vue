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

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
import { useNumberFormat } from '@/common-components/filter/UseNumberFormat.js'

const props = defineProps({
  skillsWithOutOfBoundsPoints: {
    type: Array,
    required: true
  },
  projectSkillMinPoints: {
    type: Number,
    required: true
  },
  projectSkillMaxPoints: {
    type: Number,
    required: true
  }
})

const numberFormat = useNumberFormat()
</script>

<template>
  <SkillsDataTable
    tableStoredStateId="quizAccess"
    :value="skillsWithOutOfBoundsPoints"
    aria-label="Skills"
    sortField="skillName"
    auto-max-width="false"
    :sortOrder="1"
    data-cy="skillsWithOutOfBoundsPoints">
    <Column field="skillName" header="Skill"></Column>
    <Column field="totalPoints" header="Points">
      <template #header>
        <i class="fas fa-exclamation-circle mr-1" aria-hidden="true" />
      </template>
      <template #body="slotProps">
        <Tag severity="danger">{{ numberFormat.pretty(slotProps.data.totalPoints) }}</Tag>
        <span v-if="slotProps.data.totalPoints > projectSkillMaxPoints" class="text-primary"> ( <span class="italic">more than</span> {{ numberFormat.pretty(projectSkillMaxPoints)
          }} )</span>
        <span v-if="slotProps.data.totalPoints < projectSkillMinPoints" class="text-primary"> ( <span class="italic">less than</span> {{ numberFormat.pretty(projectSkillMinPoints)
          }} )</span>
      </template>
    </Column>
  </SkillsDataTable>
</template>

<style scoped>

</style>
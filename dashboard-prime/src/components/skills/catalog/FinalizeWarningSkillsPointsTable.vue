<script setup>
import { useNumberFormat } from '@/common-components/filter/UseNumberFormat.js'

const props = defineProps({
  skillsWithOutOfBoundsPoints: {
    type: Array,
    required: true,
  },
  projectSkillMinPoints: {
    type: Number,
    required: true,
  },
  projectSkillMaxPoints: {
    type: Number,
    required: true,
  },
})

const numberFormat = useNumberFormat()
</script>

<template>
  <DataTable
    :value="skillsWithOutOfBoundsPoints"
    sortField="skillName"
    :sortOrder="1"
    data-cy="skillsWithOutOfBoundsPoints">
    <Column field="skillName" header="Skill"></Column>
    <Column field="totalPoints" header="Points">
      <template #header>
        <i class="fas fa-exclamation-circle mr-1" aria-hidden="true" />
      </template>
      <template #body="slotProps">
        <Tag severity="danger">{{ numberFormat.pretty(slotProps.data.totalPoints) }}</Tag>
        <span v-if="slotProps.data.totalPoints > projectSkillMaxPoints" class="text-primary"> ( <span class="italic">more than</span> {{ numberFormat.pretty(projectSkillMaxPoints) }} )</span>
        <span v-if="slotProps.data.totalPoints < projectSkillMinPoints" class="text-primary"> ( <span class="italic">less than</span> {{ numberFormat.pretty(projectSkillMinPoints) }} )</span>
      </template>
    </Column>
  </DataTable>
</template>

<style scoped>

</style>
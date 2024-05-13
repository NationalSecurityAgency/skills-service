<script setup>
import { onMounted, ref } from 'vue'
import SkillsDataTable from '@/components/utils/table/SkillsDataTable.vue'
import { useResponsiveBreakpoints } from '@/components/utils/misc/UseResponsiveBreakpoints.js'
import { useSkillsDisplayThemeState } from '@/skills-display/stores/UseSkillsDisplayThemeState.js'
import { useNavToSkillUtil } from '@/skills-display/components/skill/prerequisites/UseNavToSkillUtil.js'

const props = defineProps({
  items: Array
})
const responsive = useResponsiveBreakpoints()
const themeState = useSkillsDisplayThemeState()
const navHelper = useNavToSkillUtil()
const prerequisites = ref([])

onMounted(() => {
  prerequisites.value = createPrerequisites()
})

const createPrerequisites = () => {
  const alreadyAddedIds = []
  const res = []

  console.log('hie from createPrerequisites')
  props.items.forEach((link) => {
    console.log(link)
    const prereq = link.dependsOn
    const lookup = `${prereq.projectId}-${prereq.skillId}`
    if (!alreadyAddedIds.includes(lookup)) {
      res.push({
        ...prereq,
        achieved: link.achieved,
        isCrossProject: link.crossProject
      })
      alreadyAddedIds.push(lookup)
    }
  })

  return res
}

const getTypeIcon = (type) => {
  return (type === 'Badge') ? 'fa-award' : 'fa-graduation-cap'
}

const getTypeIconColor = (type) => {
  return (type === 'Badge') ? themeState.graphBadgeColor : themeState.graphSkillColor
}


</script>

<template>
  <div>
    <SkillsDataTable
      tableStoredStateId="skillsDisplayPrerequisitesTable"
      :paginator="prerequisites.length > 8"
      :rows="5"
      :value="prerequisites"
      data-cy="prereqTable">
      <Column field="skillName" header="Prerequisite Name" :sortable="true" :class="{'flex': responsive.md.value }">
        <template #header>
          <i class="fas fa-project-diagram mr-1" aria-hidden="true"></i>
        </template>
        <template #body="slotProps">
          <div v-if="slotProps.data.isCrossProject"><i>Shared From</i> <b>{{ slotProps.data.projectName }}</b></div>
          <Button :label="slotProps.data.skillName"
                  :aria-label="`Navigate to prerequisite ${slotProps.data.type} ${slotProps.data.skillName}`"
                  :data-cy="`skillLink-${slotProps.data.projectId}-${slotProps.data.skillId}`"
                  @click="navHelper.navigateToSkill(slotProps.data)"
                  text link class="underline"></Button>

        </template>
      </Column>

      <Column field="type" header="Type" :sortable="true" :class="{'flex': responsive.md.value }">
        <template #header>
          <i class="fas fa-atom mr-1" aria-hidden="true"></i>
        </template>
        <template #body="slotProps">
          <div class="flex align-items-center gap-1">
            <Avatar :icon="`fas ${getTypeIcon(slotProps.data.type)}`"
                    :style="`color: ${getTypeIconColor(slotProps.data.type)}`" />
            <div :aria-label="`Prerequisite's type is ${slotProps.data.type}`" data-cy="prereqType">
              {{ slotProps.data.type }}
            </div>
          </div>
        </template>
      </Column>

      <Column field="achieved" header="Achieved" :sortable="true" :class="{'flex': responsive.md.value }">
        <template #header>
          <i class="far fa-check-square mr-1" aria-hidden="true"></i>
        </template>
        <template #body="slotProps">
          <div data-cy="isAchievedCell">
            <div v-if="slotProps.data.achieved" class="font-weight-bold sd-theme-primary-color"
                 data-cy="achievedCellYes"
                 :aria-label="`${slotProps.data.skillName} ${slotProps.data.type} was achieved`"
                 :style="`color: ${themeState.graphAchievedColor}`">✓Yes
            </div>
            <div v-else class=""
                 data-cy="achievedCellNo"
                 :aria-label="`${slotProps.data.skillName} ${slotProps.data.type} is not achieved`">Not Yet...
            </div>
          </div>
        </template>
      </Column>

    </SkillsDataTable>
  </div>
</template>

<style scoped>

</style>
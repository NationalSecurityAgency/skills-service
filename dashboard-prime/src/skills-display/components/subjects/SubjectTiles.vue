<script setup>
import { computed } from 'vue'
import { useUserProgressSummaryState } from '@/skills-display/stores/UseUserProgressSummaryState.js'
import NoDataYet from '@/common-components/utilities/NoDataYet.vue'
import { useSkillsDisplayPreferencesState } from '@/skills-display/stores/UseSkillsDisplayPreferencesState.js'
import SubjectTile from '@/skills-display/components/subjects/SubjectTile.vue'

const userProgress = useUserProgressSummaryState()
const preferences = useSkillsDisplayPreferencesState()
const hasData = computed(() => userProgress.userProgressSummary.subjects.length > 0)
</script>

<template>
  <div>
    <Card v-if="!hasData">
      <template #content>
        <no-data-yet
          class="my-2"
          :title="`${preferences.subjectDisplayName}s have not been added yet.`"
          :sub-title="`Please contact this ${preferences.projectDisplayName.toLowerCase()}'s administrator.`" />
      </template>
    </Card>
<!--    <search-all-project-skills v-if="hasData" />-->
    <div v-if="hasData" class="flex flex-wrap gap-2">
      <div v-for="(subject, index) in userProgress.userProgressSummary.subjects"
           :key="`unique-subject-${index}`"
           class="flex-1 w-min-20rem md:max-w-30rem">
        <div class="">
          <subject-tile :subject="subject"/>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>

</style>
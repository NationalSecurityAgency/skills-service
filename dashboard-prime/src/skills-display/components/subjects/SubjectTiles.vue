<script setup>
import { computed } from 'vue'
import { useUserProgressSummaryState } from '@/skills-display/stores/UseUserProgressSummaryState.js'
import SubjectTile from '@/skills-display/components/subjects/SubjectTile.vue'
import NoContent2 from '@/components/utils/NoContent2.vue'
import SearchAllProjectSkills from '@/skills-display/components/subjects/SearchAllProjectSkills.vue'
import { useSkillsDisplayAttributesState } from '@/skills-display/stores/UseSkillsDisplayAttributesState.js'

const userProgress = useUserProgressSummaryState()
const attributes = useSkillsDisplayAttributesState()
const hasData = computed(() => userProgress.userProgressSummary.subjects?.length > 0)
</script>

<template>
  <div>
    <Card v-if="!hasData">
      <template #content>
        <no-content2
          class="my-2 text-center"
          :title="`${attributes.subjectDisplayName}s have not been added yet.`"
          :message="`Please contact this ${attributes.projectDisplayName.toLowerCase()}'s administrator.`" />
      </template>
    </Card>
    <search-all-project-skills v-if="hasData && !attributes.isSummaryOnly" class="mb-3"/>
    <div v-if="hasData" class="flex flex-wrap gap-2">
      <div v-for="(subject, index) in userProgress.userProgressSummary.subjects"
           :key="`unique-subject-${index}`"
           class="flex-1 w-min-20rem md:max-w-30rem">
        <div class="">
          <subject-tile :subject="subject" :tile-index="index"/>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>

</style>
<script setup>
import SkillsTitle from '@/skills-display/components/utilities/SkillsTitle.vue'
import { useSkillsDisplaySubjectState } from '@/skills-display/stores/UseSkillsDisplaySubjectState.js'
import { onMounted } from 'vue'
import { useRoute } from 'vue-router'

const subject = useSkillsDisplaySubjectState()
const route = useRoute()

onMounted(() => {
  subject.loadSubjectSummary(route.params.subjectId)
})

</script>

<template>
  <div>
    <skills-spinner :is-loading="subject.loadingSubjectSummary" />
    <div v-if="!subject.loadingSubjectSummary">
      <skills-title>{{  subject.subjectSummary.subject }}</skills-title>
      <div>
        skills and what not
      </div>
    </div>
  </div>
</template>

<style scoped>

</style>
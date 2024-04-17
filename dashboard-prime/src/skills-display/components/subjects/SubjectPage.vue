<script setup>
import SkillsTitle from '@/skills-display/components/utilities/SkillsTitle.vue'
import { useSkillsDisplaySubjectState } from '@/skills-display/stores/UseSkillsDisplaySubjectState.js'
import { onMounted } from 'vue'
import { useRoute } from 'vue-router'
import UserOverallProgress from '@/skills-display/components/home/UserOverallProgress.vue'
import { tryOnBeforeMount } from '@vueuse/core'
import MarkdownText from '@/common-components/utilities/markdown/MarkdownText.vue'
import SkillsProgressList from '@/skills-display/components/progress/SkillsProgressList.vue'

const subject = useSkillsDisplaySubjectState()
const route = useRoute()
tryOnBeforeMount(() => {
  subject.loadingSubjectSummary = true
})
onMounted(() => {
  subject.loadSubjectSummary(route.params.subjectId)
})

</script>

<template>
  <div>
    <skills-spinner :is-loading="subject.loadingSubjectSummary" />
    <div v-if="!subject.loadingSubjectSummary">
      <skills-title>{{  subject.subjectSummary.subject }}</skills-title>
      <div class="mt-3">
        <user-overall-progress :is-subject="true"/>
      </div>

      <Card v-if="subject.subjectSummary.description" class="mt-2">
        <template #title>
          <div class="h6 card-title mb-0 float-left">Description</div>
        </template>
        <template #content>
          <markdown-text :text="subject.subjectSummary.description" />
        </template>
        <template #footer v-if="subject.subjectSummary.helpUrl">
          <a :href="subject.subjectSummary.helpUrl" target="_blank" rel="noopener"
             class="btn btn-sm btn-outline-info skills-theme-btn">
            Learn More <i class="fas fa-external-link-alt"></i>
          </a>
        </template>
      </Card>
<!--      @points-earned="refreshHeader"-->
<!--      @scrollTo="scrollToLastViewedSkill"-->
      <skills-progress-list
        class="mt-3"
        :subject="subject.subjectSummary" />

    </div>
  </div>
</template>

<style scoped>

</style>
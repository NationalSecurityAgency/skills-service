<script setup>
import SkillsTitle from '@/skills-display/components/utilities/SkillsTitle.vue'
import { useSkillsDisplaySubjectState } from '@/skills-display/stores/UseSkillsDisplaySubjectState.js'
import { onMounted } from 'vue'
import { useRoute } from 'vue-router'
import UserOverallProgress from '@/skills-display/components/home/UserOverallProgress.vue'
import { tryOnBeforeMount } from '@vueuse/core'
import MarkdownText from '@/common-components/utilities/markdown/MarkdownText.vue'
import SkillsProgressList from '@/skills-display/components/progress/SkillsProgressList.vue'
import MyRank from '@/skills-display/components/rank/MyRank.vue'
import PointProgressChart from '@/skills-display/components/progress/points/PointProgressChart.vue'

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

      <div class="mt-3 flex flex-column md:flex-row gap-4 ">
        <div class="flex align-items-center">
          <my-rank class="w-full"/>
        </div>
        <div class="flex-1 align-items-center">
          <point-progress-chart />
        </div>
      </div>

      <Card v-if="subject.subjectSummary.description" class="mt-2">
        <template #title>
          <div class="h6 card-title mb-0 float-left">Description</div>
        </template>
        <template #content>
          <markdown-text :text="subject.subjectSummary.description" />
        </template>
        <template #footer v-if="subject.subjectSummary.helpUrl">
          <a :href="subject.subjectSummary.helpUrl" target="_blank" rel="noopener">
            <Button outlined size="small">
              <i class="fas fa-question-circle mr-1" aria-hidden="true"></i>
              Learn More
              <i class="fas fa-external-link-alt ml-1" aria-hidden="true"></i>
            </Button>
          </a>


        </template>
      </Card>
<!--      @points-earned="refreshHeader"-->
<!--      @scrollTo="scrollToLastViewedSkill"-->
      <skills-progress-list
        class="mt-3" />

    </div>
  </div>
</template>

<style scoped>

</style>
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
import SkillsTitle from '@/skills-display/components/utilities/SkillsTitle.vue'
import { useSkillsDisplaySubjectState } from '@/skills-display/stores/UseSkillsDisplaySubjectState.js'
import { onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import ProgressAndRankingOverview from '@/skills-display/components/ProgressAndRankingOverview.vue'
import { tryOnBeforeMount } from '@vueuse/core'
import MarkdownText from '@/common-components/utilities/markdown/MarkdownText.vue'
import SkillsProgressList from '@/skills-display/components/progress/SkillsProgressList.vue'
import TagList from "@/skills-display/components/skill/tags/TagList.vue";

const subject = useSkillsDisplaySubjectState()
const route = useRoute()
tryOnBeforeMount(() => {
  subject.loadingSubjectSummary = true
})
onMounted(() => {
  subject.loadSubjectSummary(route.params.subjectId)
})

watch( () => route.params.subjectId, () => {
  subject.loadSubjectSummary(route.params.subjectId)
});

</script>

<template>
  <div>
    <skills-spinner :is-loading="subject.loadingSubjectSummary" />
    <div v-if="!subject.loadingSubjectSummary">
      <skills-title>{{  subject.subjectSummary.subject }}</skills-title>
      <progress-and-ranking-overview class="mt-4" is-subject />

      <Card v-if="subject.subjectSummary.description" class="mt-2" :pt="{ body: { class : 'px-4! py-3!'}}">
        <template #title>
          <div class="h6 card-title mb-0 float-left">Description</div>
        </template>
        <template #content>
          <markdown-text :text="subject.subjectSummary.description" data-cy="subjectDescription"/>
        </template>
        <template #footer v-if="subject.subjectSummary.helpUrl">
          <a :href="subject.subjectSummary.helpUrl" target="_blank" rel="noopener" tabindex="-1">
            <Button outlined size="small">
              <i class="fas fa-question-circle mr-1" aria-hidden="true"></i>
              Learn More
              <i class="fas fa-external-link-alt ml-1" aria-hidden="true"></i>
            </Button>
          </a>


        </template>
      </Card>

      <tag-list class="mt-4" data-cy="skillTagsCard" />

      <skills-progress-list
        class="mt-4" />

    </div>
  </div>
</template>

<style scoped>

</style>

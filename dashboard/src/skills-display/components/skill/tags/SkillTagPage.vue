/*
Copyright 2026 SkillTree

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
import { computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import SkillsTitle from '@/skills-display/components/utilities/SkillsTitle.vue'
import SkillsProgressList from '@/skills-display/components/progress/SkillsProgressList.vue'
import { useSkillsDisplayAttributesState } from '@/skills-display/stores/UseSkillsDisplayAttributesState.js'
import { useSkillsDisplaySubjectState } from '@/skills-display/stores/UseSkillsDisplaySubjectState.js'
import SkillTagProgress from '@/skills-display/components/skill/tags/SkillTagProgress.vue'

const route = useRoute()
const attributes = useSkillsDisplayAttributesState()
const summaryAndSkillsState = useSkillsDisplaySubjectState()

const tag = computed(() => summaryAndSkillsState.subjectSummary)
const isLoading = computed(() => summaryAndSkillsState.loadingSkillTagSummary)

onMounted(() => {
  loadSkillTagSummary()
})

const loadSkillTagSummary = () => {
  summaryAndSkillsState.loadSkillTagSummary(route.params.tagId)
}

</script>

<template>
  <div>
    <skills-spinner v-if="isLoading" :is-loading="isLoading" class="mt-8" />

    <div v-if="!isLoading" class="flex flex-col gap-3">
      <div>
        <skills-title>{{ attributes.skillDisplayName }} Tag Overview</skills-title>
      </div>

      <skill-tag-progress :skill-tag-overview="tag" :build-link="false" />

      <skills-progress-list type="tag" />
    </div>
  </div>
</template>

<style scoped></style>
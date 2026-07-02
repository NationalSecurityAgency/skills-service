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
import { computed, onMounted, ref } from 'vue'
import SkillsTitle from '@/skills-display/components/utilities/SkillsTitle.vue'
import { useSkillsDisplayAttributesState } from '@/skills-display/stores/UseSkillsDisplayAttributesState.js'
import { useSkillsDisplayService } from '@/skills-display/services/UseSkillsDisplayService.js'
import SkillTagProgress from '@/skills-display/components/skill/tags/SkillTagProgress.vue'

const attributes = useSkillsDisplayAttributesState()
const skillsDisplayService = useSkillsDisplayService()
const isLoading = computed(() => loading.value)
const skillTagsSummaries = ref(null)
const loading = ref(true)

onMounted(() => {
  skillsDisplayService.getAllProjectSkillTagSummaries()
      .then((res) => {
        skillTagsSummaries.value = res
        return res
      }).finally(() => {
        loading.value = false
      })
})
</script>

<template>
  <div>
    <skills-spinner v-if="isLoading" :is-loading="isLoading" class="mt-8" />

    <div v-if="!isLoading" class="flex flex-col gap-3">
      <div>
        <skills-title>{{ attributes.projectDisplayName }} Tags</skills-title>
      </div>

      <div v-for="(tag, index) in skillTagsSummaries"
           :key="`tag-${tag.tagId}`"
           :id="`tagRow-${tag.tagId}`"
           class="skills-theme-bottom-border-with-background-color"
      >
        <skill-tag-progress :index="index" :skill-tag-overview="tag" :build-link="true" />
      </div>
    </div>
  </div>
</template>

<style scoped></style>
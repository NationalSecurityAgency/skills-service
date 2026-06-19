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
import VerticalProgressBar from '@/skills-display/components/progress/VerticalProgressBar.vue'
import { useNumberFormat } from '@/common-components/filter/UseNumberFormat.js'
import { useSkillsDisplayAttributesState } from '@/skills-display/stores/UseSkillsDisplayAttributesState.js'
import { useSkillsDisplaySubjectState } from '@/skills-display/stores/UseSkillsDisplaySubjectState.js'

const route = useRoute()
const attributes = useSkillsDisplayAttributesState()
const summaryAndSkillsState = useSkillsDisplaySubjectState()
const nF = useNumberFormat()

const tag = computed(() => summaryAndSkillsState.subjectSummary)
const isLoading = computed(() => summaryAndSkillsState.loadingSkillTagSummary)

onMounted(() => {
  loadSkillTagSummary()
})

const loadSkillTagSummary = () => {
  summaryAndSkillsState.loadSkillTagSummary(route.params.tagId)
}

const tagName = computed(() => {
  return tag.value.tag || tag.value.tagValue || route.query.tagValue || route.params.tagId
})

const skillsAchieved = computed(() => tag.value.skillsAchieved || 0)
const totalSkills = computed(() => tag.value.totalSkills || tag.value.skills?.length || 0)

const progressPercent = computed(() => {
  return totalSkills.value > 0 ? Math.trunc((skillsAchieved.value / totalSkills.value) * 100) : 0
})
</script>

<template>
  <div>
    <skills-spinner v-if="isLoading" :is-loading="isLoading" class="mt-8" />

    <div v-if="!isLoading" class="flex flex-col gap-3">
      <div>
        <skills-title>{{ attributes.skillDisplayName }} Tag Overview</skills-title>
      </div>

      <Card>
        <template #content>
          <div class="flex flex-col gap-4">
            <div class="flex items-center gap-3">
              <div class="border rounded p-3 text-primary">
                <i class="fa-solid fa-tag text-4xl" aria-hidden="true"></i>
              </div>

              <div class="flex-1 flex flex-col gap-2">
                <div class="flex items-end gap-1">
                  <div class="flex-1">
                    <span class="text-3xl" data-cy="skillTagName">{{ tagName }}</span>
                  </div>

                  <div data-cy="skillTagProgress">
                    <span>{{ nF.pretty(skillsAchieved) }}</span>
                    /
                    <span>{{ nF.pretty(totalSkills) }}</span>
                    {{ attributes.skillDisplayNamePlural }}
                  </div>
                </div>

                <div>
                  <vertical-progress-bar
                    :total-progress="progressPercent"
                    :disable-daily-color="true" />
                </div>
              </div>
            </div>
          </div>
        </template>
      </Card>

      <skills-progress-list type="tag" />
    </div>
  </div>
</template>

<style scoped></style>
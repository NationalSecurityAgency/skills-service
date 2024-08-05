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
import { computed, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import BadgeCatalogItem from '@/skills-display/components/badges/BadgeCatalogItem.vue'
import SkillsProgressList from '@/skills-display/components/progress/SkillsProgressList.vue'
import { useSkillsDisplaySubjectState } from '@/skills-display/stores/UseSkillsDisplaySubjectState.js'
import { useSkillsDisplayInfo } from '@/skills-display/UseSkillsDisplayInfo.js'
import GlobalBadgeProjectLevels from '@/skills-display/components/badges/GlobalBadgeProjectLevels.vue'
import Prerequisites from '@/skills-display/components/skill/prerequisites/Prerequisites.vue'

const route = useRoute()
const summaryAndSkillsState = useSkillsDisplaySubjectState()
const skillsDisplayInfo = useSkillsDisplayInfo()

const badge = computed(() => summaryAndSkillsState.subjectSummary)
const isLoading = computed(() => summaryAndSkillsState.loadingBadgeSummary)

onMounted(() => {
  loadBadgeInfo()
})
watch( () => route.params.badgeId, () => {
  loadBadgeInfo()
});
const loadBadgeInfo = () => {
  const isGlobalBadge = skillsDisplayInfo.isGlobalBadgePage.value
  summaryAndSkillsState.loadBadgeSummary(route.params.badgeId, isGlobalBadge)
}

const locked = computed(() => {
  return badge.value.dependencyInfo && !badge.value.dependencyInfo.achieved;
})
const badgeTitle = computed(() => skillsDisplayInfo.isGlobalBadgePage.value ? 'Global Badge Details' : 'Badge Details')
</script>

<template>
  <div>
    <skills-spinner :is-loading="isLoading" class="mt-8" />

    <div v-if="!isLoading">
      <skills-title>{{ badgeTitle }}</skills-title>

      <Card class="mt-3">
        <template #content>
          <badge-catalog-item :badge="badge"></badge-catalog-item>
          <Message v-if="locked" icon="fas fa-lock" severity="warn" :closable="false">
            Badge has <Tag>{{ badge.dependencyInfo.numDirectDependents }}</Tag> direct prerequisite(s).
            <span>Please see its prerequisites below.</span>

          </Message>
        </template>
        <template #footer v-if="badge.helpUrl">
          <a :href="badge.helpUrl" target="_blank" rel="noopener" class="btn btn-sm btn-outline-info skills-theme-btn">
            Learn More <i class="fas fa-external-link-alt"></i>
          </a>
        </template>
      </Card>
      <skills-progress-list
        v-if="badge && !(skillsDisplayInfo.isGlobalBadgePage.value && !(summaryAndSkillsState.subjectSummary?.skills?.length > 0))"
        :subject="badge"
        type="badge"
        class="mt-3"
        :badge-is-locked="locked"/>

      <prerequisites />
      <global-badge-project-levels :badge="badge"/>

    </div>
  </div>
</template>

<style scoped>

</style>
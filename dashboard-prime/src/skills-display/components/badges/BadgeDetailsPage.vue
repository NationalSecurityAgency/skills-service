<script setup>
import SkillsTitle from '@/skills-display/components/utilities/SkillsTitle.vue'
import { ref, onMounted, computed, watch } from 'vue'
import { useSkillsDisplayService } from '@/skills-display/services/UseSkillsDisplayService.js'
import { useRoute } from 'vue-router'
import BadgeCatalogItem from '@/skills-display/components/badges/BadgeCatalogItem.vue'
import SkillsProgressList from '@/skills-display/components/progress/SkillsProgressList.vue'
import { useSkillsDisplaySubjectState } from '@/skills-display/stores/UseSkillsDisplaySubjectState.js'
import { useSkillsDisplayInfo } from '@/skills-display/UseSkillsDisplayInfo.js'
import GlobalBadgeProjectLevels from '@/skills-display/components/badges/GlobalBadgeProjectLevels.vue'
import Prerequisites from '@/skills-display/components/skill/prerequisites/Prerequisites.vue'

const skillsDisplayService = useSkillsDisplayService()
const route = useRoute()
const summaryAndSkillsState = useSkillsDisplaySubjectState()
const skillsDisplayInfo = useSkillsDisplayInfo()

// const loadingBadge = ref(true)
const loadingPrerequisites = ref(true)
const badge = computed(() => summaryAndSkillsState.subjectSummary)
const dependencies = ref([])

const isLoading = computed(() => summaryAndSkillsState.loadingBadgeSummary || loadingPrerequisites.value)

onMounted(() => {
  loadBadgeInfo()
})
watch( () => route.params.badgeId, () => {
  loadingPrerequisites.value = true
  loadBadgeInfo()
});
const loadBadgeInfo = () => {
  const isGlobalBadge = skillsDisplayInfo.isGlobalBadgePage.value
  summaryAndSkillsState.loadBadgeSummary(route.params.badgeId, isGlobalBadge)
  if (!isGlobalBadge) {
    loadDependencies()
  } else {
    loadingPrerequisites.value = false
  }
}
const loadDependencies = () => {
  loadingPrerequisites.value = true
  return skillsDisplayService.getSkillDependencies(route.params.badgeId)
    .then((res) => {
      dependencies.value = res.dependencies
    }).finally(() => {
      loadingPrerequisites.value = false
    })
}

const locked = computed(() => {
  return badge.value.dependencyInfo && !badge.value.dependencyInfo.achieved;
})
</script>

<template>
  <div>
    <skills-spinner :is-loading="isLoading" class="mt-8" />

    <div v-if="!isLoading">
      <skills-title>Badge Details</skills-title>

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
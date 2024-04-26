<script setup>
import { computed, ref, onMounted } from 'vue'
import { useSkillsDisplayPreferencesState } from '@/skills-display/stores/UseSkillsDisplayPreferencesState.js'
import SkillsTitle from '@/skills-display/components/utilities/SkillsTitle.vue'
import { useRoute } from 'vue-router'
import { useSkillsDisplayService } from '@/skills-display/services/UseSkillsDisplayService.js'
import { useSkillsDisplayInfo } from '@/skills-display/UseSkillsDisplayInfo.js'
import SkillProgress from '@/skills-display/components/progress/SkillProgress.vue'
import { useScrollSkillsIntoViewState } from '@/skills-display/stores/UseScrollSkillsIntoViewState.js'
import { useSkillsDisplaySubjectState } from '@/skills-display/stores/UseSkillsDisplaySubjectState.js'

const displayPreferences = useSkillsDisplayPreferencesState()
const skillsDisplayService = useSkillsDisplayService()
const skillsDisplayInfo = useSkillsDisplayInfo()
const scrollIntoViewState = useScrollSkillsIntoViewState()
const route = useRoute()
const skillState = useSkillsDisplaySubjectState()
const skill = computed(() => skillState.skillSummary)
const loadingSkill = ref(true)

onMounted(() => {
  loadSkillSummary()
})
const loadSkillSummary = () => {
  const skillId = isDependency() ? route.params.dependentSkillId : route.params.skillId
  skillState.loadSkillSummary(skillId, route.params.crossProjectId, route.params.subjectId)
    .then(() => {
      loadingSkill.value = false
      if (skillId && skill.value.projectId && !isCrossProject()) {
        skillsDisplayService.updateSkillHistory(skill.value.projectId, skillId)
      }
      scrollIntoViewState.setLastViewedSkillId(skillId)
    })
}
const isDependency = () => {
  const routeName = route.name
  return routeName === 'crossProjectSkillDetails' || routeName === 'crossProjectSkillDetailsUnderBadge'
}
const isCrossProject = () => {
  const routeName = route.name
  return routeName === 'crossProjectSkillDetails' || route.params.crossProjectId
}

const prevButtonClicked = () => {
  const params = { skillId: skillState.skillSummary.prevSkillId, projectId: route.params.projectId }
  skillsDisplayInfo.routerPush(
    'skillDetails',
    params
  )
}
const nextButtonClicked = () => {
  const params = { skillId: skillState.skillSummary.nextSkillId, projectId: route.params.projectId }
  skillsDisplayInfo.routerPush(
    'skillDetails',
    params
  )
}

const isLoading = computed(() => loadingSkill.value)

</script>

<template>
  <div>
    <div v-if="!isLoading">
      <skills-title>{{ displayPreferences.skillDisplayName }} Overview</skills-title>
      <Card class="mt-3" :pt="{ content: { class: 'p-0' }}">
        <template #content>
          <div class="flex mb-4" v-if="skill && (skill.prevSkillId || skill.nextSkillId) && !isCrossProject()">
            <div>
              <SkillsButton
                @click="prevButtonClicked" v-if="skill.prevSkillId"
                outlined
                size="small"
                class="skills-theme-btn"
                data-cy="prevSkill"
                aria-label="previous skill">
                <i class="fas fa-arrow-alt-circle-left mr-1" aria-hidden="true"></i> Previous
              </SkillsButton>
            </div>
            <div class="flex-1 text-center " style="font-size: 0.9rem;" data-cy="skillOrder"><span
              class="font-italic">{{ displayPreferences.skillDisplayName }}</span> <b>{{ skill.orderInGroup
              }}</b> <span class="font-italic">of</span> <b>{{ skill.totalSkills }}</b>
            </div>
            <div>
              <SkillsButton
                @click="nextButtonClicked"
                v-if="skill.nextSkillId"
                class="skills-theme-btn"
                data-cy="nextSkill"
                outlined
                size="small"
                aria-label="next skill">
                Next
                <i class="fas fa-arrow-alt-circle-right ml-1" aria-hidden="true"></i>
              </SkillsButton>
            </div>
          </div>
          <div class="card-body text-center text-sm-left">
            <!--          @points-earned="onPointsEarned" -->
            <skill-progress :skill="skill" />
          </div>
        </template>
      </Card>

      <!--      <skill-dependencies class="mt-2" v-if="dependencies && dependencies.length > 0" :dependencies="dependencies"-->
      <!--                          :skill-id="$route.params.skillId" :subject-id="this.$route.params.subjectId"></skill-dependencies>-->
    </div>
    <skills-spinner v-if="isLoading" :is-loading="isLoading" class="mt-5" />
  </div>
</template>

<style scoped>

</style>
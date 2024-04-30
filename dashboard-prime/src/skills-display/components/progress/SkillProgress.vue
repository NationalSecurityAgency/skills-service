<script setup>
import SkillProgressNameRow from '@/skills-display/components/progress/skill/SkillProgressNameRow.vue'
import { useRoute } from 'vue-router'
import { computed } from 'vue'
import { useSkillsDisplayInfo } from '@/skills-display/UseSkillsDisplayInfo.js'
import SkillsSummaryCards from '@/skills-display/components/progress/SkillsSummaryCards.vue'
import MarkdownText from '@/common-components/utilities/markdown/MarkdownText.vue'
import { useSkillsDisplayPreferencesState } from '@/skills-display/stores/UseSkillsDisplayPreferencesState.js'
import SkillOverviewFooter from '@/skills-display/components/skill/SkillOverviewFooter.vue'
import SkillProgressBar from '@/skills-display/components/progress/skill/SkillProgressBar.vue'
import AchievementDate from '@/skills-display/components/skill/AchievementDate.vue'
import SkillBadgesAndTags from '@/skills-display/components/progress/skill/SkillBadgesAndTags.vue'
import { useSkillsDisplayAttributesState } from '@/skills-display/stores/UseSkillsDisplayAttributesState.js'

const props = defineProps({
  skill: Object,
  showDescription: {
    type: Boolean,
    default: true
  },
  enableDrillDown: {
    type: Boolean,
    default: false
  },
  subjectId: {
    type: String,
    required: false
  },
  badgeId: {
    type: String,
    required: false
  },
  type: {
    type: String,
    default: 'subject'
  },
  childSkillHighlightString: {
    type: String,
    default: ''
  },
  badgeIsLocked: {
    type: Boolean,
    default: false,
    required: false
  },
  videoCollapsedByDefault: {
    type: Boolean,
    default: false,
    required: false
  },
})
const emit = defineEmits(['add-tag-filter'])
const route = useRoute()
const skillsDisplayInfo = useSkillsDisplayInfo()
const preferences = useSkillsDisplayPreferencesState()
const attributes = useSkillsDisplayAttributesState()

const buildToRoute = () => {
  if (!props.enableDrillDown || !props.skill.isSkillType) {
    return null
  }
  let name = skillsDisplayInfo.getContextSpecificRouteName('skillDetails')
  const params = { skillId: props.skill.skillId, projectId: props.skill.projectId }
  if (route.params.subjectId) {
    params.subjectId = route.params.subjectId
  } else if (route.params.badgeId) {
    params.badgeId = route.params.badgeId
    name = (props.type === 'global-badge') ? 'globalBadgeSkillDetails' : 'badgeSkillDetails'
  } else if (props.skill.crossProject && props.skill.projectId) {
    params.crossProjectId = props.skill.projectId
  }
  return { name, params }
}
const toRoute = buildToRoute()

const locked = computed(() => {
  return (props.skill && props.skill.isLocked) || props.badgeIsLocked
})
const addTagFilter = (tag) => {
  emit('add-tag-filter', tag)
}

const isSkillsGroupWithChildren = computed(() => {
  return props.skill?.isSkillsGroupType && props.skill?.children && props.skill?.children.length > 0
})
const childSkillsInternal = computed(() => {
  return isSkillsGroupWithChildren.value ? props.skill.children.map((item) => ({ ...item, childSkill: true })) : []
})


</script>

<template>
  <div class="text-left" data-cy="skillProgress">
    <!--    <div v-if="skill.crossProject" class="row border-bottom mb-3 text-primary text-center">-->
    <!--      <div class="col-md-6 text-md-left">-->
    <!--        <div class="h4"><span class="text-muted">{{ projectDisplayName }}:</span> {{ skill.projectName }}</div>-->
    <!--      </div>-->
    <!--      <div class="col-md-6 text-md-right text-success text-uppercase">-->
    <!--        <div class="h5"><i class="fa fa-vector-square"/> Cross-{{ projectDisplayName }} {{ skillDisplayName }}</div>-->
    <!--      </div>-->
    <!--    </div>-->

    <!--    <div v-if="skill.crossProject && !isSkillComplete" class="alert alert-primary text-center" role="alert" data-cy="crossProjAlert">-->
    <!--      This is a cross-{{ projectDisplayName.toLowerCase() }} {{ skillDisplayName.toLowerCase() }}! In order to complete this {{ skillDisplayName.toLowerCase() }} please visit <strong>{{-->
    <!--        skill.projectName-->
    <!--      }}</strong> {{ projectDisplayName.toLowerCase() }}! Happy playing!!-->
    <!--    </div>-->


    <skill-progress-name-row
      :skill="skill"
      :badge-is-locked="badgeIsLocked"
      :to-route="toRoute"
      :child-skill-highlight-string="childSkillHighlightString"
      :type="type" />
    <div class="mt-1">
      <router-link
        v-if="toRoute"
        :to="toRoute"
        tabindex="-1"
        :aria-label="`Navigate to ${skill.skill}`">
        <!--        <vertical-progress-bar-->
        <!--          class="border-1 border-transparent hover:border-orange-700 border-round"-->
        <!--          data-cy="skillProgressBar" />-->
        <skill-progress-bar data-cy="skillProgressBar"
                            class="border-1 border-transparent hover:border-orange-700 border-round"
                            :skill="skill" />
      </router-link>
      <skill-progress-bar
        v-else
        :skill="skill"
        data-cy="skillProgressBar" />

      <!--        <progress-bar :skill="skill" v-on:progressbar-clicked="skillClicked"-->
      <!--                      :badge-is-locked="badgeIsLocked"-->
      <!--                      :bar-size="skill.groupId ? 12 : 22"-->
      <!--                      :class="{ 'skills-navigable-item' : allowDrillDown }" />-->
    </div>
    <skill-badges-and-tags :skill="skill" :badge-id="badgeId" :enable-to-add-tag="enableDrillDown"
                           @add-tag-filter="addTagFilter" />
    <div v-if="showDescription || (skill.type === 'SkillsGroup' && attributes.groupDescriptionsOn)"
         :data-cy="`skillDescription-${skill.skillId}`">

      <div v-if="skill.type === 'SkillsGroup'">
        <p class="skills-text-description text-primary mt-3" style="font-size: 0.9rem;">
          <markdown-text
            :instance-id="`skillDescription-${skill.skillId}`"
            v-if="skill.description && skill.description.description"
            :text="skill.description.description" />
        </p>
      </div>
      <div v-if="skill.type === 'Skill'">
        <div v-if="locked && skill.dependencyInfo" class="text-center text-muted locked-text">
          *** Skill has
          <Tag>{{ skill.dependencyInfo.numDirectDependents }}</Tag>
          direct prerequisite(s).
          <span v-if="allowDrillDown">Click <i class="fas fa-lock icon"></i> to see its prerequisites.</span>
          <span v-else>Please see its prerequisites below.</span>
          ***
        </div>
        <p v-if="skill.subjectName" class="text-secondary mt-3">
          {{ preferences.subjectDisplayName }}: {{ skill.subjectName }}
        </p>

        <achievement-date
          v-if="skill && skill.achievedOn"
          :date="skill.achievedOn" class="mt-2" />

        <!--        <partial-points-alert v-if="!allowDrillDown" :skill="skill" :is-locked="locked" />-->
        <skills-summary-cards v-if="!locked" :skill="skill" class="mt-3" />
        <!--        <catalog-import-status :skill="skill" />-->
        <!--        <skill-video v-if="skillInternal" :skill="skillInternal"-->
        <!--                     :video-collapsed-by-default="videoCollapsedByDefault"-->
        <!--                     @points-earned="pointsEarned"-->
        <!--                     class="mt-2" />-->
        <p class="skills-text-description text-primary mt-3" style="font-size: 0.9rem;">
          <markdown-text
            :instance-id="`skillDescription-${skill.skillId}`"
            v-if="skill.description && skill.description.description"
            :text="skill.description.description" />
        </p>

        <div>
          <skill-overview-footer :skill="skill" />
        </div>
      </div>
    </div>

    <div v-if="skill.isSkillsGroupType && childSkillsInternal" class="ml-4 mt-3">
      <div v-for="(childSkill, index) in childSkillsInternal"
           :key="`group-${skill.skillId}_skill-${childSkill.skillId}`"
           :id="`skillRow-${childSkill.skillId}`"
           class="skills-theme-bottom-border-with-background-color"
      >
        <!--        @points-earned="onChildSkillPointsEarned"-->
        <skill-progress
          :id="`group-${skill.skillId}_skillProgress-${childSkill.skillId}`"
          class="mb-3"
          :skill="childSkill"
          :subjectId="subjectId"
          :badgeId="badgeId"
          :type="type"
          :enable-drill-down="true"
          :show-description="showDescription"
          :child-skill-highlight-string="childSkillHighlightString"
          :data-cy="`group-${skill.skillId}_skillProgress-${childSkill.skillId}`"
          @add-tag-filter="addTagFilter"
        ></skill-progress>
      </div>
    </div>

  </div>
</template>

<style scoped>

</style>
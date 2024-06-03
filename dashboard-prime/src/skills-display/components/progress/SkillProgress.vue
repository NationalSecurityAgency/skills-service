<script setup>
import SkillProgressNameRow from '@/skills-display/components/progress/skill/SkillProgressNameRow.vue'
import { useRoute } from 'vue-router'
import { computed, ref } from 'vue'
import { useSkillsDisplayInfo } from '@/skills-display/UseSkillsDisplayInfo.js'
import SkillsSummaryCards from '@/skills-display/components/progress/SkillsSummaryCards.vue'
import MarkdownText from '@/common-components/utilities/markdown/MarkdownText.vue'
import SkillOverviewFooter from '@/skills-display/components/skill/SkillOverviewFooter.vue'
import SkillProgressBar from '@/skills-display/components/progress/skill/SkillProgressBar.vue'
import AchievementDate from '@/skills-display/components/skill/AchievementDate.vue'
import SkillBadgesAndTags from '@/skills-display/components/progress/skill/SkillBadgesAndTags.vue'
import { useSkillsDisplayAttributesState } from '@/skills-display/stores/UseSkillsDisplayAttributesState.js'
import CatalogImportStatus from '@/skills-display/components/progress/CatalogImportStatus.vue'
import PartialPointsAlert from '@/skills-display/components/skill/PartialPointsAlert.vue'
import SkillVideo from '@/skills-display/components/progress/SkillVideo.vue';
import dayjs from 'dayjs';

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
  }
})
const emit = defineEmits(['add-tag-filter', 'points-earned'])
const route = useRoute()
const skillsDisplayInfo = useSkillsDisplayInfo()
const attributes = useSkillsDisplayAttributesState()

const skillOverviewFooter = ref(null)

const buildToRoute = () => {
  if (!props.enableDrillDown || !props.skill.isSkillType) {
    return null
  }
  let name = 'skillDetails'
  const params = { skillId: props.skill.skillId, projectId: props.skill.projectId }
  if (route.params.subjectId) {
    params.subjectId = route.params.subjectId
  } else if (route.params.badgeId) {
    params.badgeId = route.params.badgeId
    name = (skillsDisplayInfo.isGlobalBadgePage.value) ? 'globalBadgeSkillDetails' : 'badgeSkillDetails'
  } else if (props.skill.crossProject && props.skill.projectId) {
    params.crossProjectId = props.skill.projectId
  }
  name = skillsDisplayInfo.getContextSpecificRouteName(name)
  return { name, params }
}
const toRoute = buildToRoute()

const addTagFilter = (tag) => {
  emit('add-tag-filter', tag)
}

const isSkillsGroupWithChildren = computed(() => {
  return props.skill?.isSkillsGroupType && props.skill?.children && props.skill?.children.length > 0
})
const childSkillsInternal = computed(() => {
  return isSkillsGroupWithChildren.value ? props.skill.children.map((item) => ({ ...item, childSkill: true })) : []
})
const isSkillLocked = computed(() => {
  let hasBadgeDependency = false;
  const sk = props.skill;
  if (sk.badgeDependencyInfo && sk.badgeDependencyInfo.length > 0) {
    if (sk.badgeDependencyInfo.find((item) => !item.achieved)) {
      hasBadgeDependency = true;
    }
  }
  return (sk.dependencyInfo && !sk.dependencyInfo.achieved) || hasBadgeDependency;
})
const pointsEarned = (pts) => {
  props.skill.mostRecentlyPerformedOn = dayjs()
  skillOverviewFooter.value.updateEarnedPoints({pointsEarned: pts})
  emit('points-earned', pts);
}

const isSkillComplete = computed(() => props.skill && props.skill.meta && props.skill.meta.complete)
</script>

<template>
  <div class="text-left" data-cy="skillProgress">
    <div v-if="skill.crossProject && !skillsDisplayInfo.isGlobalBadgePage.value" class="flex gap-3 flex-wrap">
      <div class="flex-1">
        <div class="text-xl"><span class="text-color-secondary font-italic">{{ attributes.projectDisplayName }}:</span> {{ skill.projectName }}</div>
      </div>
      <div class="">
        <div class="text-xl"><i class="fa fa-vector-square" aria-hidden="true"/> Cross-{{ attributes.projectDisplayName }} {{ attributes.skillDisplayName }}</div>
      </div>
    </div>
    <Message v-if="skill.crossProject && !isSkillComplete && !skillsDisplayInfo.isGlobalBadgePage.value"
             icon="fas fa-hands-helping"
             data-cy="crossProjAlert" :closable="false">
      This is a cross-{{ attributes.projectDisplayName.toLowerCase() }} {{ attributes.skillDisplayName.toLowerCase()
      }}! In order to complete
      this {{ attributes.skillDisplayName.toLowerCase() }} please visit <strong>{{
        skill.projectName
      }}</strong> {{ attributes.projectDisplayName.toLowerCase() }}! Happy playing!!
    </Message>


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
        <div class="relative">
          <skill-progress-bar data-cy="skillProgressBar"
                              :is-locked="isSkillLocked"
                              class="border-1 border-transparent hover:border-orange-700 border-round"
                              :skill="skill" />

        </div>
      </router-link>
      <skill-progress-bar
        v-else
        :skill="skill"
        :is-locked="isSkillLocked"
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
        <div v-if="isSkillLocked && skill.dependencyInfo" class="text-center text-muted locked-text">
          *** Skill has
          <Tag>{{ skill.dependencyInfo.numDirectDependents }}</Tag>
          direct prerequisite(s).
          <span v-if="enableDrillDown">Click <i class="fas fa-lock icon"></i> to see its prerequisites.</span>
          <span v-else>Please see its prerequisites below.</span>
          ***
        </div>
        <p v-if="skill.subjectName" class="text-secondary mt-3">
          {{ attributes.subjectDisplayName }}: {{ skill.subjectName }}
        </p>

        <achievement-date
          v-if="skill && skill.achievedOn"
          :date="skill.achievedOn" class="mt-2" />

        <partial-points-alert v-if="!enableDrillDown" :skill="skill" :is-locked="isSkillLocked" />
        <skills-summary-cards v-if="!isSkillLocked" :skill="skill" class="mt-3" />
        <catalog-import-status :skill="skill" />
        <skill-video v-if="skill" :skill="skill"
                     :video-collapsed-by-default="videoCollapsedByDefault"
                     @points-earned="pointsEarned"
                     class="mt-2" />
        <p class="skills-text-description text-primary mt-3" style="font-size: 0.9rem;">
          <markdown-text
            :instance-id="`skillDescription-${skill.skillId}`"
            v-if="skill.description && skill.description.description"
            :text="skill.description.description" />
        </p>

        <div>
          <skill-overview-footer ref="skillOverviewFooter" :skill="skill" />
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
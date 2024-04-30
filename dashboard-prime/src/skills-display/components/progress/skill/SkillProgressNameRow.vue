<script setup>
import { computed } from 'vue'
import dayjs from 'dayjs'
import { useNumberFormat } from '@/common-components/filter/UseNumberFormat.js'
import { useTimeUtils } from '@/common-components/utilities/UseTimeUtils.js'
import AnimatedNumber from '@/skills-display/components/utilities/AnimatedNumber.vue'
import { useAppConfig } from '@/common-components/stores/UseAppConfig.js'
import { useRoute } from 'vue-router'
import { useSkillsDisplayPreferencesState } from '@/skills-display/stores/UseSkillsDisplayPreferencesState.js'
import HighlightedValue from '@/components/utils/table/HighlightedValue.vue'

const props = defineProps({
  skill: Object,
  toRoute: Object,
  type: {
    type: String,
    default: 'subject'
  },
  childSkillHighlightString: {
    type: String,
    default: ''
  }
})
const numFormat = useNumberFormat()
const timeUtils = useTimeUtils()
const appConfig = useAppConfig()
const displayPref = useSkillsDisplayPreferencesState()
const route = useRoute()

const isSkillsGroupWithChildren = computed(() => props.skill?.isSkillsGroupType && props.skill?.children && props.skill?.children.length > 0)
const numSkillsRequired = computed(() => {
  if (isSkillsGroupWithChildren.value) {
    return props.skill.numSkillsRequired === -1 ? props.skill.children.length : props.skill.numSkillsRequired
  }
  return 0
})
const numChildSkillsComplete = computed(() => isSkillsGroupWithChildren.value ? props.skill.children.filter((childSkill) => childSkill.meta.complete).length : 0)
const showHasExpiredMessage = computed(() => {
  if (props.skill && props.skill.lastExpirationDate && props.skill.points === 0) {
    return true
  }
  return false
})
const showMotivationalExpirationMessage = computed(() => {
  if (props.skill && props.skill.achievedOn && props.skill.expirationDate && props.skill.isMotivationalSkill) {
    if (appConfig.motivationalSkillWarningGracePeriod) {
      const mostRecentlyPerformedOn = dayjs(props.skill.mostRecentlyPerformedOn)
      const now = dayjs()
      const daysSinceLastPerformed = now.diff(mostRecentlyPerformedOn, 'day')
      const gracePeriod = props.skill.daysOfInactivityBeforeExp * appConfig.motivationalSkillWarningGracePeriod
      return gracePeriod < 1 || daysSinceLastPerformed > gracePeriod
    }
    return true
  }
  return false
})
const isSkillComplete = computed(() => props.skill && props.skill.meta && props.skill.meta.complete)

const expirationDate = (includeTime = false) => {
  if (props.skill.expirationDate) {
    const exp = dayjs(props.skill.expirationDate)
    return includeTime ? exp.format() : exp.format('MMMM D YYYY')
  }
  return ''
}

const buildToRoute = () => {
  let name = 'skillDetails'
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

const someSkillsAreOptional = computed(() => {
  return isSkillsGroupWithChildren.value && props.skill.numSkillsRequired !== -1 && props.skill.numSkillsRequired < props.skill.children.length
})
</script>

<template>
  <div class="md:flex flex-wrap align-content-end"
       :data-cy="`skillProgressTitle-${skill.skillId}`"
       :id="`skillProgressTitle-${skill.skillId}`">
    <div class="skills-theme-primary-color flex-1 text-2xl w-min-12rem">
      <div class="py-1 md:flex">
        <div class="text-blue-700 font-medium flex">
          <div class="mr-1">
            <i  v-if="skill.isSkillsGroupType" class="fas fa-layer-group"></i>
            <i v-if="!skill.copiedFromProjectId && !skill.isSkillsGroupType"
               class="fas fa-graduation-cap text-color-secondary"></i>
            <i v-if="skill.copiedFromProjectId" class="fas fa-book text-secondary"></i>
          </div>
          <div class="">
            <router-link
              :id="`skillProgressTitleLink-${skill.skillId}`"
              v-if="toRoute"
              :to="toRoute"
              class="skill-link"
              data-cy="skillProgressTitle"
              :aria-label="`${skill.isSkillType ? `Navigate to ${skill.skill}` : skill.skill }`">
              <highlighted-value :value="skill.skill" :filter="childSkillHighlightString" />
            </router-link>
            <div v-else class="inline-block" data-cy="skillProgressTitle">
              <highlighted-value :value="skill.skill" :filter="childSkillHighlightString" />
            </div>
          </div>
        </div>
        <div v-if="skill.copiedFromProjectId" class="text-truncate d-inline-block ml-2"
             style="max-width: 15rem;"><span class="text-secondary font-italic"> in </span>
          <span class="font-italic">{{ skill.copiedFromProjectName }}</span>
        </div>

        <div
          v-if="skill.isSkillsGroupType && skill.numSkillsRequired > 0 && skill.numSkillsRequired < skill.children.length"
          :title="`A ${displayPref.groupDisplayName} allows a ${displayPref.skillDisplayName} to be defined by the collection ` +
                            `of other ${displayPref.skillDisplayName}s within a ${displayPref.projectDisplayName}. A ${displayPref.skillDisplayName} Group can require the completion of some or all of the included ${displayPref.skillDisplayName}s before the group be achieved.`"
          class="text-sm align-content-center ml-2"
          data-cy="groupSkillsRequiredBadge">
          <span class="">Requires </span>
          <Tag severity="success">{{ skill.numSkillsRequired }}</Tag>
          <span class="font-italic"> out of </span>
          <Tag severity="secondary">{{ skill.children.length }}</Tag>
          skills
        </div>

        <Tag v-if="skill.selfReporting && skill.selfReporting.enabled"
             class="self-report-badge ml-2">
          <i
            class="fas fa-user-check mr-1"></i><span class="sr-spelled-out mr-1">Self Reportable:</span>
          <span v-if="skill.selfReporting.type === 'Quiz'" data-cy="selfReportQuizTag"><span
            class="sr-spelled-out mr-1">Take </span>Quiz</span>
          <span v-if="skill.selfReporting.type === 'Survey'" data-cy="selfReportSurveyTag"><span
            class="sr-spelled-out mr-1">Complete </span>Survey</span>
          <span v-if="skill.selfReporting.type === 'HonorSystem'" data-cy="selfReportHonorSystemTag">Honor<span
            class="sr-spelled-out ml-1">System</span></span>
          <span v-if="skill.selfReporting.type === 'Approval'" data-cy="selfReportApprovalTag"><span
            class="sr-spelled-out mr-1">Request</span>Approval</span>
          <span v-if="skill.selfReporting.type === 'Video'" data-cy="selfReportApprovalTag"><span
            class="sr-spelled-out mr-1">Watch</span>Video</span>
        </Tag>
        <Tag v-if="skill.isLastViewed" id="lastViewedIndicator" data-cy="lastViewedIndicator" severity="info"
             size="small"
             class="ml-2 overflow-hidden">
          <i class="fas fa-eye mr-1"></i> Last Viewed
        </Tag>
      </div>
    </div>
    <div class="text-right align-content-end w-min-9rem flex"
         :class="{ 'text-green-500' : isSkillComplete }"
         data-cy="skillProgress-ptsOverProgressBard">
      <i class="fa fa-check mr-1 pb-1 align-content-end"
         v-if="isSkillComplete"
         :data-cy="`skillCompletedCheck-${skill.skillId}`"
         aria-hidden="true" />
      <span v-if="skill.isSkillsGroupType" class="align-content-end">
        <animated-number :num="numChildSkillsComplete" />
        / {{ numFormat.pretty(numSkillsRequired) }} Skill{{ (numSkillsRequired === 1) ? '' : 's' }}
        {{ someSkillsAreOptional ? 'Required' : '' }}
      </span>
      <span v-else class="align-content-end">
        <animated-number :num="skill.points" />
        / {{ numFormat.pretty(skill.totalPoints) }} Points
      </span>

      <div v-if="skill.points > 0 && expirationDate() && !skill.isMotivationalSkill" data-cy="expirationDate">
        <div class="my-2 text-orange-500">
          <i class="fas fa-hourglass-end text-orange-600 mr-2" arai-hidden="true"></i>Points will expire on <span
          class="font-semibold">{{ expirationDate() }}</span>
        </div>
      </div>
      <div v-if="showMotivationalExpirationMessage" data-cy="expirationDate" class="my-2">
        <div class="my-2 text-orange-500">
          Expires <span
          class="font-semibold">{{ timeUtils.relativeTime(expirationDate(true)) }}</span>,
          perform this skill to keep your points!
        </div>
      </div>
      <div v-if="showHasExpiredMessage" data-cy="hasExpired">
        <div class="my-2 text-orange-500">
          <i class="fas fa-clock skills-color-expiration mr-2"></i>Points expired <span
          class="font-weight-bold">{{ timeUtils.relativeTime(skill.lastExpirationDate) }}</span>
        </div>
      </div>

      <div v-if="skill.selfReporting && skill.selfReporting.requestedOn"
           data-cy="approvalPending">
        <span v-if="!skill.selfReporting.rejectedOn" class="text-orange-500"><i class="far fa-clock"
                                                                                aria-hidden="true" /> Pending Approval</span>
        <span v-else class="text-red-500"><i class="fas fa-heart-broken skills-theme-primary-color"
                                             aria-hidden="true"></i> Request Rejected</span>
      </div>
    </div>
  </div>
</template>

<style scoped>
.self-report-badge .sr-spelled-out {
  display: none;
}

.self-report-badge:hover .sr-spelled-out {
  display: inline-block;
}

.skill-link {
  text-decoration-thickness: 1px;
  text-decoration-color: gray;
}
</style>
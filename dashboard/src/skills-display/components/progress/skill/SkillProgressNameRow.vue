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
import { computed, ref } from 'vue'
import dayjs from 'dayjs'
import { useNumberFormat } from '@/common-components/filter/UseNumberFormat.js'
import { useTimeUtils } from '@/common-components/utilities/UseTimeUtils.js'
import AnimatedNumber from '@/skills-display/components/utilities/AnimatedNumber.vue'
import { useAppConfig } from '@/common-components/stores/UseAppConfig.js'
import { useRoute } from 'vue-router'
import HighlightedValue from '@/components/utils/table/HighlightedValue.vue'
import { useSkillsDisplayInfo } from '@/skills-display/UseSkillsDisplayInfo.js'
import { useScrollSkillsIntoViewState } from '@/skills-display/stores/UseScrollSkillsIntoViewState.js'
import { useSkillsDisplayAttributesState } from '@/skills-display/stores/UseSkillsDisplayAttributesState.js'
import {usePluralize} from "@/components/utils/misc/UsePluralize.js";

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
  },
  isExpanded: {
    type: Boolean,
    default: true
  }
})
const emit = defineEmits(['toggle-row']);
const numFormat = useNumberFormat()
const timeUtils = useTimeUtils()
const appConfig = useAppConfig()
const attributes = useSkillsDisplayAttributesState()
const pluralize = usePluralize()
const route = useRoute()
const skillDisplayInfo = useSkillsDisplayInfo()

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

const someSkillsAreOptional = computed(() => {
  return isSkillsGroupWithChildren.value && props.skill.numSkillsRequired !== -1 && props.skill.numSkillsRequired < props.skill.children.length
})

const scrollIntoViewState = useScrollSkillsIntoViewState()
const showLastViewedIndicator = computed(() => {
  if (skillDisplayInfo.isGlobalBadgePage.value) {
    return false
  }
  if (!props.toRoute) {
    return false
  }
  return props.skill.isLastViewed || props.skill.skillId === scrollIntoViewState.lastViewedSkillId
})

const skillId = computed(() => {
  let res = props.skill.skillId
  if (skillDisplayInfo.isGlobalBadgePage.value) {
    res += `=${route.params.badgeId}`
  }
  return res
})

const isAudio = computed(() => {
  return props.skill.videoSummary?.videoType?.includes('audio/')
})
const titleComponent = computed(() => route.params.skillId ? 'h2' : 'h3')
const groupLabel = computed(() => attributes.groupDisplayName)
</script>

<template>
  <div class="md:flex flex-wrap content-end"
       :data-cy="`skillProgressTitle-${skillId}`"
       :id="`skillProgressTitle-${skillId}`">
    <div class=" flex-1 text-2xl w-min-12rem">
      <div class="py-1 md:flex items-center">
        <div class="sd-theme-primary-color font-medium flex">
          <div class="rounded-border w-16 skill-icon-container text-primary text-center border mr-2">
            <i v-if="skill.isSkillsGroupType" class="skill-icon fas fa-layer-group"></i>
            <i v-if="!skill.copiedFromProjectId && !skill.isSkillsGroupType" class="skill-icon" :class="`${skill?.iconClass ? skill?.iconClass : 'fas fa-graduation-cap'}`"></i>
            <i v-if="skill.copiedFromProjectId" class="skill-icon text-secondary" :class="`${skill?.iconClass ? skill?.iconClass : 'fas fa-book'}`"></i>
          </div>
          <div class="flex items-center">
            <div v-if="skillDisplayInfo.isGlobalBadgePage.value">
              <span class="italic text-muted-color">{{ attributes.projectDisplayName }}:</span> {{ skill.projectName }}
            </div>
            <router-link
              :id="`skillProgressTitleLink-${skillId}`"
              v-if="toRoute"
              :to="toRoute"
              data-cy="skillProgressTitle"
              :aria-label="`${skill.isSkillType ? `Navigate to ${skill.skill}` : skill.skill }`">
              <component :is="titleComponent"><highlighted-value :value="skill.skill" :filter="childSkillHighlightString" /></component>
            </router-link>
            <component :is="titleComponent" v-else class="inline-block" data-cy="skillProgressTitle">
              <highlighted-value :value="skill.skill" :filter="childSkillHighlightString" />
            </component>
            <SkillsButton :icon="!isExpanded ? 'fas fa-plus' : 'fas fa-minus'"
                    v-if="skill.isSkillsGroupType"
                    outlined
                    :aria-label="!isExpanded ? `Expand ${groupLabel}` : `Collapse ${groupLabel}`"
                    style="padding: 0.3rem 0.3rem 0.3rem 0.3rem;"
                    class="ml-2"
                    :data-cy="`toggleGroup-${skillId}`"
                    @click="emit('toggle-row')">
            </SkillsButton>
          </div>
        </div>
        <div v-if="skill.copiedFromProjectId" class="ml-2"
             style="max-width: 15rem;"><span class="text-secondary italic"> in </span>
          <span class="italic" data-cy="importedFromProj">{{ skill.copiedFromProjectName }}</span>
        </div>

        <div
          v-if="skill.isSkillsGroupType && skill.numSkillsRequired > 0 && skill.numSkillsRequired < skill.children.length"
          :title="`A ${attributes.groupDisplayName} allows a ${attributes.skillDisplayName} to be defined by the collection ` +
                            `of other ${attributes.skillDisplayNamePlural} within a ${attributes.projectDisplayName}. A ${attributes.skillDisplayName} ${attributes.groupDisplayName} can require the completion of some or all of the included ${attributes.skillDisplayNamePlural} before the ${attributes.groupDisplayNameLower} be achieved.`"
          class="text-sm content-center ml-2"
          data-cy="groupSkillsRequiredBadge">
          <span class="mr-1">Requires </span>
          <Tag severity="success">{{ skill.numSkillsRequired }}</Tag>
          <span class="italic mx-1"> out of </span>
          <Tag severity="secondary">{{ skill.children.length }}</Tag>
          skills
        </div>

        <Tag v-if="skill.selfReporting && skill.selfReporting.enabled"
             class="self-report-badge ml-2 max-h-8 place-content-center">
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
          <span v-if="skill.selfReporting.type === 'Video'" data-cy="selfReportApprovalTag">
            <span v-if="isAudio"><span class="sr-spelled-out mr-1">Listen to </span>Audio</span>
            <span v-else><span class="sr-spelled-out mr-1">Watch </span>Video</span>
          </span>
        </Tag>
        <Tag v-if="showLastViewedIndicator" id="lastViewedIndicator"
             data-cy="lastViewedIndicator" severity="info"
             class="ml-2 overflow-hidden max-h-8">
          <i class="fas fa-eye mr-1"></i> Last Viewed
        </Tag>
      </div>
    </div>
    <div class="text-right justify-end flex flex-col"
         :class="{ 'text-green-700 dark:text-green-400' : isSkillComplete }"
         data-cy="skillProgress-ptsOverProgressBard">

      <div>
        <i class="fa fa-check mr-1 pb-1"
           v-if="isSkillComplete"
           :data-cy="`skillCompletedCheck-${skillId}`"
           aria-hidden="true" />
        <span v-if="skill.isSkillsGroupType">
          <animated-number :num="numChildSkillsComplete" />
          / {{ numFormat.pretty(numSkillsRequired) }} {{ pluralize.plural(attributes.skillDisplayName, numSkillsRequired) }}
          {{ someSkillsAreOptional ? 'Required' : '' }}
        </span>
        <span v-else class="content-end">
          <animated-number :num="skill.points" />
          / {{ numFormat.pretty(skill.totalPoints) }} {{ pluralize.plural(attributes.pointDisplayName, skill.totalPoints) }}
        </span>
      </div>

      <div v-if="skill.points > 0 && expirationDate() && !skill.isMotivationalSkill" data-cy="expirationDate">
        <div class="my-2 text-orange-500">
          <i class="fas fa-hourglass-end text-orange-600 mr-2" aria-hidden="true"></i>{{ attributes.pointDisplayNamePlural }} will expire on <span
          class="font-semibold">{{ expirationDate() }}</span>
        </div>
      </div>
      <div v-if="showMotivationalExpirationMessage" data-cy="expirationDate" class="my-2">
        <div class="my-2 text-orange-500">
          Expires <span
          class="font-semibold">{{ timeUtils.relativeTime(expirationDate(true)) }}</span>,
          perform this {{ attributes.skillDisplayNameLower }} to keep your {{ attributes.pointDisplayNamePlural.toLowerCase() }}!
        </div>
      </div>
      <div v-if="showHasExpiredMessage" data-cy="hasExpired">
        <div class="my-2 text-orange-500">
          <i class="fas fa-clock skills-color-expiration mr-2"></i>{{ attributes.pointDisplayNamePlural }} expired <span
          class="font-weight-bold">{{ timeUtils.relativeTime(skill.lastExpirationDate) }}</span>
        </div>
      </div>

      <div v-if="skill.selfReporting && skill.selfReporting.requestedOn && !skill.selfReporting.approved" data-cy="approvalPending">
        <span v-if="!skill.selfReporting.rejectedOn" class="text-orange-500">
          <i class="far fa-clock" aria-hidden="true" /> Pending Approval
        </span>
        <span v-else-if="skill.selfReporting.rejectedOn" class="text-red-500">
          <i class="fas fa-heart-broken skills-theme-primary-color" aria-hidden="true"></i> Request Rejected
        </span>
      </div>

      <div v-if="skill.selfReporting && skill.selfReporting.quizNeedsGrading" class="text-orange-500" data-cy="requiresGrading">
        <i class="fas fa-user-clock" aria-hidden="true"/> Awaiting Grading
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

.skill-icon {
  height: 100% !important;
  width: 100% !important;
  background-size: cover;
  background-position: center;
  font-size: 30px !important;
  line-height: 46px !important;
}

.skill-icon-container {
  max-width:48px;
  max-height:48px;
  height:48px;
  width: 48px;
}
</style>
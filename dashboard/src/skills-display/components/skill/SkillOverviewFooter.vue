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
import { computed, nextTick, ref } from 'vue'
import { useNumberFormat } from '@/common-components/filter/UseNumberFormat.js'
import { useTimeUtils } from '@/common-components/utilities/UseTimeUtils.js'
import { useSkillsDisplayInfo } from '@/skills-display/UseSkillsDisplayInfo.js'
import { useSkillsDisplayService } from '@/skills-display/services/UseSkillsDisplayService.js'
import { useSkillsDisplaySubjectState } from '@/skills-display/stores/UseSkillsDisplaySubjectState.js'
import JustificationInput from '@/skills-display/components/skill/JustificationInput.vue'
import { useSkillsDisplayAttributesState } from '@/skills-display/stores/UseSkillsDisplayAttributesState.js'
import { useLog } from '@/components/utils/misc/useLog.js'
import QuizFooter from "@/skills-display/components/skill/QuizFooter.vue";
import ApprovalHistory from '@/skills-display/components/skill/ApprovalHistory.vue';
import QuizType from '@/skills-display/components/quiz/QuizType.js';
import { useSelfReportHelper } from '@/skills-display/UseSelfReportHelper.js';

const props = defineProps({
  skill: Object
})

const numFormat = useNumberFormat()
const timeUtils = useTimeUtils()
const skillsDisplayInfo = useSkillsDisplayInfo()
const skillsDisplayService = useSkillsDisplayService()
const attributes = useSkillsDisplayAttributesState()
const skillState = useSkillsDisplaySubjectState()
const selfReportHelper = useSelfReportHelper()
const log = useLog()

const selfReport = ref({
  res: null,
  msgHidden: true
})
const selfReportAvailable = computed(() => selfReportConfigured() && (!isCompleted.value || isMotivationalSkill.value) && !isLocked() && !isCrossProject())
const skillInternal = computed(() => props.skill)
const isPointsEarned = computed(() => selfReport.value && selfReport.value.res && selfReport.value.res.skillApplied)
const isCompleted = computed(() => skillInternal.value.points === skillInternal.value.totalPoints)
const selfReportDisabled = computed(() => (isCompleted.value && !isMotivationalSkill.value) || isPendingApproval())
const isHonorSystem = computed(() => skillInternal.value.selfReporting && skillInternal.value.selfReporting.type === 'HonorSystem')
const isApprovalRequired = computed(() => skillInternal.value.selfReporting && skillInternal.value.selfReporting.type === 'Approval')
const isQuizSkill = computed(() => skillInternal.value.selfReporting && QuizType.isQuiz(skillInternal.value.selfReporting.type))
const isVideo = computed(() => skillInternal.value.selfReporting.type === 'Video')
const isJustificationRequired = computed(() => skillInternal.value.selfReporting && skillInternal.value.selfReporting.justificationRequired)
const isRejected = computed(() => skillInternal.value.selfReporting && skillInternal.value.selfReporting.rejectedOn !== null && skillInternal.value.selfReporting.rejectedOn !== undefined)
const isMotivationalSkill = computed(() => skillInternal.value && skillInternal.value.isMotivationalSkill)
const showTimeline = computed(() => {
  if (skillInternal.value.approvalHistory && skillInternal.value.approvalHistory.length > 0) {
    if (isApprovalRequired.value) {
      return true;
    }
    if (isQuizSkill.value) {
      return skillInternal.value.approvalHistory.length > 1
          || (skillInternal.value.approvalHistory.length === 1 &&
              selfReportHelper.isFailed(skillInternal.value.approvalHistory[0].eventStatus));
    }
  }
  return false
})

const showApprovalJustification = ref(false)
const requestApprovalLoading = ref(false)
const removeRejectionLoading = ref(false)
const rejectionDialogYOffset = ref(0)
const approvalRequestedMsg = ref('')

const justificationInput = ref(null)
const displayApprovalJustificationInput = () => {
  showApprovalJustification.value = true
  // nextTick(() => justificationInput.value.focusOnMarkdownEditor())
}

const isPendingApproval = () => {
  return skillInternal.value.selfReporting && skillInternal.value.selfReporting.requestedOn !== null && skillInternal.value.selfReporting.requestedOn !== undefined && !isRejected.value
}
const selfReportConfigured = () => {
  return skillInternal.value.selfReporting && skillInternal.value.selfReporting && skillInternal.value.selfReporting.enabled
}
const isLocked = () => {
  return skillInternal.value.dependencyInfo && !skillInternal.value.dependencyInfo.achieved
}
const isCrossProject = () => {
  return skillInternal.value.crossProject
}
const isAlreadyPerformed = () => {
  return selfReport.value.res && selfReport.value.res.explanation.includes('was already performed')
}
const removeRejection = () => {
  removeRejectionLoading.value = true
  skillsDisplayService.removeApprovalRejection(skillInternal.value.selfReporting.approvalId)
    .then(() => {
      skillInternal.value.selfReporting.rejectedOn = null
      skillInternal.value.selfReporting.rejectedMsg = null
      skillInternal.value.selfReporting.requestedOn = null
    }).finally(() => {
    removeRejectionLoading.value = false
  })
}
const firstReport = ref(skillInternal?.value?.points === 0);
const isRetention = ref(false);
const errNotification = ref({
  enable: false,
  msg: ''
})
const reportSkill = (approvalRequestedMsg) => {
  errNotification.value.enable = false
  errNotification.value.msg = ''

  requestApprovalLoading.value = true

  firstReport.value = skillInternal.value.points === 0;
  isRetention.value = skillInternal.value.points === skillInternal.value.totalPoints
  // selfReport.value.msgHidden = true
  // selfReport.value.res = null
  skillsDisplayService.reportSkill(skillInternal.value.skillId, approvalRequestedMsg)
    .then((res) => {
      if (res.explanation.includes('This skill was already submitted for approval and is still pending approval')
        || res.explanation.includes('This skill reached its maximum points')) {
        errNotification.value.msg = `${res.explanation}. Please refresh the page to update the status.`
        errNotification.value.enable = true
      } else {
        if (skillInternal.value.selfReporting) {
          skillInternal.value.selfReporting.rejectedOn = null
          skillInternal.value.selfReporting.message = null
        }

        selfReport.value.msgHidden = false
        selfReport.value.res = res
        if (!isAlreadyPerformed() && isApprovalRequired.value) {
          const requestedOn = new Date()
          skillInternal.value.selfReporting.requestedOn = requestedOn
          if (skillInternal.value.approvalHistory) {
            skillInternal.value.approvalHistory.unshift({
              id: '-1',
              eventTime : requestedOn.getTime(),
              eventStatus: 'Approval Requested',
              description: approvalRequestedMsg
            })
          }
        }
        updateEarnedPoints(res)
        if (res.explanation.includes('Skill Achievement retained')) {
          // do not show skill expiration warnings after points were just earned
          skillState.nullifyExpirationDate(skillInternal.value.skillId)
        }
      }
    }).catch((e) => {
    if (e.response.data && e.response.data.errorCode
      && (e.response.data.errorCode === 'InsufficientProjectPoints' || e.response.data.errorCode === 'InsufficientSubjectPoints')) {
      errNotification.value.msg = e.response.data.explanation
      errNotification.value.enable = true
    } else {
      const errorMessage = (e.response && e.response.data && e.response.data.explanation) ? e.response.data.explanation : undefined
      skillsDisplayInfo.routerPush(
        'error',
        {
          errorMessage
        })
    }
  }).finally(() => {
    requestApprovalLoading.value = false
  })
}
// testWasTaken(testResult) {
//   const { gradedRes } = testResult;
//   if (gradedRes && gradedRes.passed && gradedRes.associatedSkillResults) {
//     const skill = gradedRes.associatedSkillResults.find((e) => e.projectId ===  skillInternal.value.projectId && e.skillId ===  skillInternal.value.skillId);
//     this.updateEarnedPoints(skill);
//   }
// },
const updateEarnedPoints = (res) => {
  if (res.pointsEarned > 0 || isMotivationalSkill.value) {
    log.trace(`Skill ${skillInternal.value.skillId} earned ${res.pointsEarned} points`)
    skillState.addPoints(skillInternal.value.skillId, res.pointsEarned)
  }
}
const focusOnId = (elementId) => {
  nextTick(() => {
    const element = document.getElementById(elementId)
    element.focus()
  })
}

defineExpose({
  updateEarnedPoints
})
</script>

<template>
  <div>
    <quiz-footer :skill="skillInternal"/>
    <Message v-if="isHonorSystem && selfReportAvailable && !isCompleted" class="mb-2 alert alert-info">
      <template #container>
        <div class="flex gap-2 p-4 content-center">
          <div>
            <i class="fas fa-user-shield text-2xl" aria-hidden="true"></i>
          </div>
          <div class="flex-1 italic pt-1" data-cy="honorSystemAlert">
            This skill can be submitted under the <span class="font-size-1">Honor System</span>, claim <span class="font-size-1">
            <Tag severity="info">{{ numFormat.pretty(skillInternal.pointIncrement) }}</Tag></span> points once you've completed the skill.
          </div>
          <div class="col-auto">
            <SkillsButton
              label="Claim Points"
              icon="fas fa-check-double"
              class="skills-theme-btn"
              :disabled="selfReportDisabled"
              severity="info"
              outlined
              size="small"
              :loading="requestApprovalLoading"
              @click="reportSkill(null)"

              data-cy="claimPointsBtn" />
          </div>
        </div>
      </template>
    </Message>
    <Message v-else-if="isHonorSystem && selfReportAvailable && !firstReport && isMotivationalSkill && skillInternal.expirationDate">
      <template #container>
        <div class="flex gap-2 p-4 content-center">
          <div>
            <i class="fas fa-user-shield text-2xl" aria-hidden="true"></i>
          </div>
          <div class="flex-1 italic pt-1" data-cy="honorSystemAlert">
            This skill's achievement expires <span class="font-semibold">{{ timeUtils.relativeTime(skillInternal.expirationDate) }}</span>, but your <span class="font-size-1">
            <Tag severity="info">{{ numFormat.pretty(skillInternal.totalPoints) }}</Tag></span> points can be retained by performing another <span class="font-size-1">Honor System</span> request.
          </div>
          <div class="col-auto">
            <SkillsButton
                label="Claim Points"
                icon="fas fa-check-double"
                class="skills-theme-btn"
                :disabled="selfReportDisabled"
                severity="info"
                outlined
                size="small"
                :loading="requestApprovalLoading"
                @click="reportSkill(null)"
                data-cy="claimPointsBtn" />
          </div>
        </div>
      </template>
    </Message>
    <Message :closable="false"
             v-if="(firstReport || !isCompleted) && isApprovalRequired && selfReportAvailable && !selfReportDisabled && !isRejected"
             class="mb-2">
      <template #container>
        <div class="p-4">
          <div class="flex gap-2 sm:items-center flex-col sm:flex-row">
            <div>
              <i class="fas fa-traffic-light text-2xl" aria-hidden="true"></i>
            </div>
            <div class="flex-1" data-cy="requestApprovalAlert">
              This skill requires <span class="font-size-1">approval</span>.
              Request <span class="font-size-1"><Tag severity="info">{{ numFormat.pretty(skillInternal.pointIncrement)
              }}</Tag></span> points once you've completed the skill.
            </div>
            <div class="">
              <SkillsButton
                label="Begin Request"
                icon="far fa-arrow-alt-circle-right"
                v-if="!showApprovalJustification"
                id="beginRequestBtn"
                class="skills-theme-btn"
                :disabled="selfReportDisabled"
                severity="info"
                outlined
                size="small"
                @click="displayApprovalJustificationInput"
                data-cy="requestApprovalBtn" />
            </div>
          </div>
          <BlockUI :blocked="requestApprovalLoading">
            <justification-input v-if="showApprovalJustification"
                                 ref="justificationInput"
                                 class="mt-4"
                                 @report-skill="reportSkill"
                                 @cancel="showApprovalJustification = false; focusOnId('beginRequestBtn')"
                                 :skill="skillInternal"
                                 :is-approval-required="isApprovalRequired"
                                 :is-honor-system="isHonorSystem"
                                 :is-justitification-required="isJustificationRequired" />
          </BlockUI>
        </div>
      </template>
    </Message>
    <Message v-else-if="(!firstReport || isCompleted) && isApprovalRequired && selfReportAvailable && !selfReportDisabled && !isRejected && isMotivationalSkill">
      <template #container>
        <div class="p-4">
          <div class="flex gap-2 sm:items-center flex-col sm:flex-row">
            <div>
              <i class="fas fa-traffic-light text-2xl" aria-hidden="true"></i>
            </div>
            <div class="flex-1" data-cy="requestApprovalAlert">
              This skill's achievement expires <span class="font-semibold">{{ timeUtils.relativeTime(skillInternal.expirationDate) }}</span>, but your <span class="font-size-1">
              <Tag severity="info">{{ numFormat.pretty(skillInternal.totalPoints) }}</Tag></span> points can be retained by submitting another <span class="font-size-1">approval</span> request.
            </div>
            <div class="">
              <SkillsButton
                  label="Begin Request"
                  icon="far fa-arrow-alt-circle-right"
                  v-if="!showApprovalJustification"
                  id="beginRequestBtn"
                  class="skills-theme-btn"
                  :disabled="selfReportDisabled"
                  severity="info"
                  outlined
                  size="small"
                  @click="displayApprovalJustificationInput"
                  data-cy="requestApprovalBtn" />
            </div>
          </div>
          <BlockUI :blocked="requestApprovalLoading">
            <justification-input v-if="showApprovalJustification"
                                 ref="justificationInput"
                                 class="mt-4"
                                 @report-skill="reportSkill"
                                 @cancel="showApprovalJustification = false; focusOnId('beginRequestBtn')"
                                 :skill="skillInternal"
                                 :is-approval-required="isApprovalRequired"
                                 :is-honor-system="isHonorSystem"
                                 :is-justitification-required="isJustificationRequired" />
          </BlockUI>
        </div>
      </template>
    </Message>

    <Message :closable="false"
             icon="far fa-clock"
             severity="warn"
             v-if="isPendingApproval() && !showTimeline && selfReport.msgHidden" class="mb-2 alert alert-info italic"
             data-cy="pendingApprovalStatus">
      This skill is <span class="font-size-1 normal-font">pending approval</span>.
      Submitted {{ timeUtils.relativeTime(skillInternal.selfReporting.requestedOn) }}
    </Message>
    <div v-if="isRejected" class="alert alert-danger mt-2" data-cy="selfReportRejectedAlert">
      <BlockUI :blocked="removeRejectionLoading">
        <Message severity="error">
          <template #container>
            <div class="flex p-4 content-center">
              <div class="flex-1 content-center">
                <i class="fas fa-heart-broken text-xl" aria-hidden="true"></i>
                Unfortunately your request from
                <b>{{ timeUtils.formatDate(skillInternal.selfReporting.requestedOn, 'MM/DD/YYYY') }}</b> was rejected
                <span
                  class="text-info">{{ timeUtils.relativeTime(skillInternal.selfReporting.rejectedOn) }}</span>.
                <span v-if="skillInternal.selfReporting.message">The reason is: <b>"{{ skillInternal.selfReporting.message}}"</b></span>
              </div>
              <div class="">
                <SkillsButton
                  label="I got it!"
                  icon="fas fa-check"
                  outlined
                  size="small"
                  data-cy="clearRejectionMsgBtn"
                  @click="removeRejection" />
              </div>
            </div>
          </template>
        </Message>
      </BlockUI>
    </div>

    <Message v-if="errNotification.enable" class="mt-2" data-cy="selfReportError" severity="error">
      {{ errNotification.msg }}
    </Message>
    <div v-if="!selfReport.msgHidden" class="alert alert-success mt-2" role="alert" data-cy="selfReportAlert">
      <div class="">
        <div class="">
          <Message
            icon="fas fa-birthday-cake"
            @close="selfReport.res = null"
            v-if="isPointsEarned && (!isMotivationalSkill || !isRetention)"
            severity="success">
            Congrats! You just earned
            <Tag>{{ selfReport.res.pointsEarned }}</Tag>
            points<span
            v-if="isCompleted"> and <b>completed</b> the {{ attributes.skillDisplayName.toLowerCase() }}</span>!
          </Message>
          <Message v-if="isPointsEarned && isMotivationalSkill && !firstReport && isRetention" severity="success" icon="fas fa-birthday-cake">
            Congratulations! You just retained your <Tag>{{ skillInternal.totalPoints }}</Tag> points!
          </Message>
          <Message
            v-if="selfReport.res && !isPointsEarned && (isAlreadyPerformed() || !isApprovalRequired)"
            severity="warn"
            @close="selfReport.res = null"
            icon="fas fa-cloud-sun-rain">
            <span><b>Unfortunately</b> no points.</span>
            {{ selfReport.res?.explanation }}
          </Message>

          <Message
            icon="fas fa-user-clock"
            severity="success"
            @close="selfReport.msgHidden = true"
            v-if="!isAlreadyPerformed() && isApprovalRequired">
            <div>
              <b>Submitted successfully!</b>
              This {{ attributes.skillDisplayName.toLowerCase() }} <b class="text-info">requires approval</b> from a
              {{ attributes.projectDisplayName.toLowerCase() }} administrator. Now let's play the waiting game!
            </div>
          </Message>
        </div>
      </div>
    </div>

    <ApprovalHistory v-if="showTimeline" :events="skillInternal.approvalHistory" />

    <div class=" pt-2">
        <div class="btn-group" role="group" aria-label="Skills Buttons">
          <a v-if="skillInternal.description && skillInternal.description.href" :href="skillInternal.description.href"
             target="_blank" rel="noopener" class="" tabindex="-1">
            <Button outlined size="small">
              <i class="fas fa-question-circle mr-1" aria-hidden="true"></i>
              Learn More
              <i class="fas fa-external-link-alt ml-1" aria-hidden="true"></i>
            </Button>
          </a>
        </div>
    </div>
  </div>
</template>

<style scoped>

</style>
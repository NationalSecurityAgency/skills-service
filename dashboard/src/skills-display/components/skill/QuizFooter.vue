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

import {ref, computed} from "vue";
import QuizType from "@/skills-display/components/quiz/QuizType.js";
import {useNumberFormat} from "@/common-components/filter/UseNumberFormat.js";
import {useSkillsDisplayInfo} from "@/skills-display/UseSkillsDisplayInfo.js";
import {useTimeUtils} from "@/common-components/utilities/UseTimeUtils.js";
import QuizRunService from "@/skills-display/components/quiz/QuizRunService.js";
import SkillsSpinner from "@/components/utils/SkillsSpinner.vue";
import QuizSingleRun from "@/components/quiz/runsHistory/QuizSingleRun.vue";
import {useRoute} from "vue-router";
import {usePluralize} from "@/components/utils/misc/UsePluralize.js";
import {useSkillsDisplayAttributesState} from "@/skills-display/stores/UseSkillsDisplayAttributesState.js";
import QuizCompletedMessage from "@/skills-display/components/quiz/QuizCompletedMessage.vue";

const props = defineProps({
  skill: Object,
})

const numFormat = useNumberFormat()
const skillsDisplayInfo = useSkillsDisplayInfo()
const timeUtils = useTimeUtils()
const route = useRoute()
const pluralize = usePluralize()
const attributes = useSkillsDisplayAttributesState()

const selfReporting = computed(() => props.skill.selfReporting)
const isQuizSkill = computed(() => selfReporting.value && QuizType.isQuiz(selfReporting.value.type))
const isSurveySkill = computed(() => selfReporting.value && QuizType.isSurvey(selfReporting.value.type))
const isQuizOrSurveySkill = computed(() => isQuizSkill.value || isSurveySkill.value)
const isQuizPendingGrading = computed(() => isQuizOrSurveySkill.value && selfReporting.value?.quizNeedsGrading)

const completionWord = computed(() => isSurveySkill.value ? 'Complete' : 'Pass')
const completionWordInThePast = computed(() => isSurveySkill.value ? 'completed' : 'passed')
const typeWord = computed(() => isSurveySkill.value ? 'Survey' : 'Quiz')
const hasQuestions = computed(() => selfReporting.value.numQuizQuestions && selfReporting.value.numQuizQuestions > 0)
const isCompleted = computed(() => props.skill && props.skill.points === props.skill.totalPoints)
const isMotivationalSkill = computed(() => props.skill && props.skill.isMotivationalSkill)
const selfReportDisabled = computed(() => isCompleted.value && !isMotivationalSkill.value)
const isLocked = computed(() => {
  const lockedDueToSkillPrerequisites = props.skill.dependencyInfo && !props.skill.dependencyInfo.achieved
  const lockedDueToBadgePrerequisites = props.skill.badgeDependencyInfo && props.skill.badgeDependencyInfo.length > 0 && !props.skill.badgeDependencyInfo.find((item) => item.achieved)
  return lockedDueToSkillPrerequisites || lockedDueToBadgePrerequisites
})
const isCrossProject = computed(() => props.skill.crossProject)
const selfReportAvailable = computed(() => isQuizOrSurveySkill?.value && (!isCompleted.value || isMotivationalSkill.value) && !isLocked.value)
const quizOrSurveyPassed = computed(() => selfReporting.value?.quizOrSurveyPassed)
const subjectId = computed(() => props.skill.subjectId || route.params.subjectId)

const navToQuiz = () => {
  if (props.skill.crossProject) {
    if (route.params.badgeId) {
      // global badge
      skillsDisplayInfo.routerPush('quizPageForGlobalBadgeCrossProjectSkill',
          {
            crossProjectId: props.skill.projectId,
            skillId: props.skill.skillId,
            quizId: props.skill.selfReporting.quizId,
            badgeId: route.params.badgeId,
          }
      )
    } else {
      // learning path cross-project skill
      skillsDisplayInfo.routerPush('quizPageForLearningPathCrossProjectSkill',
          {
            crossProjectId: props.skill.projectId,
            subjectId: subjectId.value,
            skillId: props.skill.skillId,
            quizId: props.skill.selfReporting.quizId,
          }
      )
    }
  } else {
    skillsDisplayInfo.routerPush('quizPage',
        {
          subjectId: subjectId.value,
          skillId: props.skill.skillId,
          quizId: props.skill.selfReporting.quizId,
        }
    )
  }
}

const showQuizResults = ref(false)
const loadingAttempt = ref(true)
const lastQuizAttempt = ref(null)
const loadQuizAttempt = () => {
  showQuizResults.value = !showQuizResults.value
  if (!lastQuizAttempt.value) {
    loadingAttempt.value = true
    QuizRunService.getSingleQuizAttempt(selfReporting.value.quizAttemptId).then((res) => {
      lastQuizAttempt.value = res
    }).finally(() => {
      loadingAttempt.value = false
    })
  }
}
const viewResultsBtnLabel = computed(() => {
  return `${showQuizResults.value ? 'Hide' : 'View'} ${typeWord.value} Results`
})
const pointsLabelWithTotalPts = computed(() => pluralize.plural(attributes.pointDisplayName, props.skill.totalPoints).toLowerCase())
</script>

<template>
  <div v-if="skill">
    <div v-if="selfReportAvailable && isQuizOrSurveySkill" class="mb-2">
      <Message v-if="!isQuizPendingGrading" :closable="false">
        <template #container>
          <div class="p-4">
            <div class="flex gap-2 items-center" data-cy="takeQuizMsg">
              <div>
                <i class="fas fa-user-check text-2xl" aria-hidden="true"></i>
              </div>
              <div class="flex-1" data-cy="quizAlert" v-if="!isCompleted">{{ completionWord }} the<span
                  v-if="hasQuestions">&nbsp;{{ selfReporting.numQuizQuestions }}-question</span>&nbsp;<b>{{
                  selfReporting.quizName
                }}</b>&nbsp;{{ typeWord }} and earn <span class="font-size-1"><Tag
                  severity="info">{{ numFormat.pretty(skill.totalPoints) }}</Tag></span> {{ pointsLabelWithTotalPts }}!
              </div>
              <div class="flex-1" data-cy="quizAlert" v-else-if="isCompleted && isMotivationalSkill">
                This {{ attributes.skillDisplayNameLower }}'s achievement expires <span class="font-semibold">{{ timeUtils.relativeTime(skill.expirationDate) }}</span>, but your <span class="font-size-1">
                <Tag severity="info">{{ numFormat.pretty(skill.totalPoints) }}</Tag></span> {{  pointsLabelWithTotalPts }} can be retained by completing the {{ typeWord }} again.
              </div>
              <SkillsButton
                  :label="isQuizSkill ? 'Take Quiz' : 'Complete Survey'"
                  icon="far fa-arrow-alt-circle-right"
                  v-if="isQuizOrSurveySkill"
                  class="skills-theme-btn"
                  :disabled="selfReportDisabled"
                  severity="info"
                  outlined
                  size="small"
                  @click="navToQuiz"
                  data-cy="takeQuizBtn"/>
            </div>
          </div>
        </template>
      </Message>
      <quiz-completed-message v-if="isQuizPendingGrading" :attempt-timestamp="selfReporting.quizNeedsGradingAttemptDate" />
    </div>
    <Message v-if="isCompleted && isQuizOrSurveySkill && quizOrSurveyPassed" :closable="false" severity="success" data-cy="quizCompletedMsg">
      <template #container>
        <div class="flex flex-col md:flex-row gap-2 p-4 items-center">
          <div>
            <i class="far fa-smile text-2xl" aria-hidden="true"></i>
          </div>
          <div class="flex-1">
            Congratulations! You have {{ completionWordInThePast }} <b>{{selfReporting.quizName }}</b>&nbsp;{{ typeWord }}.
          </div>
          <div>
            <SkillsButton
                :label="viewResultsBtnLabel"
                :icon="showQuizResults ? 'far fa-eye-slash' : 'far fa-eye'"
                class="skills-theme-btn"
                severity="info"
                size="small"
                @click="loadQuizAttempt"
                data-cy="viewQuizAttemptInfo"/>
          </div>
        </div>
        <div v-if="showQuizResults" class="border-t">
          <skills-spinner v-if="loadingAttempt" :is-loading="true"/>
          <div v-if="!loadingAttempt" class="pl-12 pr-8 pb-6 sd-theme-tile-background">
            <quiz-single-run  :run-info="lastQuizAttempt"  :show-cards="false" />
          </div>
        </div>
      </template>
    </Message>
  </div>
</template>

<style scoped>

</style>

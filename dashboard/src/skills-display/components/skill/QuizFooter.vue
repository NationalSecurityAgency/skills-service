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

import {ref, computed, onMounted} from "vue";
import QuizType from "@/skills-display/components/quiz/QuizType.js";
import {useNumberFormat} from "@/common-components/filter/UseNumberFormat.js";
import {useSkillsDisplayInfo} from "@/skills-display/UseSkillsDisplayInfo.js";
import {useTimeUtils} from "@/common-components/utilities/UseTimeUtils.js";
import QuizRunService from "@/skills-display/components/quiz/QuizRunService.js";
import SkillsSpinner from "@/components/utils/SkillsSpinner.vue";
import QuizStatus from "@/components/quiz/runsHistory/QuizStatus.js";
import QuizSingleRun from "@/components/quiz/runsHistory/QuizSingleRun.vue";
import {viewDepthKey} from "vue-router";
import SkillsService from "@/components/skills/SkillsService.js";

const props = defineProps({
  skill: Object,
})

const numFormat = useNumberFormat()
const skillsDisplayInfo = useSkillsDisplayInfo()
const timeUtils = useTimeUtils()

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
const selfReportAvailable = computed(() => isQuizOrSurveySkill?.value && (!isCompleted.value || isMotivationalSkill.value) && !isLocked.value && !isCrossProject.value)
const quizOrSurveyPassed = computed(() => selfReporting.value?.quizOrSurveyPassed)

const loading = ref(true)
const subjectId = ref(props.skill.subjectId)
const loadSubjectId = () => {
  loading.value = true
  if (props.skill.subjectId) {
    subjectId.value = props.skill.subjectId
    loading.value = false
  } else {
    SkillsService.getSkillInfo(props.skill.projectId, props.skill.skillId)
        .then((res) => {
          subjectId.value = res.subjectId
          loading.value = false
        })
  }
}

onMounted(() => {
  loadSubjectId()
})

const navToQuiz = () => {
  skillsDisplayInfo.routerPush('quizPage',
      {
        subjectId: subjectId.value,
        skillId: props.skill.skillId,
        quizId: props.skill.selfReporting.quizId,
      }
  )
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
</script>

<template>
  <div v-if="skill">
    <skills-spinner v-if="loading" :is-loading="loading" />
    <div v-if="!loading">
      <div v-if="selfReportAvailable && isQuizOrSurveySkill" class="mb-2">
        <Message :closable="false">
          <template #container>
            <div class="p-3">
              <div v-if="!isQuizPendingGrading" class="flex gap-2 align-items-center" data-cy="takeQuizMsg">
                <div>
                  <i class="fas fa-user-check text-2xl" aria-hidden="true"></i>
                </div>
                <div class="flex-1" data-cy="quizAlert" v-if="!isCompleted">{{ completionWord }} the<span
                    v-if="hasQuestions">&nbsp;{{ selfReporting.numQuizQuestions }}-question</span>&nbsp;<b>{{
                    selfReporting.quizName
                  }}</b>&nbsp;{{ typeWord }} and earn <span class="font-size-1"><Tag
                    severity="info">{{ numFormat.pretty(skill.totalPoints) }}</Tag></span> points!
                </div>
                <div class="flex-1" data-cy="quizAlert" v-else-if="isCompleted && isMotivationalSkill">
                  This skill's achievement expires <span class="font-semibold">{{ timeUtils.relativeTime(skill.expirationDate) }}</span>, but your <span class="font-size-1">
                  <Tag severity="info">{{ numFormat.pretty(skill.totalPoints) }}</Tag></span> points can be retained by completing the {{ typeWord }} again.
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
              <div v-if="isQuizPendingGrading" class="flex gap-2 align-items-center" data-cy="quizRequiresGradingMsg">
                <div>
                  <i class="fas fa-user-clock text-2xl" aria-hidden="true"/>
                </div>
                <div>
                  <div>You completed the quiz on
                    <Tag>{{ timeUtils.formatDate(selfReporting.quizNeedsGradingAttemptDate) }}</Tag>
                    but it <b>requires grading</b>.
                  </div>
                  <div class="mt-3">It will be assessed by a quiz administrator, so there is nothing to do but wait for
                    the
                    grades to roll in!
                  </div>
                </div>
              </div>
            </div>
          </template>
        </Message>
      </div>
      <Message v-if="isCompleted && isQuizOrSurveySkill && quizOrSurveyPassed" :closable="false" severity="success" data-cy="quizCompletedMsg">
      <template #container>
        <div class="flex flex-column md:flex-row gap-2 p-3 align-items-center">
          <div>
            <i class="far fa-smile text-2xl" aria-hidden=""></i>
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
        <div v-if="showQuizResults" class="border-top-1">
          <skills-spinner v-if="loadingAttempt" :is-loading="true"/>
          <div v-if="!loadingAttempt" class="pl-6 pr-5 pb-4">
            <quiz-single-run  :run-info="lastQuizAttempt"  :show-cards="false" />
          </div>
        </div>
      </template>
    </Message>
    </div>
  </div>
</template>

<style scoped>

</style>
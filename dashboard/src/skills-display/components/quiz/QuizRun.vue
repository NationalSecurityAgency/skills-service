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
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { object, string, number, array } from 'yup';
import { useSkillsAnnouncer } from '@/common-components/utilities/UseSkillsAnnouncer.js'
import { useTimeUtils } from '@/common-components/utilities/UseTimeUtils.js'
import { useCheckIfAnswerChangedForValidation } from '@/common-components/utilities/UseCheckIfAnswerChangedForValidation.js'
import dayjs from '@/common-components/DayJsCustomizer.js'
import SkillsSpinner from '@/components/utils/SkillsSpinner.vue';

import QuizRunService from '@/skills-display/components/quiz/QuizRunService.js';
import QuizRunSplashScreen from '@/skills-display/components/quiz/QuizRunSplashScreen.vue';
import SurveyRunCompletionSummary from '@/skills-display/components/quiz/SurveyRunCompletionSummary.vue';
import QuizRunCompletionSummary from '@/skills-display/components/quiz/QuizRunCompletionSummary.vue';
import QuizRunValidationWarnings from '@/skills-display/components/quiz/QuizRunValidationWarnings.vue';
import QuestionType from '@/skills-display/components/quiz/QuestionType.js';
import SkillsOverlay from "@/components/utils/SkillsOverlay.vue";
import QuizRunQuestion from '@/skills-display/components/quiz/QuizRunQuestion.vue';
import { useForm } from "vee-validate";
import QuizStatus from "@/components/quiz/runsHistory/QuizStatus.js";
import {useAppConfig} from "@/common-components/stores/UseAppConfig.js";
import {useNumberFormat} from "@/common-components/filter/UseNumberFormat.js";
import MarkdownText from "@/common-components/utilities/markdown/MarkdownText.vue";
import {useDebounceFn} from "@vueuse/core";
import {useDescriptionValidatorService} from "@/common-components/validators/UseDescriptionValidatorService.js";

const props = defineProps({
  quizId: String,
  quiz: {
    type: Object,
    default: null,
  },
  multipleTakes: {
    type: Boolean,
    default: false,
  },
  skillId: {
    type: String,
    default: null,
  },
  projectId: {
    type: String,
    default: null,
  }
})
const emit = defineEmits(['cancelled', 'testWasTaken'])
const announcer = useSkillsAnnouncer()
const timeUtils = useTimeUtils()
const appConfig = useAppConfig()
const numFormat = useNumberFormat()
const checkIfAnswerChangedForValidation = useCheckIfAnswerChangedForValidation()
const descriptionValidatorService = useDescriptionValidatorService()

const isLoading = ref(true);
const isCompleting = ref(false);
const quizInfo = ref({});
const quizResult = ref(null);
const quizAttemptId = ref(null);
const reportAnswerPromises = ref([]);
const splashScreen = ref({
  show: false,
});
const currentDate = ref(null);
const dateTimer = ref(null);
const scrollDistance = ref(0);
const isAttemptAlreadyInProgress = ref(false);


const atLeastOneSelected = (value) => {
  return !isAttemptAlreadyInProgress.value || value && (value.findIndex((a) => a.selected) >= 0);
}
const ratingSelected = (value) => {
  return  value > 0;
}
const getQuestionNumFromPath = (path) => {
  return Number(path.split('[').pop().split(']')[0]) + 1;
}

const validateFunCache = new Map()

const getAnswerIdFromContext = (testContext) => {
  const quizAnswers = testContext?.parent.quizAnswers
  if (quizAnswers && quizAnswers.length === 1 && quizAnswers[0].id) {
    return quizAnswers[0].id
  }
  return null
}
const createValidateAnswerFn = (valueOuter, contextOuter) => {
  if (!QuestionType.isTextInput(contextOuter.parent.questionType)) {
    return true
  }
  const getResponseBasedOnResult = (result, resContext) => {
    if (result.valid) {
      return true
    }
    if (result.msg) {
      return resContext.createError({ message: `Answer to question #${getQuestionNumFromPath(resContext.path)} - ${result.msg}` })
    }
    return resContext.createError({ message: `'Field' is invalid` })
  }
  const doValidateAnswer = (value, context) => {
    if (!value || value.trim().length === 0 || !appConfig.paragraphValidationRegex) {
      return true
    }
    const forceAnswerValidation = isSubmitting.value
    if (!forceAnswerValidation) {
      const existingResultIfValueTheSame = checkIfAnswerChangedForValidation.getStatusIfValueTheSame(getAnswerIdFromContext(context), context.originalValue)
      if (existingResultIfValueTheSame !== null) {
        return getResponseBasedOnResult(existingResultIfValueTheSame, context)
      }
    }

    return descriptionValidatorService.validateDescription(value, false, null, true).then((result) => {
      checkIfAnswerChangedForValidation.setValueAndStatus(getAnswerIdFromContext(context), value, result)
      return getResponseBasedOnResult(result, context)
    })
  }

  if (isSubmitting.value) {
    return doValidateAnswer(valueOuter, contextOuter)
  }
  let validateFn = validateFunCache.get(contextOuter.path)
  if (!validateFn) {
    validateFn = useDebounceFn((valueDebounce, contextDebounce) => {
      return doValidateAnswer(valueDebounce, contextDebounce)
    }, appConfig.formFieldDebounceInMs)

    validateFunCache.set(contextOuter.path, validateFn)
  }
  return validateFn(valueOuter, contextOuter)
}
const schema = object({
  'questions': array()
      .of(
          object({
            'questionType': string(),
            // important: please note that this logic has to match what's performed by `validateTextAnswer` method beow
            'answerText': string()
                .trim()
                .max(appConfig.maxTakeQuizInputTextAnswerLength, (d) => `Answer to question #${getQuestionNumFromPath(d.path)} must not exceed ${numFormat.pretty(appConfig.maxTakeQuizInputTextAnswerLength)} characters`)
                .when('questionType', {
                  is: QuestionType.TextInput,
                  then: (sch)  => sch
                      .trim()
                      .required((d) => `Answer to question #${getQuestionNumFromPath(d.path)} is required`)
                      .test('customAnswerValidator',"", async (value, context) => {
                        return await createValidateAnswerFn(value, context)
                      }),
                }),
            'answerRating': number()
                .when('questionType', {
                  is: QuestionType.Rating,
                  then: (sch)  => sch
                      .nullable()
                      .test('ratingSelected', 'A rating must be selected', (value) => ratingSelected(value))
                      .label('Rating'),
                }),
            'quizAnswers': array()
                .when('questionType', {
                  is: (questionType) => questionType === QuestionType.SingleChoice || questionType === QuestionType.MultipleChoice,
                  then: (sch)  => sch
                      .required()
                      .test('atLeastOneSelected', 'At least 1 choice must be selected', (value) => atLeastOneSelected(value))
                      .label('Answers'),
                })
          })
      ),
})
const { values, meta, handleSubmit, isSubmitting, resetForm, setFieldValue, validate, validateField, errors, errorBag, setErrors } = useForm({
  validationSchema: schema,
})

// important: please note that this logic has to match what's performed by `answerText` in the yup schema above
const validateTextAnswer = (value) => {
  validateField(value.fieldName)
  const textAnswer = value.answerText?.trim()
  if (textAnswer && textAnswer.length === 0) {
    return Promise.resolve(false);
  }
  if (textAnswer && textAnswer.length > appConfig.maxTakeQuizInputTextAnswerLength) {
    return Promise.resolve(false);
  }

  if (!appConfig.paragraphValidationRegex) {
    return Promise.resolve(true)
  }
  const existingStatusIfValueTheSame = checkIfAnswerChangedForValidation.getStatusIfValueTheSame(value.answerId, value.answerText)
  if (existingStatusIfValueTheSame !== null && result.valid) {
    return Promise.resolve(existingStatusIfValueTheSame)
  }
  return descriptionValidatorService.validateDescription(value.answerText, false, null, true).then((result) => {
    checkIfAnswerChangedForValidation.setValueAndStatus(value.answerId, value.answerText, result)
    if (result.valid) {
      return true
    }
    return false
  })
}

onMounted(() => {
  if (props.quiz) {
    setQuizInfo(({ ...props.quiz }));
  } else {
    loadData();
  }
})

onBeforeUnmount(() => {
  destroyDateTimer();
})

const isSurveyType = computed(() => {
  return quizInfo.value.quizType === 'Survey';
})
const errorsToShow = computed(() => {
  const values = Object.values(errors.value).flat().filter((val) => val && val.length > 0);
  const unique = values.filter((v, i, a) => a.indexOf(v) === i);
  return unique && unique.length > 0 ? unique : null;
})
const showQuestions = computed(() => {
  return !quizResult.value
      || (quizResult.value && quizResult.value.gradedRes && quizResult.value.gradedRes.gradedQuestions && quizResult.value.gradedRes.gradedQuestions.length > 0);
})

// methods
const handleScroll = () => {
  scrollDistance.value = window.scrollY;
}
const beginDateTimer = () => {
  if (quizInfo.value.deadline) {
    window.addEventListener('scroll', handleScroll());
    currentDate.value = dayjs().utc().valueOf();
    dateTimer.value = setInterval(() => {
      currentDate.value = dayjs().utc().valueOf();
      if (currentDate.value >= dayjs(quizInfo.value.deadline).utc().valueOf()) {
        destroyDateTimer();
        QuizRunService.failQuizAttempt(props.quizId, quizAttemptId.value).then((gradedRes) => {
          const numTotal = quizInfo.value.quizLength;
          const numCorrect = 0;
          const percentCorrect = Math.trunc(((numCorrect * 100) / numTotal));
          quizResult.value = {
            gradedRes,
            numCorrect,
            numTotal,
            percentCorrect,
            missedBy: numTotal,
            outOfTime: true,
          };
        });
      }
    }, 1000);
  }
}
const destroyDateTimer = () => {
  clearInterval(dateTimer.value);
  dateTimer.value = null;
  window.removeEventListener('scroll', handleScroll);
}
const loadData = () => {
  isLoading.value = true;
  QuizRunService.getQuizInfo(props.quizId, props.skillId, props.projectId)
      .then((quizInfoRes) => {
        setQuizInfo(quizInfoRes);
      });
}
const setQuizInfo = (quizInfoRes) => {
  const percentToPass = quizInfoRes.minNumQuestionsToPass <= 0 ? 100 : Math.trunc(((quizInfoRes.minNumQuestionsToPass * 100) / quizInfoRes.quizLength));
  quizInfo.value = { ...quizInfoRes, percentToPass };
  if (quizInfoRes.isAttemptAlreadyInProgress) {
    isAttemptAlreadyInProgress.value = true;
    startQuizAttempt();
  } else {
    splashScreen.value.show = true;
    isLoading.value = false;
  }
}
const failQuizAttempt = () => {
  destroyDateTimer();
  quizResult.value = {
    gradedRes: {
      associatedSkillResults: [],
      completed: null,
      gradedQuestion: [],
      numberQuestionsGotWrong: 0,
      passed: false,
      started: null,
    },
    missedBy: quizInfo.value.quizLength,
    numCorrect: 0,
    numTotal: quizInfo.value.quizLength,
    percentCorrect: 0,
    outOfTime: true,
  };
}
const startQuizAttempt = () => {
  isLoading.value = true;
  splashScreen.value.show = false;

  QuizRunService.startQuizAttempt(props.quizId, props.skillId, props.projectId)
      .then((startQuizAttemptRes) => {
        if (startQuizAttemptRes.existingAttemptFailed) {
          failQuizAttempt();
          return;
        }
        quizAttemptId.value = startQuizAttemptRes.id;
        const {
          selectedAnswerIds, enteredText, questions, deadline,
        } = startQuizAttemptRes;
        quizInfo.value.deadline = deadline;
        quizInfo.value.questions = questions;
        const copy = ({ ...quizInfo.value });
        copy.questions = quizInfo.value.questions.map((q) => {
          const answerOptions = q.answerOptions.map((a) => ({
            ...a,
            selected: !!(selectedAnswerIds && selectedAnswerIds.includes(a.id)),
          }));
          if (enteredText && q.questionType === QuestionType.TextInput) {
            const answerId = q.answerOptions[0].id;
            const enteredTextObj = enteredText.find((t) => t.answerId === answerId);
            if (enteredTextObj) {
              // eslint-disable-next-line no-param-reassign
              answerOptions[0].answerText = enteredTextObj.answerText;
            }
          }
          return ({ ...q, answerOptions });
        });
        quizInfo.value = copy;
        initializeFormData(copy)
        beginDateTimer();
      }).finally(() => {
    isLoading.value = false;
  });
}
const initializeFormData = (copy) => {
  const formQuestions = copy.questions.map((q) => {
    const answerRating = q.questionType === QuestionType.Rating ? q.answerOptions.find((a) => a.selected) : 0
    return {
      questionType: q.questionType,
      quizAnswers: q.answerOptions.map((a) => ({ ...a, selected: a.selected ? a.selected : false })),
      answerText: q.questionType === QuestionType.TextInput ? (q.answerOptions[0]?.answerText || '') : '',
      answerRating: answerRating ? Number(answerRating.answerOption) : 0,
    }
  })
  checkIfAnswerChangedForValidation.reset()
  resetForm({ values: { questions: formQuestions }, errors: {} });
}
const updateSelectedAnswers = (questionSelectedAnswer) => {
  isAttemptAlreadyInProgress.value = true;
  if (questionSelectedAnswer.reportAnswerPromise) {
    reportAnswerPromises.value.push(questionSelectedAnswer.reportAnswerPromise);
  }
}
const completeTestRun = () => {
  isAttemptAlreadyInProgress.value = true;
  submitTestRun()
}
const submitTestRun = handleSubmit((values) => {
  isCompleting.value = true;
  Promise.all(reportAnswerPromises.value)
    .then(() => {
      reportTestRunToBackend()
        .finally(() => {
          destroyDateTimer();
          isCompleting.value = false;
          if (!isSurveyType.value) {
            nextTick(() => {
              const element = document.getElementById('quizRunCompletionSummary');
              element.scrollIntoView({ behavior: 'smooth' });
            });
          }
          let announceMsg = `Completed ${quizInfo.value.quizType}`;
          if (!isSurveyType.value) {
            announceMsg = `${announceMsg}. ${!quizResult.value.gradedRes.passed ? 'Failed' : 'Successfully passed'} quiz.`;
          }
          announcer.polite(announceMsg);
        });
    });
})
const reportTestRunToBackend = () => {
  return QuizRunService.completeQuizAttempt(props.quizId, quizAttemptId.value)
      .then((gradedRes) => {
        const numTotal = quizInfo.value.quizLength;
        const numCorrect = numTotal - gradedRes.numQuestionsGotWrong;
        const percentCorrect = Math.trunc(((numCorrect * 100) / numTotal));
        const minNumQuestionsToPass = quizInfo.value.minNumQuestionsToPass;
        const numQuestionsGotWrong = gradedRes.numQuestionsGotWrong;
        const missedBy = minNumQuestionsToPass > 0 ? minNumQuestionsToPass - numCorrect : numQuestionsGotWrong;
        quizResult.value = {
          gradedRes,
          numCorrect,
          numTotal,
          percentCorrect,
          missedBy,
        };

        if (gradedRes.gradedQuestions && gradedRes.gradedQuestions.length > 0) {
          const updatedQuizInfo = ({ ...quizInfo.value });
          updatedQuizInfo.questions = updatedQuizInfo.questions.map((q) => {
            const gradedQuestion = gradedRes.gradedQuestions.find((gradedQ) => gradedQ.questionId === q.id);

            const answerOptions = q.answerOptions.map((a) => ({
              ...a,
              selected: gradedQuestion.selectedAnswerIds.includes(a.id),
              isGraded: !QuizStatus.isNeedsGrading(gradedQuestion.status),
              isCorrect: gradedQuestion.correctAnswerIds.includes(a.id),
            }));
            return ({
              ...q,
              gradedInfo: gradedQuestion,
              answerOptions,
            });
          });
          quizInfo.value = updatedQuizInfo;
        }
        announcer.polite('Quiz Completed')
      });
}
const tryAgain = () => {
  quizResult.value = null;
  loadData();
}
const cancelQuizAttempt = () => {
  emit('cancelled');
}
const saveAndCloseThisRun = () => {
  isCompleting.value = true;
  Promise.all(reportAnswerPromises.value)
      .then(() => {
        emit('cancelled');
        isCompleting.value = false;
      });
}
const doneWithThisRun = () => {
  emit('testWasTaken', quizResult.value);
}
</script>

<template>
  <div>
    <SkillsSpinner :is-loading="isLoading" class="mt-4"/>
    <div v-if="!isLoading">
      <QuizRunSplashScreen v-if="splashScreen.show" :quiz-info="quizInfo" @cancelQuizAttempt="cancelQuizAttempt" @start="startQuizAttempt" :multipleTakes="multipleTakes">
        <template #aboveTitle>
          <slot name="splashPageTitle" />
        </template>
      </QuizRunSplashScreen>

      <SurveyRunCompletionSummary
          ref="surveyRunCompletionSummary"
          v-if="isSurveyType && quizResult && !splashScreen.show"
          class="mb-4"
          :quiz-info="quizInfo"
          :quiz-result="quizResult"
          @close="doneWithThisRun">
        <template #completeAboveTitle>
          <slot name="aboveTitleWhenPassed" v-if="quizResult.gradedRes.passed">
            <Message severity="success" icon="fas fa-handshake">Thank you for taking the time to complete the survey!</Message>
          </slot>
        </template>
      </SurveyRunCompletionSummary>

      <QuizRunCompletionSummary
          id="quizRunCompletionSummary"
          ref="quizRunCompletionSummary"
          v-if="!isSurveyType && quizResult && !splashScreen.show"
          class="mb-4"
          :quiz-info="quizInfo"
          :quiz-result="quizResult"
          @close="doneWithThisRun"
          @run-again="tryAgain">
        <template #completeAboveTitle>
          <slot name="aboveTitleWhenPassed" v-if="quizResult.gradedRes.passed">
            <Message severity="success" icon="fas fa-handshake">
              <span v-if="isSurveyType">Thank you for taking time to take this survey! </span>
              <span v-else>Thank you for completing the Quiz!</span>
            </Message>
          </slot>
        </template>
      </QuizRunCompletionSummary>
      
      <Card v-if="!splashScreen.show && !(isSurveyType && quizResult) && showQuestions" class="mb-6" data-cy="quizRunQuestions">
        <template #content>
          <div class="flex flex-wrap items-center justify-center border-b py-2 mb-4" data-cy="subPageHeader">
            <div class="flex">
              <div class="text-2xl text-primary font-bold skills-page-title-text-color" data-cy="quizName" role="heading" aria-level="1">{{ quizInfo.name }}</div>
            </div>
            <div class="flex-1 text-right text-muted">
              <Tag severity="success" data-cy="numQuestions">{{quizInfo.quizLength}}</Tag> <span class="uppercase">questions</span>
              <span v-if="quizInfo.quizTimeLimit > 0 && dateTimer !== null"> | {{ timeUtils.formatDurationDiff(currentDate, quizInfo.deadline, false, true)}}</span>
            </div>
          </div>

          <Card :pt="{ body: { class: '!p-1' }, content: { class: '!p-1' } }" class="mb-3" v-if="quizInfo.description && quizInfo.showDescriptionOnQuizPage">
            <template #content>
              <markdown-text
                  :text="quizInfo.description"
                  instance-id="quizDescriptionText"
                  data-cy="quizDescription" />
            </template>
          </Card>

          <SkillsOverlay :show="isCompleting" opacity="0.2">
            <div v-for="(q, index) in quizInfo.questions" :key="q.id">
              <QuizRunQuestion
                  :q="q"
                  :quiz-id="quizId"
                  :quiz-attempt-id="quizAttemptId"
                  :num="index+1"
                  :validate="validateTextAnswer"
                  @selected-answer="updateSelectedAnswers"
                  @answer-text-changed="updateSelectedAnswers"/>
            </div>
          </SkillsOverlay>

          <QuizRunValidationWarnings v-if="!meta.valid && !quizResult?.gradedRes?.needsGrading" :errors-to-show="errorsToShow" />

          <div v-if="!quizResult" class="text-left mt-8 flex flex-wrap">
            <SkillsOverlay :show="isCompleting" opacity="0.6">
              <SkillsButton severity="success" outlined
                            :label="`Complete ${quizInfo.quizType}`"
                            icon="fas fa-check-double"
                            @click="completeTestRun"
                            :disabled="isCompleting"
                            :aria-label="`Done with ${quizInfo.quizType}`"
                            class="text-uppercase font-weight-bold skills-theme-btn"
                            data-cy="completeQuizBtn">
              </SkillsButton>
            </SkillsOverlay>
          </div>

          <div v-if="quizResult && quizResult.gradedRes && quizResult.gradedRes.passed" class="text-left mt-8">
            <SkillsButton :severity="quizResult.gradedRes.passed || quizResult.gradedRes.needsGrading ? 'success' : 'danger'" outlined
                          label="Close"
                          icon="fas fa-times-circle"
                          @click="doneWithThisRun"
                          class="text-uppercase font-weight-bold skills-theme-btn">
            </SkillsButton>
          </div>
          <div v-if="quizResult && quizResult.gradedRes && !quizResult.gradedRes.passed" class="mt-8">
            <div class="my-2" v-if="(quizInfo.maxAttemptsAllowed - quizInfo.userNumPreviousQuizAttempts - 1) > 0"><span class="text-info">No worries!</span> Would you like to try again?</div>
            <SkillsButton :severity="quizResult.gradedRes.needsGrading ? 'success' : 'danger'"
                          outlined
                          label="Close"
                          icon="fas fa-times-circle"
                          @click="doneWithThisRun"
                          class="text-uppercase font-weight-bold mr-2 skills-theme-btn"
                          data-cy="closeQuizBtn">
            </SkillsButton>
            <SkillsButton v-if="(quizInfo.maxAttemptsAllowed - quizInfo.userNumPreviousQuizAttempts - 1) > 0"
                          severity="success" outlined
                          label="Try Again"
                          icon="fas fa-redo"
                          @click="tryAgain"
                          class="text-uppercase font-weight-bold skills-theme-btn"
                          data-cy="runQuizAgainBtn">
            </SkillsButton>
          </div>

        </template>
      </Card>

      <div v-if="scrollDistance > 300" id="floating-timer">
        <div v-if="quizInfo.quizTimeLimit > 0 && dateTimer !== null">{{ timeUtils.formatDurationDiff(currentDate, quizInfo.deadline, false, true) }}</div>
      </div>
    </div>
  </div>
</template>

<style scoped>

</style>
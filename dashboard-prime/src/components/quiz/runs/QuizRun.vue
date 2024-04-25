<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { object, string, number, array } from 'yup';
import { useSkillsAnnouncer } from '@/common-components/utilities/UseSkillsAnnouncer.js'
import { useTimeUtils } from '@/common-components/utilities/UseTimeUtils.js'
import SkillsSpinner from '@/components/utils/SkillsSpinner.vue';

import QuizRunService from '@/common-components/quiz/QuizRunService.js';
import QuizRunSplashScreen from '@/components/quiz/runs/QuizRunSplashScreen.vue';
import QuestionType from '@/common-components/quiz/QuestionType.js';
import SkillsOverlay from "@/components/utils/SkillsOverlay.vue";
import QuizRunQuestion from '@/components/quiz/runs/QuizRunQuestion.vue';
import { useForm } from "vee-validate";

const props = defineProps({
  quizId: String,
  quiz: {
    type: Object,
    default: null,
  },
})
const emit = defineEmits(['cancelled', 'test-was-taken'])
const announcer = useSkillsAnnouncer()
const timeUtils = useTimeUtils()

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
const schema = object({
  'questions': array()
      .of(
          object({
            'questionType': string(),
            'answerText': string()
                .when('questionType', {
                  is: QuestionType.TextInput,
                  then: (sch)  => sch
                      .trim()
                      .required((d) => `Answer to question #${getQuestionNumFromPath(d.path)} is required`)
                      .customDescriptionValidator(null, false, null, (d) => `Answer to question #${getQuestionNumFromPath(d.path)}`),
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
const { values, meta, handleSubmit, isSubmitting, resetForm, setFieldValue, validate, errors, errorBag, setErrors } = useForm({
  validationSchema: schema,
})
// const { remove, push, fields } = useFieldArray('questions');

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
  const values = Object.values(errors).flat().filter((val) => val && val.length > 0);
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
          const numTotal = quizInfo.value.questions.length;
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
  window.removeEventListener('scroll', handleScroll());
}
const loadData = () => {
  isLoading.value = true;
  QuizRunService.getQuizInfo(props.quizId)
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
    missedBy: quizInfo.value.questions.length,
    numCorrect: 0,
    numTotal: quizInfo.value.questions.length,
    percentCorrect: 0,
    outOfTime: true,
  };
}
const startQuizAttempt = () => {
  isLoading.value = true;
  splashScreen.value.show = false;

  QuizRunService.startQuizAttempt(props.quizId)
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
  resetForm({ values: { questions: formQuestions }, errors: {} });
}
const updateSelectedAnswers = (questionSelectedAnswer) => {
  isAttemptAlreadyInProgress.value = true;
  if (questionSelectedAnswer.reportAnswerPromise) {
    reportAnswerPromises.value.push(questionSelectedAnswer.reportAnswerPromise);
  }
}
const completeTestRun = handleSubmit((values) => {
  isCompleting.value = true;
  this.$refs.observer.validate().then((validationResults) => {
    if (validationResults) {
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
    } else {
      isCompleting.value = false;
    }
  });
})
const reportTestRunToBackend = () => {
  return QuizRunService.completeQuizAttempt(props.quizId, quizAttemptId.value)
      .then((gradedRes) => {
        const numTotal = quizInfo.value.questions.length;
        const numCorrect = numTotal - gradedRes.numQuestionsGotWrong;
        const percentCorrect = Math.trunc(((numCorrect * 100) / numTotal));
        quizResult.value = {
          gradedRes,
          numCorrect,
          numTotal,
          percentCorrect,
          missedBy: gradedRes.numQuestionsGotWrong,
        };

        if (gradedRes.gradedQuestions && gradedRes.gradedQuestions.length > 0) {
          const updatedQuizInfo = ({ ...quizInfo.value });
          updatedQuizInfo.questions = updatedQuizInfo.questions.map((q) => {
            const gradedQuestion = gradedRes.gradedQuestions.find((gradedQ) => gradedQ.questionId === q.id);

            const answerOptions = q.answerOptions.map((a) => ({
              ...a,
              selected: gradedQuestion.selectedAnswerIds.includes(a.id),
              isGraded: true,
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
    <SkillsSpinner :is-loading="isLoading" class="mt-3"/>
    <div v-if="!isLoading">
      <QuizRunSplashScreen v-if="splashScreen.show" :quiz-info="quizInfo" @cancelQuizAttempt="cancelQuizAttempt" @start="startQuizAttempt">
        <template #aboveTitle>
          <slot name="splashPageTitle">
            <span v-if="isSurveyType">Thank you for taking the time to take this survey!</span>
            <span v-else>You are about to begin the quiz!</span>
          </slot>
        </template>
      </QuizRunSplashScreen>

<!--      <survey-run-completion-summary-->
<!--          ref="surveyRunCompletionSummary"-->
<!--          v-if="isSurveyType && quizResult && !splashScreen.show"-->
<!--          class="mb-3"-->
<!--          :quiz-info="quizInfo"-->
<!--          :quiz-result="quizResult"-->
<!--          @close="doneWithThisRun">-->
<!--        <template slot="completeAboveTitle">-->
<!--          <slot name="completeAboveTitle">-->
<!--            <i class="fas fa-handshake text-info" aria-hidden="true"></i> Thank you for taking the time to complete the survey!-->
<!--          </slot>-->
<!--        </template>-->
<!--      </survey-run-completion-summary>-->

<!--      <quiz-run-completion-summary-->
<!--          id="quizRunCompletionSummary"-->
<!--          ref="quizRunCompletionSummary"-->
<!--          v-if="!isSurveyType && quizResult && !splashScreen.show"-->
<!--          class="mb-3"-->
<!--          :quiz-info="quizInfo"-->
<!--          :quiz-result="quizResult"-->
<!--          @close="doneWithThisRun"-->
<!--          @run-again="tryAgain">-->
<!--        <template slot="completeAboveTitle">-->
<!--          <slot name="completeAboveTitle">-->
<!--            <span v-if="isSurveyType">Thank you for taking time to take this survey! </span>-->
<!--            <span v-else>Thank you for completing the Quiz!</span>-->
<!--          </slot>-->
<!--        </template>-->
<!--      </quiz-run-completion-summary>-->
      
      <Card v-if="!splashScreen.show && !(isSurveyType && quizResult) && showQuestions" class="mb-4" data-cy="quizRunQuestions">
        <template #content>
          <div class="flex flex-wrap align-items-center justify-content-center border-bottom-1 py-2 mb-3" data-cy="subPageHeader">
            <div class="flex">
              <div class="text-2xl text-primary font-bold skills-page-title-text-color" data-cy="quizName">{{ quizInfo.name }}</div>
            </div>
            <div class="flex-1 text-right text-muted">
              <Tag severity="success" data-cy="numQuestions">{{quizInfo.quizLength}}</Tag> <span class="uppercase">questions</span>
              <span v-if="quizInfo.quizTimeLimit > 0 && dateTimer !== null"> | {{currentDate | duration(quizInfo.deadline, false, true)}}</span>
            </div>
          </div>

          <SkillsOverlay :show="isCompleting" opacity="0.2">
            <div v-for="(q, index) in quizInfo.questions" :key="q.id">
              <QuizRunQuestion
                  :q="q"
                  :quiz-id="quizId"
                  :quiz-attempt-id="quizAttemptId"
                  :num="index+1"
                  @selected-answer="updateSelectedAnswers"
                  @answer-text-changed="updateSelectedAnswers"/>
            </div>
          </SkillsOverlay>

<!--          <quiz-run-validation-warnings v-if="invalid" :errors-to-show="errorsToShow" />-->

          <div v-if="!quizResult" class="text-left mt-5 flex flex-wrap">
<!--            <SkillsButton severity="info" outlined-->
<!--                          label="Save and Close"-->
<!--                          icon="fas fa-save"-->
<!--                          @click="saveAndCloseThisRun"-->
<!--                          class="text-uppercase mr-2 font-weight-bold skills-theme-btn"-->
<!--                          :disabled="isCompleting"-->
<!--                          :aria-label="`Save and close this ${quizInfo.quizType}`"-->
<!--                          data-cy="saveAndCloseQuizAttemptBtn">-->
<!--            </SkillsButton>-->
            <SkillsOverlay :show="isCompleting" opacity="0.6">
              <SkillsButton severity="success" outlined
                            :label="`Complete ${quizInfo.quizType}`"
                            icon="fas fa-check-double"
                            @click="completeTestRun"
                            :disabled="isCompleting || !meta.valid"
                            :aria-label="`Done with ${quizInfo.quizType}`"
                            class="text-uppercase font-weight-bold skills-theme-btn"
                            data-cy="completeQuizBtn">
              </SkillsButton>
            </SkillsOverlay>
          </div>

          <div v-if="quizResult && quizResult.gradedRes && quizResult.gradedRes.passed" class="text-left mt-5">
            <SkillsButton severity="danger" outlined
                          label="Close"
                          icon="fas fa-times-circle"
                          @click="doneWithThisRun"
                          class="text-uppercase font-weight-bold skills-theme-btn">
            </SkillsButton>
          </div>
          <div v-if="quizResult && quizResult.gradedRes && !quizResult.gradedRes.passed" class="mt-5">
            <div class="my-2" v-if="(quizInfo.maxAttemptsAllowed - quizInfo.userNumPreviousQuizAttempts - 1) > 0"><span class="text-info">No worries!</span> Would you like to try again?</div>
            <SkillsButton severity="danger" outlined
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
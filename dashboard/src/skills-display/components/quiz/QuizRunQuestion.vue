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
import { computed, onMounted, defineAsyncComponent, ref } from 'vue'
import MarkdownText from '@/common-components/utilities/markdown/MarkdownText.vue';
import SkillsRating from "@/components/utils/inputForm/SkillsRating.vue";
import QuizRunAnswers from '@/skills-display/components/quiz/QuizRunAnswers.vue';
import QuestionType from '@/skills-display/components/quiz/QuestionType.js';
import QuizRunService from '@/skills-display/components/quiz/QuizRunService.js';
import MarkdownEditor from "@/common-components/utilities/markdown/MarkdownEditor.vue";
import QuizStatus from "@/components/quiz/runsHistory/QuizStatus.js";
import {useDebounceFn} from "@vueuse/core";
import {useAppConfig} from "@/common-components/stores/UseAppConfig.js";
import SkillsButton from "@/components/utils/inputForm/SkillsButton.vue";
import QuizRunMatchingComponent from "@/skills-display/components/quiz/QuizRunMatchingComponent.vue";

const VideoPlayer = defineAsyncComponent(() =>
    import('@/common-components/video/VideoPlayer.vue')
)

const props = defineProps({
  q: Object,
  quizId: String,
  quizAttemptId: Number,
  num: Number,
  validate: Function,
  userCommunity: String,
  quizComplete: Boolean,
})

const isLoading = ref(true);
const emit = defineEmits(['answer-text-changed', 'selected-answer', 'answer-matched'])

const appConfig = useAppConfig()

const answerOptions = ref([])
const answerRating = ref(0)
const answerText = ref(props.q.questionType === QuestionType.TextInput ? (props.q.answerOptions[0]?.answerText || '') : '')

const mediaAttributes = computed(() => {
  const attr = props.q.mediaAttributes?.videoConf;
  const captionsUrl = attr?.captions
      ? `/api/quiz-definitions/${props.quizId}/questions/${props.q.id}/videoCaptions`
      : null;
  if(attr) {
    return {
      videoId: props.q.id,
      url: attr.videoUrl,
      videoType: attr.videoType ? attr.videoType : null,
      isAudio: attr.videoType ? attr.videoType.includes('audio/') : null,
      captionsUrl,
      width: attr.width,
      height: attr.height,
      transcript: attr.transcript,
    };
  }
  return null;
})
const isMultipleChoice = computed(() => {
  return props.q.questionType === QuestionType.MultipleChoice;
})
const isSingleChoice = computed(() => {
  return props.q.questionType === QuestionType.SingleChoice;
})
const isTextInput = computed(() => {
  return props.q.questionType === QuestionType.TextInput;
})
const isRating = computed(() => {
  return props.q.questionType === QuestionType.Rating;
})
const isMatchingType = computed(() => {
  return props.q.questionType === QuestionType.Matching;
})
const isMissingAnswer = computed(() => {
  if (isTextInput.value) {
    return !answerText.value || answerText.value.trimEnd() === '';
  }
  return answerOptions.value.findIndex((a) => a.selected === true) < 0;
})
const questionNumAriaLabel = computed(() => {
  let res = `Question number ${props.num}`;
  if (props.q.gradedInfo) {
    if (props.q.gradedInfo.isCorrect) {
      res = `${res} was answered correctly`;
    } else {
      res = `${res} was answered incorrectly`;
    }
  }
  return res;
})
const numberOfStars = computed(() => {
  return props.q?.answerOptions?.length;
})
const fieldName = computed(() => {
  const num = props.num;
  if (isTextInput.value) {
    return `questions[${num-1}].answerText`;
  } else if (isRating.value) {
    return `questions[${num-1}].answerRating`;
  }
  return `questions[${num-1}].quizAnswers`;
})

onMounted(() => {
  answerOptions.value = props.q.answerOptions.map((a) => ({ ...a, selected: a.selected ? a.selected : false }));
  if (isRating.value) {
    const selectedAnswer = answerOptions.value.find((a) => a.selected);
    if (selectedAnswer) {
      answerRating.value = Number(selectedAnswer.answerOption);
    }
  }
  isLoading.value = false;
})

const textAnswerChanged = (providedAnswerText) => {
  const selectedAnswerIds = answerOptions.value.map((a) => a.id);
  if (providedAnswerText) {
    answerText.value = providedAnswerText;
  }
  const isAnswerBlank = !answerText.value || answerText.value.trimEnd() === '';
  const currentAnswer = {
    questionId: props.q.id,
    questionType: props.q.questionType,
    selectedAnswerIds,
    changedAnswerId: answerOptions.value[0].id,
    changedAnswerIdSelected: !isAnswerBlank,
    answerText: answerText.value,
  };
  reportAnswer(currentAnswer).then((reportAnswerPromise) => {
    // only 1 answer in case of TextInput
    emit('answer-text-changed', {
      ...currentAnswer,
      reportAnswerPromise,
    });
  });
}
const textAnswerChangedDebounced = useDebounceFn((providedAnswerTextOuter) => textAnswerChanged(providedAnswerTextOuter), appConfig.formFieldDebounceInMs)

const selectionChanged = (currentAnswer) => { 
  reportAnswer(currentAnswer).then((reportAnswerPromise) => {
    emit('selected-answer', {
      ...currentAnswer,
      reportAnswerPromise,
    });
  });
}
const ratingChanged = (value) => {
  if (value) {
    const selectedAnswerIds = answerOptions.value.map((a) => a.id);
    const answerId = selectedAnswerIds[value - 1];
    const currentAnswer = {
      questionId: props.q.id,
      questionType: props.q.questionType,
      selectedAnswerIds: [answerId],
      changedAnswerId: answerId,
      changedAnswerIdSelected: true,
    };
    reportAnswer(currentAnswer).then((reportAnswerPromise) => {
      emit('selected-answer', {
        ...currentAnswer,
        reportAnswerPromise,
      });
    });
  }
}
const reportAnswer = (answer) => {
  if (!isLoading.value) {
    const reportAnswer = () => QuizRunService.reportAnswer(props.quizId, props.quizAttemptId, answer.changedAnswerId, answer.changedAnswerIdSelected, answer.answerText)
    if (QuestionType.isTextInput(props.q.questionType) ) {
      const answerInfo = { answerText: answer.answerText, fieldName: fieldName.value, quizId: props.quizId, quizAttemptId: props.quizAttemptId, answerId: answer.changedAnswerId };
      return props.validate(answerInfo).then((isValid) => {
        if (isValid) {
          return reportAnswer()
        }
        return null;
      })
    } else {
      return reportAnswer()
    }
  }
  return new Promise((resolve) => {
    resolve(null);
  });
}
const needsGrading = computed(() => QuizStatus.isNeedsGrading(props.q.gradedInfo?.status))

const showTranscript = ref(false);
const toggleTranscript = () => {
  showTranscript.value = !showTranscript.value;
}

const updateAnswerOrder = (newOrder) => {
  newOrder.forEach((pair) => {
    const answerItem = answerOptions.value.find((a) => a.answerOption === pair.term)
    const currentAnswer = {
      questionId: props.q.id,
      questionType: props.q.questionType,
      answerText: pair.value,
      changedAnswerId: answerItem.id
    };
    reportAnswer(currentAnswer).then((reportAnswerPromise) => {
      emit('answer-matched', {
        ...currentAnswer,
        reportAnswerPromise,
      });
    })
  })
}
</script>

<template>
  <div :data-cy="`question_${num}`">
    <div v-if="needsGrading">
      <Tag severity="warn" class="uppercase" data-cy="needsGradingTag"><i class="fas fa-user-check mr-1" aria-hidden="true"></i> Needs Grading</Tag>
    </div>
    <div class="flex gap-0 mb-6">
      <div class="flex items-start pt-2 pr-2">
        <Tag class="inline-block"
                 :aria-label="questionNumAriaLabel"
                 :severity="`${q.gradedInfo && !needsGrading ? (q.gradedInfo.isCorrect ? 'success' : 'danger') : 'secondary'}`">
          {{ num }}
        </Tag>
        <span v-if="q.gradedInfo && !needsGrading" class="ml-1 pt-1">
          <span v-if="q.gradedInfo.isCorrect" class="text-green-700 dark:text-green-400 skills-theme-quiz-correct-answer" style="font-size: 1.1rem;" data-cy="questionAnsweredCorrectly"><i class="fas fa-check-double" aria-hidden="true"></i></span>
          <span v-if="!q.gradedInfo.isCorrect" class="text-red-700 skills-theme-quiz-incorrect-answer" style="font-size: 1.1rem;" data-cy="questionAnsweredWrong"><i class="fas fa-times-circle" aria-hidden="true"></i></span>
        </span>
      </div>
      <div class="flex flex-1">
        <div class="flex flex-col w-full">
          <markdown-text :text="q.question" data-cy="questionsText" :instance-id="`${q.id}`" />
          <div v-if="mediaAttributes">
            <video-player :video-player-id="`quizVideoFor-${q.id}`"
                          :options="mediaAttributes"
                          :storeAndRecoverSizeFromStorage="true"
                          :align-center="false" />
            <div v-if="mediaAttributes.transcript">
              <SkillsButton style="text-decoration: underline; padding-right: 0.25rem; padding-left: 0.5rem;"
                            class="skills-theme-primary-color"
                            :label="!showTranscript ? 'View Transcript' : 'Hide Transcript'"
                            variant="link"
                            size="small"
                            text
                            data-cy="viewTranscriptBtn"
                            @click="toggleTranscript">

              </SkillsButton>
            </div>
            <Card v-if="mediaAttributes.transcript && showTranscript">
              <template #content>
                <label for="transcriptDisplay" class="h4">{{ mediaAttributes.isAudio ? 'Audio' : 'Video'}} Transcript:</label>
                <Panel id="transcriptDisplay" data-cy="videoTranscript">
                  <p class="m-0">{{ mediaAttributes.transcript }}</p>
                </Panel>
              </template>
            </Card>
          </div>
          <div v-if="isTextInput">
            <div v-if="needsGrading" class="border rounded-border border-surface px-4">
              <markdown-text
                  :text="answerText"
                  data-cy="textInputAnswer"
                  :instance-id="`question-${num}`" />
            </div>
            <markdown-editor v-else
                             class="form-text"
                             :id="`question-${num}`"
                             data-cy="textInputAnswer"
                             markdownHeight="250px"
                             label="Answer"
                             @value-changed="textAnswerChangedDebounced"
                             :show-label="false"
                             :name="fieldName"
                             :user-community="userCommunity"
                             :allow-community-elevation="true"
                             :allow-attachments="false"
                             :allow-insert-images="false"
                             :aria-label="`Please enter text to answer question number ${num}`"
                             placeholder="Please enter your response here..."
                             :disable-ai-prompt="true"
                             :resizable="true" />
          </div>
          <div v-else-if="isRating">
            <SkillsRating @update:modelValue="ratingChanged" class="flex-initial rounded-border py-4 px-6" v-model="answerRating" :stars="numberOfStars" :cancel="false" :name="fieldName"/>
          </div>
          <div v-else-if="isMatchingType">
            <QuizRunMatchingComponent :q="q" :name="fieldName" :value="answerOptions" @updateAnswerOrder="updateAnswerOrder" :questionNumber="num" :quizComplete="quizComplete" />
          </div>
          <div v-else>
            <div v-if="isMultipleChoice" class="text-secondary italic small" data-cy="multipleChoiceMsg">(Select <b>all</b> that apply)</div>
            <QuizRunAnswers class="mt-1 pl-1"
                            :name="fieldName"
                            @selected-answer="selectionChanged"
                            :value="answerOptions"
                            :q="q"
                            :q-num="num"
                            :can-select-more-than-one="isMultipleChoice"/>
          </div>
          <div class="flex" v-if="q.answerHint" data-cy="answerHint">
            <Message size="small" severity="warn" icon="fas fa-lightbulb" :closable="false" class="mt-2" data-cy="answerHintMsg">
              <pre data-cy="answerHintMsgContent">{{ q.answerHint}}</pre>
            </Message>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>

</style>
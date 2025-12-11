/*
Copyright 2025 SkillTree

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
import { computed, nextTick, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { useLog } from '@/components/utils/misc/useLog.js'
import { useImgHandler } from '@/common-components/utilities/learning-conent-gen/UseImgHandler.js'
import { useInstructionGenerator } from '@/common-components/utilities/learning-conent-gen/UseInstructionGenerator.js'
import QuestionCard from '@/components/quiz/testCreation/QuestionCard.vue'
import AiPromptDialog from '@/common-components/utilities/learning-conent-gen/AiPromptDialog.vue'
import { useAppConfig } from '@/common-components/stores/UseAppConfig.js'
import { useDescriptionValidatorService } from '@/common-components/validators/UseDescriptionValidatorService.js'
import { object, string } from 'yup'
import { useForm } from 'vee-validate'

const model = defineModel()
const props = defineProps({
  communityValue: {
    type: String,
    default: null
  },
})
const emit = defineEmits(['generated-quiz'])
const route = useRoute()
const log = useLog()
const imgHandler = useImgHandler()
const appConfig = useAppConfig()

const enableEditQuestionsIds = ref([])
const aiPromptDialogRef = ref(null)
const currentDescription = ref('')
const isGenerating = ref(true)
const currentQuizData = ref(null)
const allQuestions = ref([])

const instructionsGenerator = useInstructionGenerator()
const dynamicSchema = computed(() => {
  const validationObj = {}
  for (const question of allQuestions.value) {
    validationObj[question.name] = string()
        .required()
        .max(appConfig.descriptionMaxLength)
        .customDescriptionValidator('Question', false)
        .label('Question')
  }
  return object(validationObj)
})
watch(dynamicSchema, () => {
  nextTick(() => {
    validate()
  })
})
const { values, meta, handleSubmit, isSubmitting, resetForm, setFieldValue, validate, validateField, errors, errorBag, setErrors } = useForm({
  validationSchema: dynamicSchema,
})

const generateQuizFromDescription = (skillDescription) => {
  currentDescription.value = skillDescription
  const welcomeMsg = 'Hi there! I\'ll generate a quiz based on the *existing skill description*. Feel free to type additional instructions for me or just click the `Generate` button to get started'
  aiPromptDialogRef.value.addWelcomeMsg(welcomeMsg)
}
defineExpose({
  generateQuizFromDescription
});

const useGenerated = (historyItem) => {
  emit('generated-quiz', historyItem.generatedInfo.generatedQuiz);
}
const getQuizDataForDisplay = (quizData, historyId) => {
  let quizDataForDisplay = null
  if (quizData && quizData.generatedQuiz) {
    quizDataForDisplay = quizData.generatedQuiz.map((question, questionIdx) => ({
      ...question,
      id: `q-${historyId}-${(questionIdx+1)}`,
      answers: question.answers.map((answer, answerIdx) => ({
        ...answer,
        id: `a-${historyId}-${(questionIdx+1)}-${(answerIdx+1)}`
      }))
    }));
  }
  return quizDataForDisplay
}
const communityValue = computed(() => {
  let res = appConfig.defaultCommunityDescriptor
  if (props.allowCommunityElevation) {
    if (props.userCommunity) {
      res = props.userCommunity
    } else {
      if (route.params.projectId) {
        res = projConfig.getProjectCommunityValue();
      }
    }
  }
  return res
})

const overallErrMsg = 'Use Generate Quiz button is disabled because portion markings are missing - choose "Edit Questions" to fix or "Portion Mark Then Use" to fix all questions'
// const overallErrMsg = computed(() => {
//   return `
//     <div>
//       <button class="p-button p-component p-button-sm p-button-info" disabled>
//         <span>Use Generated Quiz</span>
//         <i class="fa-solid fa-check-double"></i>
//       </button>
//       <span>is disabled because portion markings are missing - Choose "Edit Questions" to fix or "Portion Mark Then Use" to fix all questions</span>
//     </div>
//   `;
// });
const currentJsonString = ref('')
const inQuestionsArray = ref(false);
const alreadyParsedQuestions = () => {
  return currentQuizData.value && currentQuizData.value.length > 0
}

const handleGeneratedChunk = (chunk, historyId) => {
  log.debug(`handleGeneratedChunk [inQuestionsArray: ${inQuestionsArray.value}], [chunk: ${chunk}]`)
  // Combine any previous buffer with the new chunk
  let newBuffer = currentJsonString.value + chunk;
  log.debug(`newBuffer: ${newBuffer}`)
  let currentPosition = 0;
  let inString = false;
  let braceDepth = 0;
  let bracketDepth = 0;
  let objectStart = -1;
  let questionBraceDepth = -1;
  let i = 0;

  // Process the buffer to find complete objects
  for (; i < newBuffer.length; i++) {
    const char = newBuffer[i];
    const prevChar = i > 0 ? newBuffer[i - 1] : '';

    // Track if we're inside a string (accounting for escaped quotes)
    if (char === '"' && prevChar !== '\\') {
      inString = !inString;
    }

    // Only process structure when not in a string
    if (!inString) {
      if (char === '{') {
        if (inQuestionsArray.value && (alreadyParsedQuestions() && questionBraceDepth === -1) || !alreadyParsedQuestions() && braceDepth === 1     ) {
          objectStart = i; // Start of a new object
          questionBraceDepth = braceDepth;
          log.debug(`starting new question [objectStart: ${objectStart}, questionBraceDepth: ${questionBraceDepth}, braceDepth: ${braceDepth}], i: ${i}`)
        }
        braceDepth++;
      } else if (char === '}') {
        braceDepth--;

        // If we've found a complete object and we're inside the questions array
        if (braceDepth>= 0 && braceDepth === questionBraceDepth && (inQuestionsArray.value ||
            (i > 12 && newBuffer.lastIndexOf('"questions"') > -1 &&
                newBuffer.lastIndexOf('"questions"') < i &&
                newBuffer.substring(newBuffer.lastIndexOf('"questions"'), i).includes('[')))) {
          try {
            inQuestionsArray.value = true;
            const jsonStr = newBuffer.substring(objectStart, i + 1);
            log.debug(`found complete question [jsonStr: ${jsonStr}]`)
            const question = safeJsonParse(jsonStr);
            if (question && question.question) {  // Basic validation
              const questionIdx = currentQuizData && currentQuizData.value && currentQuizData.value.length ? currentQuizData.value.length : 0
              question.name = `q${historyId}${questionIdx + 1}`;
              ensureValidQuestionType(question);
              if (currentQuizData && currentQuizData.value && currentQuizData.value.length > 0) {
                currentQuizData.value.push(question)
              } else {
                currentQuizData.value = [question]
              }
              setFieldValue(question.name, question.question);
              objectStart = -1; // Reset for next object
              questionBraceDepth = -1;
              currentJsonString.value = ''
              currentPosition = i + 1;
            } else {
              log.warn(`invalid question [jsonStr: ${jsonStr}]`)
            }
          } catch (e) {
            console.error('Error parsing question:', e);
          }
        }
      } else if (char === '[') {
        if (i > 12 && newBuffer.substring(i - 13, i).includes('"questions"')) {
          inQuestionsArray.value = true;
        }
        bracketDepth++;
      } else if (char === ']') {
        bracketDepth--;
      }
    }
  }

  // Return remaining buffer that couldn't be processed yet
  currentJsonString.value = newBuffer.substring(currentPosition);
  log.debug(`returning remaining buffer: ${currentJsonString.value}`)

  return {
    append: true,
    chunk: chunk,
    generatedInfo: {
      generatedQuiz: currentQuizData.value
    }
  }
}
const safeJsonParse = (jsonString) => {
  try {
    // First try to parse as is
    return JSON.parse(jsonString);
  } catch (e) {
    try {
      // If that fails, try to evaluate the string as JavaScript
      const evaluated = (0, eval)(`(${jsonString})`);
      // Convert back to string and parse to ensure it's valid JSON
      return JSON.parse(JSON.stringify(evaluated));
    } catch (innerError) {
      throw new Error(`error parsing question: ${jsonString}`, { cause: innerError})
    }
  }
}

const ensureValidQuestionType = (question) => {
  const correctAnswersCount = question.answers.reduce((count, answer) => {
    return answer.isCorrect ? count + 1 : count;
  }, 0);
  if (question.questionType !== 'MultipleChoice' && correctAnswersCount > 1) {
    log.debug(`Changing [${question.question}] from [${question.questionType}] to MultipleChoice, correctAnswersCount [${correctAnswersCount}]`)
    question.questionType = 'MultipleChoice'
  } else if (question.questionType !== 'SingleChoice' && correctAnswersCount === 1) {
    log.debug(`Changing [${question.question}] from [${question.questionType}] to SingleChoice, correctAnswersCount [${correctAnswersCount}]`)
    question.questionType = 'SingleChoice'
  }
}

const handleGenerationCompleted = (generated) => {
  log.debug(`handleGenerationCompleted: ${JSON.stringify(generated, null, 2)}`)
  currentJsonString.value = ''
  isGenerating.value = false
  allQuestions.value.push(...generated.generatedInfo.generatedQuiz)
  validate()
  return generated
}

const validationService = useDescriptionValidatorService()
const handleAddPrefix = (historyItem, missingPrefix) => {
  if (historyItem?.generatedInfo) {
    const promises = []
    historyItem.generatedInfo.generatedQuiz.forEach((q) => {
      promises.push(validationService.addPrefixToInvalidParagraphs(q.question, missingPrefix).then((result) => {
        q.question = result.newDescription
        setFieldValue(q.name, q.question)
        return q
      }))
    })
    return Promise.all(promises).then(() => {
      return historyItem
    })
  }
  return Promise.resolve(historyItem)
}

const createPromptInstructions = (userEnterInstructions) => {
  let instructionsToSend = ''
  if (currentDescription.value) {
    const extractedImagesRes = imgHandler.extractImages(currentDescription.value)
    const descriptionText = extractedImagesRes.hasImages ? extractedImagesRes.processedText : currentDescription.value

    if (currentQuizData.value) {
      const existingQuiz = JSON.stringify(currentQuizData.value)
      currentQuizData.value = null
      instructionsToSend = instructionsGenerator.updateQuizInstructions(descriptionText, existingQuiz, userEnterInstructions)
    } else {
      instructionsToSend = instructionsGenerator.newQuizInstructions(descriptionText, '5 - 10', userEnterInstructions)
    }
  }
  return instructionsToSend
}
const toggleEnableEditQuestions = ((id) => {
  enableEditQuestionsIds.value = enableEditQuestionsIds.value.includes(id) ? enableEditQuestionsIds.value.filter(item => item !== id) : [...enableEditQuestionsIds.value, id];
})
const handleQuestionUpdated = (updatedQuestion, historyItem) => {
  const { updatedQuestionText } = updatedQuestion;
  const question = historyItem.generatedInfo.generatedQuiz.find(q => q.name === updatedQuestion.question.name);
  question.question = updatedQuestionText;
}
</script>

<template>
  <ai-prompt-dialog
      ref="aiPromptDialogRef"
      v-model="model"
      :create-instructions-fn="createPromptInstructions"
      :chunk-handler-fn="handleGeneratedChunk"
      :generation-completed-fn="handleGenerationCompleted"
      :add-prefix-fn="handleAddPrefix"
      :generation-started-msg="'Generating a new quiz for this skill, please stand by...'"
      :use-generated-label="'Use Generated Quiz'"
      :instructions-placeholder="'Optional Additional Instructions Here'"
      :send-button-label="'Generate'"
      @use-generated="useGenerated"
      :community-value="communityValue"
      :is-valid="meta.valid"
      :overall-err-msg="overallErrMsg"
  >
    <template #generatedValue="{ historyItem }">
      <!--      must not use local variables as there can be more than 1 history item-->
      <div v-if="historyItem.generatedInfo" class="px-5 border rounded-lg bg-blue-50 ml-4">
        <div v-for="(q, index) in getQuizDataForDisplay(historyItem.generatedInfo, historyItem.id)" :key="q.id">
          <div class="my-4">
            <QuestionCard data-cy="generatedQuestion"
                          quiz-type="Quiz"
                          @question-updated="handleQuestionUpdated($event, historyItem)"
                          :question="q"
                          :question-num="index+1"
                          :show-edit-controls="false"
                          :supports-edit-question-inline="true"
                          :show-edit-question-inline="enableEditQuestionsIds.includes(historyItem.id)"/>
          </div>
        </div>
        <div class="flex justify-start items-center gap-3 my-2">
          <SkillsButton v-if="!!historyItem.finalMsg"
                        icon="fa-solid fa-check-double"
                        severity="info" :outlined="false"
                        label="Edit Questions"
                        data-cy="enabledEitQuestionsBtn"
                        @click="toggleEnableEditQuestions(historyItem.id)"/>
        </div>

      </div>
    </template>
  </ai-prompt-dialog>

</template>

<style scoped>

</style>
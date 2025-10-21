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
import { computed, ref } from 'vue'
import { useRoute } from 'vue-router'
import { useLog } from '@/components/utils/misc/useLog.js'
import { useImgHandler } from '@/common-components/utilities/learning-conent-gen/UseImgHandler.js'
import { useInstructionGenerator } from '@/common-components/utilities/learning-conent-gen/UseInstructionGenerator.js'
import QuestionCard from '@/components/quiz/testCreation/QuestionCard.vue'
import AiPromptDialog from '@/common-components/utilities/learning-conent-gen/AiPromptDialog.vue'
import SkillsSpinner from '@/components/utils/SkillsSpinner.vue'
import { useAppConfig } from '@/common-components/stores/UseAppConfig.js'
import { useDescriptionValidatorService } from '@/common-components/validators/UseDescriptionValidatorService.js'

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

const aiPromptDialogRef = ref(null)
const currentDescription = ref('')
const isGenerating = ref(true)

const currentQuizData = ref(null)

const instructionsGenerator = useInstructionGenerator()

const generateQuizFromDescription = (skillDescription) => {
  currentDescription.value = skillDescription
  aiPromptDialogRef.value.startGeneration()
}
defineExpose({
  generateQuizFromDescription
});

const useGenerated = (historyItem) => {
  emit('generated-quiz', historyItem.generatedInfo.generatedQuiz)
}
const getQuizDataForDisplay = (quizData, historyId) => {
  let quizDataForDisplay = null
  if (quizData && quizData.generatedQuiz) {
    quizDataForDisplay = quizData.generatedQuiz.map((question, questionIdx) => ({
      ...question,
      id: `${historyId}-${(questionIdx+1)}`,
      answers: question.answers.map(answer => ({
        ...answer,
        isSelected: answer.isCorrect
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

const currentJsonString = ref('')
const handleGeneratedChunk = (chunk) => {
  let newBuffer = currentJsonString.value + chunk;
  let currentPosition = 0;
  let inString = false;
  let inArray = false;
  let braceDepth = 0;
  let bracketDepth = 0;
  let startIndex = -1;
  let questionsStartIndex = -1;

  // Process the buffer to find complete objects
  for (let i = 0; i < newBuffer.length; i++) {
    const char = newBuffer[i];
    const prevChar = i > 0 ? newBuffer[i - 1] : '';

    // Track if we're inside a string (accounting for escaped quotes)
    if (char === '"' && prevChar !== '\\') {
      inString = !inString;
    }

    // Only process structure when not in a string
    if (!inString) {
      if (char === '{') {
        if (braceDepth === 0) {
          startIndex = i; // Start of a new object
        }
        braceDepth++;
      } else if (char === '}') {
        braceDepth--;
        const lastHistoryItem = { generatedQuiz: currentQuizData.value }
        const partialQuestionsExist = lastHistoryItem && lastHistoryItem.generatedQuiz && lastHistoryItem.generatedQuiz.length > 0;
        const isFirstQuestion = !partialQuestionsExist && inArray && bracketDepth === 1
        const isAdditionalQuestion = partialQuestionsExist && bracketDepth === 0 && !inArray

        if (isFirstQuestion || isAdditionalQuestion) {
          const questionStr = newBuffer.substring(isFirstQuestion ? questionsStartIndex + 1 : startIndex, i + 1);
          try {
            const question = JSON.parse(questionStr);
            if (lastHistoryItem && lastHistoryItem.generatedQuiz && lastHistoryItem.generatedQuiz.length > 0) {
              lastHistoryItem.generatedQuiz.push(question)
            } else {
              lastHistoryItem.generatedQuiz = [question]
            }
            currentQuizData.value = lastHistoryItem.generatedQuiz
            currentJsonString.value = ''
          } catch (e) {
            // Continue with partial parsing
            // console.log(`json: ${questionStr}`)
            console.error('Error parsing questions array:', e);
          }
          currentPosition = i + 1;
        } else if (braceDepth < 0) {
          braceDepth = 0; // Reset on error
        }
      } else if (char === '[') {
        if (bracketDepth === 0) {
          inArray = true;
          questionsStartIndex = i;
        }
        bracketDepth++;
      } else if (char === ']') {
        bracketDepth--;
        if (bracketDepth === 0) {
          inArray = false;
        }
      }
    }
  }

  // Return remaining buffer that couldn't be processed yet
  currentJsonString.value = newBuffer.substring(currentPosition);

  return {
    append: true,
    chunk: chunk,
    generatedInfo: {
      generatedQuiz: currentQuizData.value
    }
  }
}

const handleGenerationCompleted = (generated) => {
  currentJsonString.value = ''
  currentQuizData.value = null
  isGenerating.value = false
  return generated
}

const validationService = useDescriptionValidatorService()
const handleAddPrefix = (historyItem, missingPrefix) => {
  if (historyItem?.generatedInfo) {
    const promises = []
    historyItem.generatedInfo.generatedQuiz.forEach((q) => {
      promises.push(validationService.addPrefixToInvalidParagraphs(q.question, missingPrefix).then((result) => {
        q.question = result.newDescription
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

    // TODO = handle existing quiz and/or follow-on updates after initial generation
    if (currentQuizData.value) {
      instructionsToSend = instructionsGenerator.updateQuizInstructions(userEnterInstructions)
    } else {
      instructionsToSend = instructionsGenerator.newQuizInstructions(descriptionText, '5 - 10')
    }
  }
 return instructionsToSend
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
      @use-generated="useGenerated"
      :community-value="communityValue"
  >
    <template #generatedValue="{ historyItem }">
<!--      <markdown-text :text="historyItem.generatedValue"-->
<!--                     data-cy="generatedSegment"-->
<!--                     :instanceId="`${historyItem.id}-desc`"/>-->
      <!--      must not use local variables as there can be more than 1 history item-->
      <div v-if="historyItem.generatedInfo" class="px-5 border rounded-lg bg-blue-50 ml-4">
        <div v-for="(q, index) in getQuizDataForDisplay(historyItem.generatedInfo, historyItem.id)" :key="q.id">
          <div class="my-4">
            <QuestionCard :question="q" :question-num="index+1" quiz-type="Quiz" :show-edit-controls="false"/>
          </div>
        </div>
      </div>
      <div v-if="!!currentJsonString" class="flex mt-2">
        <skills-spinner :is-loading="!!currentJsonString" :size-in-rem="2"/>
      </div>
    </template>
  </ai-prompt-dialog>

</template>

<style scoped>

</style>
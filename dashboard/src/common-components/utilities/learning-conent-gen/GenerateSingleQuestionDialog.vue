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
import {computed, onMounted, ref} from 'vue'
import {useRoute} from "vue-router";
import MarkdownText from "@/common-components/utilities/markdown/MarkdownText.vue";
import {useLog} from "@/components/utils/misc/useLog.js";
import {useImgHandler} from "@/common-components/utilities/learning-conent-gen/UseImgHandler.js";
import {useInstructionGenerator} from "@/common-components/utilities/learning-conent-gen/UseInstructionGenerator.js";
import {useDescriptionValidatorService} from "@/common-components/validators/UseDescriptionValidatorService.js";
import {useAppConfig} from "@/common-components/stores/UseAppConfig.js";
import AiPromptDialog from "@/common-components/utilities/learning-conent-gen/AiPromptDialog.vue";
import SelectCorrectAnswer from "@/components/quiz/testCreation/SelectCorrectAnswer.vue";
import SkillsSpinner from "@/components/utils/SkillsSpinner.vue";
import {useQuizConfig} from '@/stores/UseQuizConfig.js'
import QuestionTypeDropDown from "@/components/quiz/testCreation/QuestionTypeDropDown.vue";
import QuestionType from "@/skills-display/components/quiz/QuestionType.js";

const model = defineModel()
const props = defineProps({
  questionType: Object,
  existingQuestion: Object,
  communityValue: {
    type: String,
    default: null
  },
})
const emit = defineEmits(['question-generated'])
const route = useRoute()
const log = useLog()
const imgHandler = useImgHandler()
const appConfig = useAppConfig()
const quizConfig = useQuizConfig()
const instructionsGenerator = useInstructionGenerator()

const extractedImageState = { hasImages: false, extractedImages: null, }

const aiPromptDialogRef = ref(null)
onMounted(() => {
  const welcomeMsg = props.existingQuestion ?
      'I noticed you\'ve already started and can help you refine and enhance! \n\nFor example, you could type `proofread` or `rewrite with more detail`. The more **specific** you are, the better I can assist you!'
      : 'Hi there! I\'m excited to help you craft a new question. Please share some `details` about what you have in mind.'

  aiPromptDialogRef.value.addWelcomeMsg(welcomeMsg)
  lastGeneratedQuestionInfo.value = props.existingQuestion
})

const createPromptInstructions = (userEnterInstructions) => {
  let instructionsToSend = ''
  if (lastGeneratedQuestionInfo.value) {
    const question = lastGeneratedQuestionInfo.value.question
    const extractedImagesRes = imgHandler.extractImages(question)
    lastGeneratedQuestionInfo.value.question = extractedImagesRes.hasImages ? extractedImagesRes.processedText : question
    const instructionsToKeepPlaceholders = extractedImagesRes.hasImages? imgHandler.instructionsToKeepPlaceholders() : ''
    instructionsToSend = instructionsGenerator.singleQuestionInstructions(userEnterInstructions, props.questionType.selectedType, lastGeneratedQuestionInfo.value, instructionsToKeepPlaceholders)
    if (extractedImagesRes.hasImages) {
      extractedImageState.extractedImages = extractedImagesRes.extractedImages
    }
  } else {
    instructionsToSend = instructionsGenerator.singleQuestionInstructions(userEnterInstructions, props.questionType.selectedType)
  }

  return instructionsToSend
}

const createFollowOnConvoInstructions = (userEnterInstructions) => {
  if (props.questionType.selectedType.id !== lastGeneratedQuestionInfo.value.questionTypeId) {
    return instructionsGenerator.updateSingleQuestionTypeChangedInstructions(userEnterInstructions, lastGeneratedQuestionInfo.value.questionTypeId, props.questionType.selectedType)
  }
  return instructionsGenerator.followOnConvoInstructions(userEnterInstructions)
}

const upToStartOfAnswers = ref('')
const answersFound = ref(false)
const answersString = ref('')
const questionsTitle = '### Question:'
const answersTitle = '### Answers:'
const lastGeneratedQuestionInfo = ref(null)
const isGenerating = ref(false)
const handleGeneratedChunk = (chunk) => {
  isGenerating.value = true
  let append = true
  let chunkToSend = chunk

  if (!answersFound.value) {
    upToStartOfAnswers.value += chunk
    const questionStarted = upToStartOfAnswers.value.indexOf(questionsTitle) !== -1

    // Check if we've found the answers section
    const answersIndex = upToStartOfAnswers.value.indexOf(answersTitle)
    if (answersIndex !== -1) {
      // Split at the start of answers
      const beforeAnswers = upToStartOfAnswers.value.substring(0, answersIndex)
      const afterAnswers = upToStartOfAnswers.value.substring(answersIndex)

      // Update values
      upToStartOfAnswers.value = beforeAnswers
      answersString.value = afterAnswers
      answersFound.value = true

      const lookFor = chunkToSend.indexOf(questionsTitle) !== -1 ? 'Answers:' : ':'
      const colonIndex = chunkToSend.indexOf(lookFor);
      chunkToSend = (questionStarted && colonIndex !== -1) ? chunkToSend.substring(0, colonIndex+lookFor.length+1).trim() : chunkToSend;
    }
  } else {
    // Once we've found answers, append all new chunks to answersString
    answersString.value += chunk
    append = false
  }

  return {
    append,
    chunk: chunkToSend
  }
}

const cleanJsonString = (jsonString) => {
  // Remove single-line comments (// ...)
  const res = jsonString.replace(/\/\/.*$/gm, '')
      // Remove multi-line comments (/* ... */)
      .replace(/\/\*[\s\S]*?\*\//g, '')
      // Remove any remaining whitespace
      .trim();

  return res
};

const handleGenerationCompleted = (generated) => {
  const questionMatch = /### Question:\s*([\s\S]+?)(?=\s*#+\s|$)/.exec(upToStartOfAnswers.value)
  if (!questionMatch) {
    throw new Error(`Invalid response format for question text=[${upToStartOfAnswers.value}]`);
  }
  let answersToParse = answersString.value
  let generateValueChangedNotes = null
  const [newText, comments] = answersString.value.split('Here is what was changed');
  if (newText && comments) {
    const cleanedComments = comments.replace(/^[\s:]+/, '').trim()
    answersToParse = newText.replace(/\s*#+\s*$/gm, '') // Remove ### at end of lines.trim()
    generateValueChangedNotes = `### Here is what was changed\n\n${cleanedComments}`
  }

  const answersMatch = /### Answers:([\s\S]+)/.exec(answersToParse);
  if (!answersMatch) {
    throw new Error(`Invalid response format for answers=[${answersToParse}]`);
  }
  try {
    const answers = QuestionType.isTextInput(props.questionType.selectedType?.id) ? [] : JSON.parse(cleanJsonString(answersMatch[1].trim()))

    const generatedInfo = {
      question: questionMatch[1].trim(),
      answers: answers,
      questionTypeId: props.questionType.selectedType?.id
    }
    lastGeneratedQuestionInfo.value = generatedInfo
    isGenerating.value = false
    return {
      generatedValue: generated.generatedValue,
      generateValueChangedNotes,
      generatedInfo
    }
  } catch (e) {
    console.error(e)
    throw new Error(`Failed to parse answers JSON from [${answersString.value}]`);
  } finally {
    answersFound.value = false
    answersString.value = ''
    upToStartOfAnswers.value = ''
  }
}

const useGenerated = (historyItem) => {
  emit('question-generated', historyItem.generatedInfo)
}

const validationService = useDescriptionValidatorService()
const handleAddPrefix = (historyItem, missingPrefix) => {
  if (historyItem?.generatedInfo?.question) {
    return validationService.addPrefixToInvalidParagraphs(historyItem?.generatedInfo?.question, missingPrefix).then((result) => {
      historyItem.generatedInfo.question = result.newDescription
      return historyItem
    })
  }

  return historyItem
}

const communityValue = computed(() => {
  let res = appConfig.defaultCommunityDescriptor
  if (props.allowCommunityElevation) {
    if (props.userCommunity) {
      res = props.userCommunity
    } else {
      if (route.params.quizId) {
        res = quizConfig.quizCommunityValue;
      }
    }
  }
  return res
})

const generationFailed = () => {
  answersFound.value = false
}

const questionTypeSelectionDisabled = computed(() => {
  return isGenerating.value || (appConfig.openAiDisableSingleQuestionTypeChange && !!lastGeneratedQuestionInfo.value);
})
</script>

<template>
  <ai-prompt-dialog
      ref="aiPromptDialogRef"
      v-model="model"
      :create-instructions-fn="createPromptInstructions"
      :create-follow-on-convo-instructions-fn="createFollowOnConvoInstructions"
      :chunk-handler-fn="handleGeneratedChunk"
      :generation-completed-fn="handleGenerationCompleted"
      :add-prefix-fn="handleAddPrefix"
      @use-generated="useGenerated"
      @generation-failed="generationFailed"
      :community-value="communityValue"
  >
    <template #aboveChatHistory>
      <div class="pb-5">
        <label>Question Type:</label>
        <question-type-drop-down
            name="genQuestionType"
            data-cy="genQuestionTypeSelector"
            v-model="questionType.selectedType"
            :options="questionType.options"
            :ignore-vee-validate="true"
            :disabled="questionTypeSelectionDisabled"
        />
      </div>
    </template>
    <template #generatedValue="{ historyItem }">
      <markdown-text :text="historyItem.generatedValue"
                     data-cy="generatedSegment"
                     :instanceId="`${historyItem.id}-desc`"/>
      <div v-if="answersFound" class="flex mt-2">
        <skills-spinner :is-loading="answersFound" :size-in-rem="2"/>
      </div>
      <!--      must not use local variables as there can be more than 1 history item-->
      <div v-if="historyItem.generatedInfo?.answers" class="mt-2 flex flex-col gap-2" data-cy="generatedAnswers">
        <div v-if="QuestionType.isTextInput(historyItem.generatedInfo?.questionTypeId)">
            <Textarea
                style="resize: none"
                class="w-full"
                placeholder="Users will be required to enter text."
                disabled
                aria-hidden="true"
                data-cy="textAreaPlaceHolder"
                rows="2"/>
        </div>
        <div v-else-if="QuestionType.isMatching(historyItem.generatedInfo?.questionTypeId)">
          <div v-for="(answer, index) in historyItem.generatedInfo?.answers" :key="index">
            <div v-if="answer.multiPartAnswer" class="flex flex-row gap-3" :data-cy="`answer-${index}`">
              <div :data-cy="`answer-${index}-term`">
                {{ answer.multiPartAnswer.term }}
              </div>
              <div>
                <i class="fas fa-arrow-right text-gray-500 dark:text-gray-400" aria-hidden="true"></i>
              </div>
              <div :data-cy="`answer-${index}-value`">
                {{ answer.multiPartAnswer.value }}
              </div>
            </div>
            <hr v-if="index < historyItem.generatedInfo?.answers.length - 1"
                class="mt-3 border-t border-dashed border-gray-300 dark:border-gray-600"/>
          </div>
        </div>
        <div v-else  v-for="(answer, index) in historyItem.generatedInfo?.answers" :key="index" class="flex gap-1 items-start">
          <select-correct-answer
              v-model="answer.isCorrect"
              :read-only="true"
              :is-radio-icon="QuestionType.isSingleChoice(historyItem.generatedInfo?.questionTypeId)"
              font-size="1.5rem"
              :name="`ans${index}`"/>
          <div>{{ answer.answer }}</div>
        </div>
      </div>
    </template>
  </ai-prompt-dialog>
</template>

<style scoped>

</style>
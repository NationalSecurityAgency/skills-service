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
import { nextTick, ref } from 'vue'
import SkillsDialog from '@/components/utils/inputForm/SkillsDialog.vue'
import LearningGenService from '@/common-components/utilities/learning-conent-gen/LearningGenService.js'
import { useRoute } from 'vue-router'
import MarkdownText from '@/common-components/utilities/markdown/MarkdownText.vue'
import AssistantMsg from '@/common-components/utilities/learning-conent-gen/AssistantMsg.vue'
import UserMsg from '@/common-components/utilities/learning-conent-gen/UserMsg.vue'
import { useLog } from '@/components/utils/misc/useLog.js'
import { useImgHandler } from '@/common-components/utilities/learning-conent-gen/UseImgHandler.js'
import { useInstructionGenerator } from '@/common-components/utilities/learning-conent-gen/UseInstructionGenerator.js'
import QuestionCard from '@/components/quiz/testCreation/QuestionCard.vue'

const model = defineModel()
const emit = defineEmits(['generated-quiz'])
const route = useRoute()
const log = useLog()
const imgHandler = useImgHandler()
const close = () => {
  model.value = false
}

const instructions = ref('')
const currentDescription = ref('')
const loadingExistingQuizQuestions = ref(false)
const isGenerating = ref(false)

const extractedImageState = { hasImages: false, extractedImages: null, }
const chatCounter = ref(0)
const chatHistory = ref([])
const ChatRole = {
  USER: 'user',
  ASSISTANT: 'assistant',
}
const addChatItem = (origMsg, role = ChatRole.ASSISTANT, isGenerating = false) => {
  const res = {
    id: `${chatCounter.value++}`,
    role,
    origMessage: origMsg,
    isGenerating,
    generatedValue: '',
    generatedQuiz: null,
    finalMsg: '',
    failedToGenerate: false
  }
  chatHistory.value.push(res)
  return res
}
const appendGeneratedToLastChatItem = (toAppend) => {
  const historyItem = chatHistory.value[chatHistory.value.length - 1]
  historyItem.generatedValue += toAppend
  try {
    if (currentQuizData.value) {
      historyItem.generatedQuiz = currentQuizData.value
    }
  } catch (e) {
    console.error(`Failed to parse json respond due to: ${e.message}`, toAppend);
  }
}
const setFinalMsgToLastChatItem = (finalMsg, failedToGenerate = false) => {
  const historyItem = chatHistory.value[chatHistory.value.length - 1]
  historyItem.finalMsg = finalMsg
  historyItem.isGenerating = false
  historyItem.failedToGenerate = failedToGenerate
}

const instructionsGenerator = useInstructionGenerator()
const reviewDescMsg = 'I\'ve prepared a quiz based on the description of this skill. Review the quiz and let me know if you need any changes.'
const failedToGenerateMsg = 'I apologize, but I was unable to generate a quiz at this time. Please try again in a few moments.'
function parseResponse(text) {
  const [newText, comments] = text.split('Here is what was changed');
  
  // Clean up the newText by removing training ### and extra spaces
  const cleanedText = newText
    .replace(/\s*###+\s*$/gm, '') // Remove ### at end of lines
    .trim();
    
  return {
    newText: cleanedText,
    comments
  };
}

const shouldStream = ref(true)
const instructionsInput = ref(null)
const generateQuiz = () => {

  if (log.isTraceEnabled()) {
    log.trace(`Generating based on instructions: [${instructions.value}]`)
  }

  let instructionsToSend = null
  const userInstructions = instructions.value
  if (userInstructions) {
    addChatItem(instructions.value, ChatRole.USER)
  }

  if (currentDescription.value) {
    const extractedImagesRes = imgHandler.extractImages(currentDescription.value)
    const descriptionText = extractedImagesRes.hasImages ? extractedImagesRes.processedText : currentDescription.value

    // TODO = handle existing quiz and/or follow-on updates after initial generation
    if (currentQuizData.value) {
      instructionsToSend = instructionsGenerator.updateQuizInstructions(userInstructions)
    } else {
      instructionsToSend = instructionsGenerator.newQuizInstructions(descriptionText, '5 - 10')
    }
  }
  instructions.value = ''

  if (shouldStream.value) {
    isGenerating.value = true
    addChatItem('Got it! I\'ll get started right away!', ChatRole.ASSISTANT, true)
    generateQuizWithStreaming(instructionsToSend, extractedImageState)
  } else {
    isGenerating.value = true
    addChatItem('Generating a new quiz for this skill, please stand by...', ChatRole.ASSISTANT, true)
    scrollInstructionsIntoView()
    generateQuizWithoutStreaming(instructionsToSend, extractedImageState)
  }
}

const generateQuizFromDescription = (skillDescription) => {
  currentDescription.value = skillDescription
  generateQuiz()
}
defineExpose({
  generateQuizFromDescription
});

const scrollInstructionsIntoView = () => {
  document.getElementById('instructionsInput')?.scrollIntoView()
}
const currentQuizData = ref(null)
const checkThatProgressWasMade = () => {
  const TIMEOUT_MS = 5000;
  const MAX_ATTEMPTS = 6; // Max 30 seconds (5s * 6)
  let numAttempts = 0;
  const statusMessages = [
    "This is taking longer than expected but I am still working on it...",
    "Still working on generating the best response for you...",
    "Hang tight! Still processing your request...",
    "I am trying but unfortunately it is still taking way longer than expected...",
    "I am still trying, sorry for the delay!",
    "I may not be able to generate a quiz at this time. But I'll try my best to get it done."
  ];
  const lastItem = chatHistory.value[chatHistory.value.length - 1];
  const initialLength = lastItem.generatedValue.length;

  setTimeout(() => {
    if (numAttempts < MAX_ATTEMPTS && lastItem.generatedValue.length === initialLength) {
      lastItem.origMessage += ` \n${statusMessages[numAttempts]}`
      numAttempts += 1
    }
  }, TIMEOUT_MS)
}

const generateQuizWithStreaming = (instructionsToSend) => {
  checkThatProgressWasMade();
  let buffer = '';
  let fullBuffer = '';

  return LearningGenService.generateDescriptionStreamWithFetch(
      route.params.projectId,
      instructionsToSend,
      (chunk) => {
        buffer = parseQuizStream(
            chunk,
            buffer,
            // onProgress
            (question) => {
              if (question) {
                // Add to chat or update UI
                const lastHistoryItem = chatHistory.value[chatHistory.value.length - 1]
                if (lastHistoryItem && lastHistoryItem.generatedQuiz && lastHistoryItem.generatedQuiz.length > 0) {
                  lastHistoryItem.generatedQuiz.push(question)
                } else {
                  lastHistoryItem.generatedQuiz = [question]
                }
              }
            },
            // onComplete
            (completeData) => {
            }
        );
        fullBuffer += chunk;
        scrollInstructionsIntoView();
      },
      // onComplete
      () => {
        setFinalMsgToLastChatItem(reviewDescMsg);
        isGenerating.value = false;
        focusOnInstructionsInput();
        // Store complete quiz data
        const lastHistoryItem = chatHistory.value[chatHistory.value.length - 1]
        if (lastHistoryItem) {
          lastHistoryItem.generatedValue = fullBuffer;
          if (lastHistoryItem.generatedQuiz && lastHistoryItem.generatedQuiz.length > 0) {
            currentQuizData.value = lastHistoryItem.generatedQuiz;
          }
        }
      },
      // onError
      (error) => {
        log.error(`Failed to generate quiz: ${error}`);
        isGenerating.value = false;
        setFinalMsgToLastChatItem(failedToGenerateMsg, true);
      }
  );
};

function parseQuizStream(chunk, buffer, onProgress, onComplete) {
  let newBuffer = buffer + chunk;
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
        const lastHistoryItem = chatHistory.value[chatHistory.value.length - 1]
        const partialQuestionsExist = lastHistoryItem && lastHistoryItem.generatedQuiz && lastHistoryItem.generatedQuiz.length > 0;
        const isFirstQuestion = !partialQuestionsExist && inArray && bracketDepth === 1
        const isAdditionalQuestion = partialQuestionsExist && bracketDepth === 0 && !inArray

        if (isFirstQuestion || isAdditionalQuestion) {
          const questionStr = newBuffer.substring(isFirstQuestion ? questionsStartIndex + 1 : startIndex, i + 1);
          try {
            const question = JSON.parse(questionStr);
            onProgress(question);
          } catch (e) {
            // Continue with partial parsing
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
  //
  // // Try to parse the entire buffer as complete JSON
  // if (currentPosition === 0) {
  //   try {
  //     const completeData = JSON.parse(newBuffer);
  //     if (completeData.questions && Array.isArray(completeData.questions)) {
  //       onComplete(completeData);
  //       return ''; // Clear buffer
  //     }
  //   } catch (e) {
  //     // Not a complete JSON, continue with partial processing
  //   }
  // }

  // Return remaining buffer that couldn't be processed yet
  return newBuffer.substring(currentPosition);
}

const generateQuizWithoutStreaming = (instructionsToSend) => {
  return LearningGenService.generateDescription(route.params.projectId, instructionsToSend)
      .then((response) => {
        currentQuizData.value = extractJsonArray(response.description);
        appendGeneratedToLastChatItem(response.description)
        setFinalMsgToLastChatItem(reviewDescMsg)
      }).finally(() => {
        isGenerating.value = false
        focusOnInstructionsInput()
      }).catch((err) => {
        isGenerating.value = false
        log.error(err)
        addChatItem(failedToGenerateMsg)
      })
}

const getQuizDataForDisplay = (quizData, historyId) => {
  let quizDataForDisplay = null
  if (quizData) {
    quizDataForDisplay = quizData.map((question, questionIdx) => ({
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
const focusOnInstructionsInput = () => {
  nextTick(() => {
    document.getElementById('instructionsInput')?.focus()
  })
}

const useGeneratedQuiz = (historyId) => {
  const historyItem = chatHistory.value.find(item => item.id === historyId)
  if (historyItem?.generatedQuiz) {
    emit('generated-quiz', historyItem.generatedQuiz)
  }
  close()
}

const extractJsonArray = (text) => {
  const start = text.indexOf('[');
  const end = text.lastIndexOf(']') + 1;

  if (start === -1 || end === 0) {
    throw new Error('No JSON array found in the text');
  }

  const jsonString = text.slice(start, end);
  try {
    return JSON.parse(jsonString);
  } catch (e) {
    throw new Error('Failed to parse JSON: ' + e.message);
  }
}

</script>

<template>
  <SkillsDialog
      ref="generateQuizDialog"
      :loading="loadingExistingQuizQuestions"
      :maximizable="true"
      :maximized="true"
      v-model="model"
      header="Quiz Assistant"
      :show-ok-button="false"
      :show-cancel-button="false"
      :enable-return-focus="true">

    <div class="flex justify-end gap-2">
      Streaming: <ToggleSwitch v-model="shouldStream" />
    </div>
    <div class="py-5" style="min-height: 70vh">
      <div id="chatHistory" class="flex flex-col gap-3 mb-2">
        <div v-for="(historyItem) in chatHistory" :key="historyItem.id">
          <div v-if="historyItem.role === ChatRole.USER" class="relative flex justify-end">
            <user-msg>
              <markdown-text :text="historyItem.origMessage" :instanceId="historyItem.id"/>
            </user-msg>
          </div>
          <assistant-msg v-if="historyItem.role === ChatRole.ASSISTANT" class="flex flex-col gap-2">
            <div class="flex gap-2 items-center">
              <markdown-text :text="historyItem.origMessage" :instanceId="`${historyItem.id}-content`"/>
              <skills-spinner v-if="historyItem.isGenerating" :id="`${historyItem.id}-spinner`" :is-loading="true" :size-in-rem="0.8"/>
            </div>
            <div v-if="historyItem.generatedQuiz" class="px-5 border rounded-lg bg-blue-50 ml-4">
<!--              <markdown-text :text="historyItem.generatedValue" :instanceId="`${historyItem.id}-desc`"/>-->
              <div v-for="(q, index) in getQuizDataForDisplay(historyItem.generatedQuiz, historyItem.id)" :key="q.id">
                <div class="mt-4">
                  <QuestionCard :question="q" :question-num="index+1" quiz-type="Quiz" :show-edit-controls="false"/>
                </div>
              </div>
            </div>
            <div v-if="historyItem.finalMsg">
              <markdown-text :text="historyItem.finalMsg" :instanceId="`${historyItem.id}-finalMsg`"/>
              <div class="flex justify-start mt-2">
                <SkillsButton
                    v-if="!historyItem.failedToGenerate"
                    icon="fa-solid fa-check-double"
                    severity="info" :outlined="false"
                    label="Use Generated Quiz"
                    @click="useGeneratedQuiz(historyItem.id)"/>
              </div>
            </div>
          </assistant-msg>
        </div>
      </div>

      <div class="flex justify-end mt-6">
        <div class="flex gap-2 w-10/12">
          <InputText
              id="instructionsInput"
              ref="instructionsInput"
              v-model="instructions"
              class="w-full"
              placeholder="Type Instructions Here"
              :disabled="isGenerating"
              @keydown.enter="generateQuiz"
              autofocus/>
          <div class="flex justify-end">
            <SkillsButton icon="fa-solid fa-play" label="Send" @click="generateQuiz" :disabled="isGenerating"/>
          </div>
        </div>
      </div>

    </div>
  </SkillsDialog>
</template>

<style scoped>

</style>
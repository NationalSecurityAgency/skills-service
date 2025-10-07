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
import {nextTick, onMounted, ref, watch} from 'vue'
import SkillsDialog from "@/components/utils/inputForm/SkillsDialog.vue";
import LearningGenService from "@/common-components/utilities/learning-conent-gen/LearningGenService.js";
import {useRoute} from "vue-router";
import MarkdownText from "@/common-components/utilities/markdown/MarkdownText.vue";
import AssistantMsg from "@/common-components/utilities/learning-conent-gen/AssistantMsg.vue";
import UserMsg from "@/common-components/utilities/learning-conent-gen/UserMsg.vue";
import {useLog} from "@/components/utils/misc/useLog.js";
import {useImgHandler} from "@/common-components/utilities/learning-conent-gen/UseImgHandler.js";
import {useInstructionGenerator} from "@/common-components/utilities/learning-conent-gen/UseInstructionGenerator.js";
import PrefixControls from "@/common-components/utilities/markdown/PrefixControls.vue";
import {useDescriptionValidatorService} from "@/common-components/validators/UseDescriptionValidatorService.js";

const model = defineModel()
const emit = defineEmits(['generated-desc'])
const route = useRoute()
const log = useLog()
const imgHandler = useImgHandler()
const updateDescription = (newDesc) => {
  currentDescription.value = newDesc
  if (newDesc) {
    chatHistory.value.push({
      id: `${chatCounter.value++}`,
      role: 'assistant',
      origMessage: 'Hope your day is going well! Looks like you already started on a description. Would you like me to proofread or expand on it?'
    })
  } else {
    chatHistory.value.push({
      id: `${chatCounter.value++}`,
      role: 'assistant',
      origMessage: 'Hi! I am here to help. Describe the skill, and I\'ll help generate a description'
    })
  }
}
defineExpose({
  updateDescription
});

const close = () => {
  model.value = false
}

const instructions = ref('')
const currentDescription = ref('')
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
    finalMsg: '',
    failedToGenerate: false
  }
  chatHistory.value.push(res)
  return res
}
const appendGeneratedToLastChatItem = (toAppend) => {
  const historyItem = chatHistory.value[chatHistory.value.length - 1]
  historyItem.generatedValue += toAppend
  if (extractedImageState.extractedImages) {
    const {
      text,
      unusedImages
    } = imgHandler.reinsertImages(historyItem.generatedValue, extractedImageState.extractedImages)
    historyItem.generatedValue = text
    extractedImageState.extractedImages = unusedImages
  }
  currentDescription.value = historyItem.generatedValue
}
const setFinalMsgToLastChatItem = (finalMsg, failedToGenerate = false) => {
  const historyItem = chatHistory.value[chatHistory.value.length - 1]
  historyItem.finalMsg = finalMsg
  historyItem.isGenerating = false
  historyItem.failedToGenerate = failedToGenerate
}

const instructionsGenerator = useInstructionGenerator()
const reviewDescMsg = 'I\'ve prepared a description based on your input. Review the description and let me know if you need any changes.'
const failedToGenerateMsg = 'I apologize, but I was unable to generate a description at this time. Please try again in a few moments.'
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
const generateDescription = () => {

  if (log.isTraceEnabled()) {
    log.trace(`Generating based on instructions: [${instructions.value}]`)
  }

  addChatItem(instructions.value, ChatRole.USER)

  let instructionsToSend = null
  const userInstructions = instructions.value

  if (currentDescription.value) {
    const extractedImagesRes = imgHandler.extractImages(currentDescription.value)
    const descriptionText = extractedImagesRes.hasImages ? extractedImagesRes.processedText : currentDescription.value
    const instructionsToKeepPlaceholders = extractedImagesRes.hasImages? imgHandler.instructionsToKeepPlaceholders() : ''
    instructionsToSend = instructionsGenerator.existingDescriptionInstructions(descriptionText, userInstructions, instructionsToKeepPlaceholders)
    if (extractedImagesRes.hasImages) {
      extractedImageState.extractedImages = extractedImagesRes.extractedImages
    }
  } else {
    instructionsToSend = instructionsGenerator.newDescriptionInstructions(userInstructions)
  }
  instructions.value = ''

  if (shouldStream.value) {
    isGenerating.value = true
    addChatItem('Got it! I\'ll get started right away!', ChatRole.ASSISTANT, true)
    generateDescriptionWithStreaming(instructionsToSend, extractedImageState)
  } else {
    isGenerating.value = true
    addChatItem('Got it! I am now working on it...', ChatRole.ASSISTANT, true)
    scrollInstructionsIntoView()
    generateDescriptionWithoutStreaming(instructionsToSend, extractedImageState)
  }
}

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
    "I may not be able to generate a description at this time. But I'll try my best to get it done."
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
const generateDescriptionWithStreaming = (instructionsToSend) => {
  checkThatProgressWasMade()
  return LearningGenService.generateDescriptionStreamWithFetch(route.params.projectId, instructionsToSend,
      (chunk) => {
        appendGeneratedToLastChatItem(chunk)
        scrollInstructionsIntoView()
      },
      (completeData) => {
        setFinalMsgToLastChatItem(reviewDescMsg)
        isGenerating.value = false
        focusOnInstructionsInput()
      },
      (error) => {
        log.error(`Failed to generate description via streaming: ${error}`)
        isGenerating.value = false
        setFinalMsgToLastChatItem(failedToGenerateMsg, true)
      })
}

const scrollInstructionsIntoView = () => {
  document.getElementById('instructionsInput')?.scrollIntoView()
}
const generateDescriptionWithoutStreaming = (instructionsToSend) => {
  return LearningGenService.generateDescription(route.params.projectId, instructionsToSend)
      .then((response) => {
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

const focusOnInstructionsInput = () => {
  nextTick(() => {
    document.getElementById('instructionsInput')?.focus()
  })
}

const useGeneratedDescription = (historyId) => {
  const historyItem = chatHistory.value.find(item => item.id === historyId)
  if (historyItem?.generatedValue) {
    const { newText } = parseResponse(historyItem.generatedValue)
    emit('generated-desc', newText || historyItem.generatedValue)
  }
  close()
}

const validationService = useDescriptionValidatorService()
const isAddingPrefix = ref(false)
const addPrefixThenUseDesc = (info) => {
  isAddingPrefix.value = true
  const historyId = info.id
  const missingPrefix = info.prefix
  console.log(`prefixThenUseGeneratedDescription: ${historyId}, ${missingPrefix}`)
  const historyItem = chatHistory.value.find(item => item.id === historyId)
  if (historyItem?.generatedValue) {
    return validationService.addPrefixToInvalidParagraphs(historyItem?.generatedValue, missingPrefix).then((result) => {
      const { newText } = parseResponse(result.newDescription)
      emit('generated-desc', newText || result.newDescription)
      close()
    }).finally(() => {
      isAddingPrefix.value = false
    })
  }
}

</script>

<template>
  <SkillsDialog
      ref="generateDescriptionDialog"
      :maximizable="true"
      :maximized="true"
      v-model="model"
      header="Description Assistant"
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
            <div v-if="historyItem.generatedValue" class="px-5 border rounded-lg bg-blue-50 ml-4">
              <markdown-text :text="historyItem.generatedValue" :instanceId="`${historyItem.id}-desc`"/>
            </div>
            <div v-if="historyItem.finalMsg">
              <markdown-text :text="historyItem.finalMsg" :instanceId="`${historyItem.id}-finalMsg`"/>
              <div class="flex justify-start items-center gap-3 mt-2">
                <SkillsButton
                    v-if="!historyItem.failedToGenerate"
                    icon="fa-solid fa-check-double"
                    severity="info" :outlined="false"
                    label="Use Generated Value"
                    :loading="isAddingPrefix"
                    @click="useGeneratedDescription(historyItem.id)"/>

                OR

                <prefix-controls
                    @add-prefix="addPrefixThenUseDesc"
                    :is-loading="isAddingPrefix"
                    :id="historyItem.id"
                    size="normal"
                    :outlined-buttons="false"
                    buttons-severity="info"
                    add-button-label-conf-prop="addPrefixToGeneratedValueBtnLabel"/>
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
              @keydown.enter="generateDescription"
              autofocus/>
          <div class="flex justify-end">
            <SkillsButton icon="fa-solid fa-play" label="Send" @click="generateDescription" :disabled="isGenerating"/>
          </div>
        </div>
      </div>
    </div>
  </SkillsDialog>
</template>

<style scoped>

</style>
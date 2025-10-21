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
import {nextTick, ref} from 'vue'
import SkillsDialog from "@/components/utils/inputForm/SkillsDialog.vue";
import {useRoute} from "vue-router";
import MarkdownText from "@/common-components/utilities/markdown/MarkdownText.vue";
import AssistantMsg from "@/common-components/utilities/learning-conent-gen/AssistantMsg.vue";
import UserMsg from "@/common-components/utilities/learning-conent-gen/UserMsg.vue";
import {useLog} from "@/components/utils/misc/useLog.js";
import PrefixControls from "@/common-components/utilities/markdown/PrefixControls.vue";
import {useAppConfig} from "@/common-components/stores/UseAppConfig.js";
import {useOpenaiService} from "@/common-components/utilities/learning-conent-gen/UseOpenaiService.js";

const model = defineModel()
const props = defineProps({
  createInstructionsFn: {
    type: Function,
    required: true
  },
  chunkHandlerFn: {
    type: Function,
    required: true
  },
  generationCompletedFn: {
    type: Function,
    required: true
  },
  addPrefixFn: {
    type: Function,
    default: null,
  },
  communityValue: {
    type: String,
    default: null
  },
  generationCompletedMsg: {
    type: String,
    default: 'Take a look at what I came up with! Please review it and let me know if you need any changes.'
  },
  failedToGenerateMsg: {
    type: String,
    default: 'I apologize, but I couldn\'t process your request at this time. Please try again shortly.'
  },
  generateCancelledMsg: {
    type: String,
    default: 'Successfully stopped. Feel free to try again when you\'re ready.'
  },
  generationStartedMsg: {
    type: String,
    default: 'Got it! I\'ll get started right away!'
  },
})

const emit = defineEmits(['use-generated'])
const route = useRoute()
const log = useLog()
const appConfig = useAppConfig()
const openaiService = useOpenaiService()

const addWelcomeMsg = (welcomeMsg) => {
  chatHistory.value.push({
    id: `${chatCounter.value++}`,
    role: 'assistant',
    origMessage: welcomeMsg
  })
}
const startGeneration = () => {
  if (log.isTraceEnabled()) {
    log.trace(`Generating based on instructions: [${instructions.value}]`)
  }
  addChatItem(instructions.value, ChatRole.USER)

  const promptInstructions = props.createInstructionsFn(instructions.value)
  instructions.value = ''

  isGenerating.value = true
  addChatItem(props.generationStartedMsg, ChatRole.ASSISTANT, true)
  genWithStreaming(promptInstructions)
}
defineExpose({
  addWelcomeMsg,
  startGeneration
});

const close = () => {
  model.value = false
}

const isAddingPrefix = ref(false)
const instructions = ref('')
const isGenerating = ref(false)
const chatCounter = ref(0)
const chatHistory = ref([])

const ChatRole = {
  USER: 'user',
  ASSISTANT: 'assistant',
}
const getLastChatItem = () => {
  return chatHistory.value[chatHistory.value.length - 1]
}
const addChatItem = (origMsg, role = ChatRole.ASSISTANT, isGenerating = false) => {
  const res = {
    id: `${chatCounter.value++}`,
    role,
    origMessage: origMsg,
    isGenerating,
    generatedValue: '',
    generateValueChangedNotes: '',
    generatedInfo: {}, // arbitrary object for clients of this component
    finalMsg: '',
    failedToGenerate: false,
    cancelled: false,
  }
  chatHistory.value.push(res)
  return res
}
const appendGeneratedToLastChatItem = (toAppend) => {
  const historyItem = getLastChatItem()
  historyItem.generatedValue += toAppend
}
const setFinalMsgToLastChatItem = (finalMsg, failedToGenerate = false, cancelled = false) => {
  const historyItem = getLastChatItem()
  historyItem.finalMsg = finalMsg
  historyItem.isGenerating = false
  historyItem.failedToGenerate = failedToGenerate
  historyItem.cancelled = cancelled
}
const updateLastChatItemGeneratedValue = (item) => {
  const toUpdate = getLastChatItem()
  toUpdate.generatedValue = item.generatedValue
  toUpdate.generateValueChangedNotes = item.generateValueChangedNotes
  toUpdate.generatedInfo = item.generatedInfo
}

const onStartStopBtn = () => {
  if (isGenerating.value) {
    cancelCurrentPrompt()
  } else {
    startGeneration()
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
  const lastItem = getLastChatItem()
  const initialLength = lastItem.generatedValue.length;

  const checkProgress = () => {
    setTimeout(() => {
      if (isGenerating.value && numAttempts < MAX_ATTEMPTS && lastItem.generatedValue.length === initialLength) {
        lastItem.origMessage += ` \n${statusMessages[numAttempts]}`
        numAttempts += 1
        log.info(`GenerateDescriptionDialog: Checking progress attempt=[${numAttempts}]`)
        checkProgress()
      }
    }, TIMEOUT_MS)
  }
  checkProgress()
}

const genWithStreaming = (instructionsToSend) => {
  lastPromptCancelled.value = false
  checkThatProgressWasMade()
  scrollInstructionsIntoView()
  return openaiService.prompt(instructionsToSend,
      (chunk) => {
        const chunkRes = props.chunkHandlerFn(chunk)
        if (chunkRes.append) {
          appendGeneratedToLastChatItem(chunkRes.chunk)
        }
        if (chunkRes.generatedInfo) {
          const toUpdate = getLastChatItem()
          toUpdate.generatedInfo = chunkRes.generatedInfo
        }
        scrollInstructionsIntoView()
      },
      () => {
        const updatedHistoryItem = props.generationCompletedFn(getLastChatItem())
        updateLastChatItemGeneratedValue(updatedHistoryItem)
        setFinalMsgToLastChatItem(props.generationCompletedMsg)
        isGenerating.value = false
        focusOnInstructionsInput()
      },
      (error) => {
        isGenerating.value = false
        if (lastPromptCancelled.value) {
          setFinalMsgToLastChatItem(props.generateCancelledMsg, false, true)
        } else {
          log.error(`Failed to generate description via streaming: ${error}`)
          setFinalMsgToLastChatItem(props.failedToGenerateMsg, true)
        }
        focusOnInstructionsInput()
      })
}
const lastPromptCancelled = ref(false)
const cancelCurrentPrompt = () => {
  lastPromptCancelled.value = true
  openaiService.cancelCurrentPrompt()
}

const scrollInstructionsIntoView = () => {
  nextTick(() => document.getElementById('instructionsInputId')?.scrollIntoView())
}
const focusOnInstructionsInput = () => {
  nextTick(() => document.getElementById('instructionsInputId')?.focus())
}

const useGenerated = (historyId) => {
  const historyItem = chatHistory.value.find(item => item.id === historyId)
  emit('use-generated', historyItem)
  close()
}

const addPrefix = (info) => {
  isAddingPrefix.value = true
  const historyId = info.id
  const missingPrefix = info.prefix
  const historyItem = chatHistory.value.find(item => item.id === historyId)
  if (historyItem?.generatedValue) {
    return props.addPrefixFn(historyItem, missingPrefix).then((result) => {
      historyItem.generatedValue = result.generatedValue
      useGenerated(historyItem.id)
      isAddingPrefix.value = false
    })
  }
}

const finalMsgSeverity = (historyItem) => historyItem.failedToGenerate ? 'error' : historyItem.cancelled ? 'warn' : 'info'
</script>

<template>
  <SkillsDialog
      ref="generateDescriptionDialog"
      :maximizable="true"
      :maximized="true"
      v-model="model"
      header="AI Assistant"
      :show-ok-button="false"
      :show-cancel-button="false"
      :enable-return-focus="true">

    <div class="py-5 flex flex-col" style="min-height: 70vh">
      <div id="chatHistory"
           :class="{ 'flex-1': chatHistory.length > 1 }"
           class="flex flex-col gap-3 mb-2">
        <div v-for="(historyItem) in chatHistory" :key="historyItem.id">
          <div v-if="historyItem.role === ChatRole.USER" class="relative flex justify-end">
            <user-msg :id="historyItem.id">
              <markdown-text :text="historyItem.origMessage" :instanceId="historyItem.id"/>
            </user-msg>
          </div>
          <assistant-msg v-if="historyItem.role === ChatRole.ASSISTANT" class="flex flex-col gap-2" :id="historyItem.id" :is-generating="historyItem.isGenerating">
            <div class="flex gap-2 items-center" data-cy="origSegment">
              <markdown-text :text="historyItem.origMessage" :instanceId="`${historyItem.id}-content`"/>
            </div>
            <div v-if="historyItem.generatedValue" class="px-5 border rounded-lg bg-blue-50">
              <slot name="generatedValue" :historyItem="historyItem" data-cy="generatedSegment"/>
              <div v-if="historyItem.generateValueChangedNotes"
                   data-cy="generatedSegmentNotes"
                   class="border-t-2 border-dotted border-blue-200 p-0 mt-5">
               <markdown-text :text="historyItem.generateValueChangedNotes"
                             :instanceId="`${historyItem.id}-generateValueChangedNotes`"/>
              </div>
            </div>
            <div v-if="historyItem.finalMsg" data-cy="finalSegment">
              <Message :closable="false" :severity="finalMsgSeverity(historyItem)">
                <markdown-text :text="historyItem.finalMsg" :instanceId="`${historyItem.id}-finalMsg`"/>
              </Message>
              <div v-if="!historyItem.failedToGenerate && !historyItem.cancelled"
                   class="flex justify-start items-center gap-3 mt-2">
                <SkillsButton
                    icon="fa-solid fa-check-double"
                    severity="info" :outlined="false"
                    label="Use Generated Value"
                    :data-cy="`useGenValueBtn-${historyItem.id}`"
                    :loading="isAddingPrefix"
                    @click="useGenerated(historyItem.id)"/>

                <span v-if="appConfig.addPrefixToInvalidParagraphsOptions && props.addPrefixFn !== null">OR</span>

                <prefix-controls
                    v-if="props.addPrefixFn !== null"
                    @add-prefix="addPrefix"
                    :is-loading="isAddingPrefix"
                    :id="historyItem.id"
                    size="normal"
                    :outlined-buttons="false"
                    buttons-severity="info"
                    :community-value="communityValue"
                    add-button-label-conf-prop="addPrefixToGeneratedValueBtnLabel"/>
              </div>
            </div>
          </assistant-msg>
        </div>
      </div>

      <div class="flex justify-end mt-6">
        <div class="flex gap-2 w-10/12">
          <InputText
              id="instructionsInputId"
              v-model="instructions"
              class="w-full"
              placeholder="Type Instructions Here"
              :disabled="isGenerating"
              @keydown.enter="startGeneration"
              data-cy="instructionsInput"
              autofocus/>
          <div class="flex justify-end">
            <SkillsButton
                :icon="`fa-solid ${isGenerating ? 'fa-stop': 'fa-play'}`"
                :label="isGenerating ? 'Stop' : 'Send'"
                :severity="isGenerating ? 'warn' : 'success'"
                data-cy="sendAndStopBtn"
                @click="onStartStopBtn" />
          </div>
        </div>
      </div>
    </div>
  </SkillsDialog>
</template>

<style scoped>

</style>
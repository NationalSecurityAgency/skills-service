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
import {computed, nextTick, onMounted, ref, useTemplateRef} from 'vue'
import SkillsDialog from "@/components/utils/inputForm/SkillsDialog.vue";
import MarkdownText from "@/common-components/utilities/markdown/MarkdownText.vue";
import AssistantMsg from "@/common-components/utilities/learning-conent-gen/AssistantMsg.vue";
import UserMsg from "@/common-components/utilities/learning-conent-gen/UserMsg.vue";
import {useLog} from "@/components/utils/misc/useLog.js";
import PrefixControls from "@/common-components/utilities/markdown/PrefixControls.vue";
import {useAppConfig} from "@/common-components/stores/UseAppConfig.js";
import {useOpenaiService} from "@/common-components/utilities/learning-conent-gen/UseOpenaiService.js";
import GenStatus from "@/common-components/utilities/learning-conent-gen/GenStatus.vue";
import {useAiModelsState} from "@/common-components/utilities/learning-conent-gen/UseAiModelsState.js";
import AiModelsSelector from "@/common-components/utilities/learning-conent-gen/AiModelsSelector.vue";
import {useElementVisibility, useEventListener} from "@vueuse/core";
import {useInstructionGenerator} from "@/common-components/utilities/learning-conent-gen/UseInstructionGenerator.js";
import AiPromptDialogFooter from "@/common-components/utilities/learning-conent-gen/AiPromptDialogFooter.vue";
import {useDialogUtils} from "@/components/utils/inputForm/UseDialogUtils.js";
import { useSkillsAnnouncer } from '@/common-components/utilities/UseSkillsAnnouncer.js'


const model = defineModel()
const props = defineProps({
  createInstructionsFn: {
    type: Function,
    required: true
  },
  createFollowOnConvoInstructionsFn: {
    type: Function,
    required: false
  },
  chunkHandlerFn: {
    type: Function,
    required: true
  },
  generationCompletedFn: {
    type: Function,
    required: true
  },
  beforeGenerationStartedFn: {
    type: Function,
    required: false
  },
  addPrefixFn: {
    type: Function,
    default: null,
  },
  communityValue: {
    type: String,
    default: null
  },
  useGeneratedLabel: {
    type: String,
    default: 'Use Generated Value'
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
  instructionsPlaceholder: {
    type: String,
    default: 'Type Instructions Here'
  },
  sendButtonLabel: {
    type: String,
    default: 'Send'
  },
  stopButtonLabel: {
    type: String,
    default: 'Stop'
  },
  isValid: {
    type: Boolean,
    default: true
  },
  overallErrMsg: {
    type: String,
    default: null
  },
  allowInitialSubmitWithoutInput: {
    type: Boolean,
    default: false
  },
})

const emit = defineEmits(['use-generated', 'generation-failed'])
const log = useLog()
const appConfig = useAppConfig()
const openaiService = useOpenaiService()
const aiModelsState = useAiModelsState()
const instructionsGenerator = useInstructionGenerator()
const dialogUtils = useDialogUtils()
const announcer = useSkillsAnnouncer()

const addWelcomeMsg = (welcomeMsg) => {
  if (log.isDebugEnabled()) {
    log.debug(`Adding welcome msg: [${welcomeMsg}]`)
  }
  const item = addChatItem(welcomeMsg, ChatRole.ASSISTANT)
  announcer.polite(welcomeMsg.replaceAll('`', ''))
  return item
}
const startGeneration = () => {
  stopScrollingWithIncomingText.value = false
  aiModelsState.afterModelsLoaded().then(() => {
    if (log.isTraceEnabled()) {
      log.trace(`Generating based on instructions: [${instructions.value}]`)
    }
    if (props.beforeGenerationStartedFn) {
      props.beforeGenerationStartedFn()
    }

    const messages = []
    const isNewConvo = chatHistory.value.length === 1
    let userChatInstructions
    if (isNewConvo) {
      userChatInstructions = props.createInstructionsFn(instructions.value)
      messages.push({role: 'User', content: userChatInstructions})
    } else {
      chatHistory.value.slice(1).forEach((item) => {
        const content = item.role === ChatRole.USER ? item.userChatInstructions : item.generatedValue
        messages.push({role: item.role, content})
      })
      if (props.createFollowOnConvoInstructionsFn) {
        userChatInstructions = props.createFollowOnConvoInstructionsFn(instructions.value)
      } else {
        userChatInstructions = instructionsGenerator.followOnConvoInstructions(instructions.value)
      }
      messages.push({role: 'User', content: userChatInstructions})
    }
    addChatItem(instructions.value, ChatRole.USER, false, userChatInstructions)
    instructions.value = ''

    if (log.isDebugEnabled()) {
      log.debug(JSON.stringify(chatHistory.value.map((v) => ({...v}))))
    }

    isGenerating.value = true
    addChatItem(props.generationStartedMsg, ChatRole.ASSISTANT, true)
    announceSlowGenStatusStarted = false
    announcer.polite('AI Generation Started')
    genWithStreaming(messages)
  })
}
defineExpose({
  addWelcomeMsg,
});

const close = () => {
  model.value = false
}

const isAddingPrefix = ref(false)
const instructions = ref('')
const isGenerating = ref(false)
const chatCounter = ref(0)
const chatHistory = ref([])

onMounted(() => {
  aiModelsState.loadModels()
})

const ChatRole = {
  USER: 'User',
  ASSISTANT: 'Assistant',
}
const getLastChatItem = () => {
  return chatHistory.value[chatHistory.value.length - 1]
}
const isLastChatItem = (item) => {
  return item.id === getLastChatItem().id
}
const addChatItem = (origMsg, role = ChatRole.ASSISTANT, isGenerating = false, userChatInstructions=null) => {
  const res = {
    id: `${chatCounter.value++}`,
    role,
    origMessage: origMsg,
    userChatInstructions,
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

  if (failedToGenerate) {
    announcer.assertive(`Error: ${finalMsg}`)
  } else if (cancelled) {
    announcer.polite(`Generation cancelled: ${finalMsg}`)
  }
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
const onInputEnter = () => {
  if (!isGenerating.value && isSendEnabled.value) {
    startGeneration()
  }
}

let announceSlowGenStatusStarted = false
const startAnnounceSlowGenStatus = () => {
  if (!announceSlowGenStatusStarted) {
    announceSlowGenStatusStarted = true
    announceSlowGenStatus()
  }

}
const announceSlowGenStatus = () => {
  setTimeout(() => {
    if (isGenerating.value) {
      announcer.polite(`AI Generation is in progress...`)
      announceSlowGenStatus()
    } else {
      announceSlowGenStatusStarted = false
    }
  }, appConfig.openAiAnnounceGenStatusInterval)
}



const genWithStreaming = (messages) => {
  lastPromptCancelled.value = false
  scrollInstructionsIntoView()
  const promptParams = {
    messages,
    model: aiModelsState.selectedModel.model,
    modelTemperature: aiModelsState.modelTemperature,
  }
  return openaiService.prompt(promptParams,
      (chunk) => {
        startAnnounceSlowGenStatus()
        const lastChatItem = getLastChatItem()
        const chunkRes = props.chunkHandlerFn(chunk, lastChatItem.id)

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
        announcer.polite(`AI Generation Completed`)
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
        emit('generation-failed')
      })
}
const lastPromptCancelled = ref(false)
const cancelCurrentPrompt = () => {
  lastPromptCancelled.value = true
  openaiService.cancelCurrentPrompt()
  announcer.polite('Stopping generation...')
}

const instructionsInputTemplateRef = useTemplateRef('instructionsInputRef')
const isInstructionsInputVisible = useElementVisibility(instructionsInputTemplateRef)
const showScrollToBottomBtn = computed(() => !isInstructionsInputVisible.value)

const stopScrollingWithIncomingText = ref(false)
useEventListener(document, 'wheel', (evt) => {
  if (evt.deltaY < 0) {
    stopScrollingWithIncomingText.value = true
  }
})

const resumeAutoScroll = () => {
  stopScrollingWithIncomingText.value = false
  if (isGenerating.value) {
    scrollInstructionsIntoView()
  } else {
    focusOnInstructionsInput()
  }
}

const scrollInstructionsIntoView = () => {
  if (!stopScrollingWithIncomingText.value) {
    nextTick(() => document.getElementById('instructionsInputId')?.scrollIntoView())
  }
}
const focusOnInstructionsInput = () => {
  if (!stopScrollingWithIncomingText.value) {
    nextTick(() => {
      const input = document.getElementById('instructionsInputId')
      input?.focus()
    })
  }
}

const useGenerated = (historyId) => {
  const historyItem = chatHistory.value.find(item => item.id === historyId)
  emit('use-generated', historyItem)
  announcer.polite('Using generated content. Closing dialog.')
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

const isLoading = computed(() => aiModelsState.loadingModels)
const showChat = computed(() => !isLoading.value && !aiModelsState.failedToLoad)
const finalMsgSeverity = (historyItem) => historyItem.failedToGenerate ? 'error' : historyItem.cancelled ? 'warn' : 'info'

const isSendEnabled = computed(() => ((props.allowInitialSubmitWithoutInput && chatHistory.value?.length === 1) || instructions.value?.trim()?.length > 0) || isGenerating.value)

const hasPoweredByInfo = computed(() => appConfig.openaiFooterPoweredByLink && appConfig.openaiFooterPoweredByLinkText)
const hasFooter = computed(() => appConfig.openaiFooterMsg || hasPoweredByInfo.value)
</script>

<template>
  <SkillsDialog
      :pt="{ header: { class: '!pb-2' }, footer: { class: '!pb-3' }, pcMaximizeButton: dialogUtils.getMaximizeButtonPassThrough() }"
      ref="generateDescriptionDialog"
      :maximizable="true"
      :maximized="true"
      v-model="model"
      header="AI Assistant"
      :loading="isLoading"
      :show-ok-button="false"
      :show-cancel-button="false"
      :enable-return-focus="true">
    <SkillsButton v-if="showScrollToBottomBtn && showChat"
                  icon="fas fa-arrow-down"
                  class="scrollToBottomBtn"
                  @click="resumeAutoScroll"
                  data-cy="scrollToBottomBtn"
                  aria-label="Scroll to Bottom"
                  :outlined="false"/>
    <ai-models-selector />
    <div v-if="showChat" class="py-5 flex flex-col" style="min-height: 70vh">
      <slot name="aboveChatHistory" />
      <div id="chatHistory"
           :class="{ 'flex-1': chatHistory.length > 1 }"
           class="flex flex-col gap-3 mb-2">
        <div v-for="(historyItem) in chatHistory" :key="historyItem.id">
          <div v-if="historyItem.role === ChatRole.USER" class="relative flex justify-end">
            <user-msg :id="historyItem.id">
              <markdown-text :text="historyItem.origMessage || 'Not Provided'" :instanceId="historyItem.id"/>
            </user-msg>
          </div>
          <assistant-msg v-if="historyItem.role === ChatRole.ASSISTANT" class="flex flex-col gap-2" :id="historyItem.id" :is-generating="historyItem.isGenerating">
            <div>
              <gen-status :id="`${historyItem.id}-genStatusId`"
                          :welcome-msg="historyItem.origMessage"
                          :is-generating="historyItem.isGenerating"
                          :is-generate-value-empty="!historyItem.generatedValue || historyItem.generatedValue.trim() === ''"
                          data-cy="origSegment"/>
            </div>
            <div v-if="historyItem.generatedValue" class="px-5 border rounded-lg bg-blue-50 dark:bg-blue-900">
              <slot name="generatedValue" :historyItem="historyItem" data-cy="generatedSegment"/>
              <div v-if="historyItem.generateValueChangedNotes"
                   data-cy="generatedSegmentNotes"
                   class="border-t-2 border-dotted border-blue-200 p-0 mt-5">
               <markdown-text :text="historyItem.generateValueChangedNotes"
                             :instanceId="`${historyItem.id}-generateValueChangedNotes`"/>
              </div>
            </div>
            <div v-if="isLastChatItem(historyItem) && historyItem.finalMsg" data-cy="finalSegment">
              <Message :closable="false" :severity="finalMsgSeverity(historyItem)" aria-live="polite">
                <markdown-text :text="historyItem.finalMsg" :instanceId="`${historyItem.id}-finalMsg`" aria-hidden="true"/>
              </Message>
              <div v-if="!historyItem.failedToGenerate && !historyItem.cancelled"
                   class="flex justify-start items-center gap-3 mt-2">
                <SkillsButton
                    icon="fa-solid fa-check-double"
                    severity="info" :outlined="false"
                    :disabled="!isValid"
                    :label="useGeneratedLabel"
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

      <Message severity="warn" v-if="!isValid && overallErrMsg && !isGenerating" data-cy="overallErrMsg">
        <div v-html="overallErrMsg"></div>
      </Message>
      <div class="flex justify-end mt-6">
        <div class="flex gap-2 w-10/12">
          <InputText
              id="instructionsInputId"
              ref="instructionsInputRef"
              v-model="instructions"
              class="w-full"
              :placeholder="instructionsPlaceholder"
              :disabled="isGenerating"
              @keydown.enter="onInputEnter"
              data-cy="instructionsInput"
              autofocus/>
          <div class="flex justify-end">
            <SkillsButton
                :icon="`fa-solid ${isGenerating ? 'fa-stop': 'fa-play'}`"
                :label="isGenerating ? stopButtonLabel : sendButtonLabel"
                :severity="isGenerating ? 'warn' : 'success'"
                data-cy="sendAndStopBtn"
                :disabled="!isSendEnabled"
                @click="onStartStopBtn" />
          </div>
        </div>
      </div>
    </div>
    <template #footer v-if="hasFooter">
      <ai-prompt-dialog-footer />
    </template>
  </SkillsDialog>
</template>

<style scoped>
.scrollToBottomBtn {
  position: fixed;
  top: 7rem;
  right: 2rem;
  opacity: 0.7;
  z-index: 50;
}

.scrollToBottomBtn:hover {
  opacity: 1;
}
</style>
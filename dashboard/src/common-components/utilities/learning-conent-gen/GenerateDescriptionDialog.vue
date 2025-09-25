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

const model = defineModel()
const emit = defineEmits(['generated-desc'])
const route = useRoute()
const log = useLog()
const imgHandler = useImgHandler()
const chatCounter = ref(0)
const updateDescription = (newDesc) => {
  generatedDescription.value = newDesc
  if (newDesc) {
    chatHistory.value.push({
      id: `${chatCounter.value++}`,
      role: 'assistant',
      origMessage: 'Hope your I am sorry but I took way today is going well! Looks like you already started on a description. Would you like me to proofread or expand on it?'
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
const generatedDescription = ref('')
const isGenerating = ref(false)
const failedGenerating = ref(false)

const chatHistory = ref([])



const instructionsGenerator = useInstructionGenerator()
const reviewDescMsg = 'Review the description and let me know if you need any changes.'
function parseResponse(text) {
  const [newTextPart, comments] = text.split('---COMMENTS---');
  const newText = newTextPart.replace(/---New Text---/i, '').trim();
  return {
    newText,
    comments
  };
}

const shouldStream = ref(true)
const instructionsInput = ref(null)
let latestStreamCleanup = null
const generateDescription = () => {
  failedGenerating.value = false

  if (log.isTraceEnabled()) {
    log.trace(`Generating based on instructions: [${instructions.value}]`)
  }


  chatHistory.value.push({
    id: `${chatCounter.value++}`,
    role: 'user',
    origMessage: instructions.value
  })

  let instructionsToSend = null
  const userInstructions = instructions.value
  const extractedImageState = { hasImages: false, extractedImages: null, }
  if (generatedDescription.value) {
    const extractedImagesRes = imgHandler.extractImages(generatedDescription.value)
    const currentDescription = extractedImagesRes.hasImages ? extractedImagesRes.processedText : generatedDescription.value
    const instructionsToKeepPlaceholders = extractedImagesRes.hasImages? imgHandler.instructionsToKeepPlaceholders() : ''
    instructionsToSend = instructionsGenerator.existingDescriptionInstructions(currentDescription, userInstructions, instructionsToKeepPlaceholders)
    if (extractedImagesRes.hasImages) {
      extractedImageState.hasImages = true
      extractedImageState.extractedImages = extractedImagesRes.extractedImages
    }
  } else {
    instructionsToSend = instructionsGenerator.newDescriptionInstructions(userInstructions)
  }
  instructions.value = ''

  generatedDescription.value = ''
  if (shouldStream.value) {
    chatHistory.value.push({
      id: `${chatCounter.value++}`,
      role: 'assistant',
      origMessage: `I am on it!`,
      generatedValue: ''
    })
    generateDescriptionWithStreaming(instructionsToSend, extractedImageState)
  } else {
    isGenerating.value = true
    generateDescriptionWithoutStreaming(instructionsToSend, extractedImageState)
  }
}

const generateDescriptionWithStreaming = (instructionsToSend, extractedImageState) => {
  return LearningGenService.generateDescriptionStreamWithFetch(route.params.projectId, instructionsToSend,
      (chunk) => {
        generatedDescription.value += chunk
        chatHistory.value[chatHistory.value.length - 1].generatedValue += chunk
      },
      (completeData) => {
        chatHistory.value.push({
          id: `${chatCounter.value++}`,
          role: 'assistant',
          origMessage: `I've prepared a description based on your input. ${reviewDescMsg}`
        })
        isGenerating.value = false
        nextTick(() => {
          document.getElementById('instructionsInput')?.focus()
        })
      },
      (error) => {
        console.error('Error in stream:', error);
      })
}

const generateDescriptionWithoutStreaming = (instructionsToSend, extractedImageState) => {
  return LearningGenService.generateDescription(route.params.projectId, instructionsToSend)
      .then((response) => {
        const {newText, comments} = parseResponse(response.description);
        if (!generatedDescription.value || !comments) {
          chatHistory.value.push({
            id: `${chatCounter.value++}`,
            role: 'assistant',
            origMessage: `I've prepared a description based on your input. ${reviewDescMsg}`
          })
        } else if (comments) {
          chatHistory.value.push({
            id: `${chatCounter.value++}`,
            role: 'assistant',
            origMessage: `Here are my comments: \n${comments}\n\n${reviewDescMsg}`
          })
        }

        let textToInsert = newText
        if (extractedImageState.hasImages) {
          textToInsert = imgHandler.reinsertImages(textToInsert, extractedImageState.extractedImages)
        }
        generatedDescription.value = textToInsert

      }).finally(() => {
    isGenerating.value = false
    nextTick(() => {
      document.getElementById('instructionsInput')?.focus()
    })
  }).catch((err) => {
    failedGenerating.value = true
    isGenerating.value = false
    log.error(err)
    chatHistory.value.push({
      id: `${chatCounter.value++}`,
      role: 'assistant',
      origMessage: 'I apologize, but I was unable to generate a description at this time. Please try again in a few moments.'
    })
  })
}

const useGeneratedDescription = () => {
  emit('generated-desc', generatedDescription.value)
  close()
}

</script>

<template>
  <SkillsDialog
      ref="generateDescriptionDialog"
      :maximizable="true"
      :maximized="true"
      v-model="model"
      header="Description Assistant"
      cancel-button-severity="secondary"
      ok-button-icon="fas fa-check-double"
      ok-button-label="Use"
      :ok-button-disabled="!generatedDescription"
      @on-ok="useGeneratedDescription"
      @on-cancel="close"
      :enable-return-focus="true">

    <div class="flex justify-end gap-2">
      Streaming: <ToggleSwitch v-model="shouldStream" />
    </div>
    <div class="py-5" style="min-height: 70vh">
      <div id="chatHistory" class="flex flex-col gap-3 mb-2">
        <div v-for="(historyItem) in chatHistory" :key="historyItem.id">
          <div v-if="historyItem.role === 'user'" class="relative flex justify-end">
            <user-msg>
              <markdown-text :text="historyItem.origMessage" :instanceId="historyItem.id"/>
            </user-msg>
          </div>
          <assistant-msg v-else>
            <markdown-text :text="historyItem.origMessage" :instanceId="`${historyItem.id}-content`"/>
            <div v-if="historyItem.generatedValue" class="pl-5">
              <markdown-text :text="historyItem.generatedValue" :instanceId="`${historyItem.id}-desc`"/>
            </div>
          </assistant-msg>
        </div>

        <assistant-msg v-if="isGenerating">
          <div class="flex gap-2"><skills-spinner :is-loading="isGenerating" :size-in-rem="0.8"/> Generating...</div>
        </assistant-msg>
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

      <Card class="mt-6 bg-gray-200" v-if="generatedDescription">
        <template #header>
          <div class="px-3 pt-4 text-xl font-bold flex gap-2 text-green-700">
            <i class="fa-solid fa-file-circle-check" aria-hidden="true"></i> Working Copy
          </div>
        </template>
        <template #content>
          <markdown-text v-if="generatedDescription" :text="generatedDescription" instanceId="workingCopy"/>
        </template>
      </Card>
    </div>
  </SkillsDialog>
</template>

<style scoped>

</style>
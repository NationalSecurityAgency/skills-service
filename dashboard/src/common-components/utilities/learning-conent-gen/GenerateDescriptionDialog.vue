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

const model = defineModel()
const emit = defineEmits(['generated-desc'])
const route = useRoute()
const log = useLog()
const chatCounter = ref(0)
const updateDescription = (newDesc) => {
  generatedDescription.value = newDesc
  if (newDesc) {
    chatHistory.value.push({
      id: `${chatCounter.value++}`,
      role: 'assistant',
      content: 'Hope your I am sorry but I took way today is going well! Looks like you already started on a description. Would you like me to proofread or expand on it?'
    })
  } else {
    chatHistory.value.push({
      id: `${chatCounter.value++}`,
      role: 'assistant',
      content: 'Hi! I am here to help. Describe the skill, and I\'ll help generate a description'
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



const coreInstructions = 'Generate a detailed description for a skill that\'s will be part of a larger training. Do no provide introduction. Use Markdown. User the word "skill" and not training. Do not put word skill in titles. How are the instructions of how to achieved this skill:'
const reviewDescMsg = 'Review the description and let me know if you need any changes.'
function parseResponse(text) {
  const [newTextPart, comments] = text.split('---COMMENTS---');
  const newText = newTextPart.replace('---New Text---', '').trim();
  return {
    newText,
    comments
  };
}

const instructionsInput = ref(null)
const generateDescription = () => {
  failedGenerating.value = false

  if (log.isTraceEnabled()) {
    log.trace(`Generating based on instructions: [${instructions.value}]`)
  }
  isGenerating.value = true

  chatHistory.value.push({
    id: `${chatCounter.value++}`,
    role: 'user',
    content: instructions.value
  })

  let instructionsToSend = null
  if (generatedDescription.value) {
    instructionsToSend = `Here is the current description:\n ${generatedDescription.value}\n\nPlease modify it based on the following instructions:\n ${instructions.value}\n\n Provide the new text after ---New Text--- in its raw form first without any comments or fields (such as "corrected text"), then use "---COMMENTS---" as a separator, and finally list any comments or suggestions.`
  } else {
    instructionsToSend = `${coreInstructions}\n\n${instructions.value}`
  }
  instructions.value = ''

  LearningGenService.generateDescription(route.params.projectId, instructionsToSend)
      .then((response) => {
        const {newText, comments} = parseResponse(response.description);
        if (log.isDebugEnabled()) {
          log.debug(`Sent instructions: [${instructionsToSend}]\nResponse: [${response.description}]\nNew Text: [${newText}]\nComments: [${comments}]`)
        }
        if (!generatedDescription.value || !comments) {
          chatHistory.value.push({
            id: `${chatCounter.value++}`,
            role: 'assistant',
            content: `I've prepared a description based on your input. ${reviewDescMsg}`
          })
        } else if (comments) {
          chatHistory.value.push({
            id: `${chatCounter.value++}`,
            role: 'assistant',
            content: `Here are my comments: \n${comments}\n\n${reviewDescMsg}`
          })
        }
        generatedDescription.value = newText

      }).finally(() => {
        isGenerating.value = false
        nextTick(() => {
          document.getElementById('instructionsInput')?.focus()
        })
      }).catch((err) => {
        failedGenerating.value = true
        isGenerating.value = false
        log.error(JSON.stringify(err))
        console.log('Failed to generate description')
        chatHistory.value.push({
          id: `${chatCounter.value++}`,
          role: 'assistant',
          content: 'I apologize, but I was unable to generate a description at this time. Please try again in a few moments.'
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

    <div class="py-5" style="min-height: 70vh">
      <div id="chatHistory" class="flex flex-col gap-3 mb-2">
        <div v-for="(historyItem) in chatHistory" :key="historyItem.id">
          <div v-if="historyItem.role === 'user'" class="relative flex justify-end">
            <user-msg>
              <markdown-text :text="historyItem.content" :instanceId="historyItem.id"/>
            </user-msg>
          </div>
          <assistant-msg v-else>
            <markdown-text :text="historyItem.content" :instanceId="historyItem.id"/>
          </assistant-msg>
        </div>

        <assistant-msg v-if="isGenerating">
          <div class="flex gap-2"><skills-spinner :is-loading="isGenerating" :size-in-rem="0.8"/> Thinking...</div>
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
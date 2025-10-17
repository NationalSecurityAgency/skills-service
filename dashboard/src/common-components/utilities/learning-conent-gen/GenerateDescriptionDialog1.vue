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
import {ref} from 'vue'
import {useRoute} from "vue-router";
import MarkdownText from "@/common-components/utilities/markdown/MarkdownText.vue";
import {useLog} from "@/components/utils/misc/useLog.js";
import {useImgHandler} from "@/common-components/utilities/learning-conent-gen/UseImgHandler.js";
import {useInstructionGenerator} from "@/common-components/utilities/learning-conent-gen/UseInstructionGenerator.js";
import {useDescriptionValidatorService} from "@/common-components/validators/UseDescriptionValidatorService.js";
import {useAppConfig} from "@/common-components/stores/UseAppConfig.js";
import AiPromptDialog from "@/common-components/utilities/learning-conent-gen/AiPromptDialog.vue";

const model = defineModel()
const props = defineProps({
  communityValue: {
    type: String,
    default: null
  },
})
const emit = defineEmits(['generated-desc'])
const route = useRoute()
const log = useLog()
const imgHandler = useImgHandler()
const appConfig = useAppConfig()
const instructionsGenerator = useInstructionGenerator()

const currentDescription = ref('')
const extractedImageState = { hasImages: false, extractedImages: null, }

const aiPromptDialogRef = ref(null)
const updateDescription = (newDesc) => {
  currentDescription.value = newDesc

  const welcomeMsg = newDesc ?
      'I noticed you\'ve already started and can help you refine and enhance! \n\nFor example, you could type `proofread` or `rewrite with more detail`. The more **specific** you are, the better I can assist you!'
      : 'Hi there! I\'m excited to help you craft something **amazing**. Please share some `details` about what you have in mind.'

  aiPromptDialogRef.value.addWelcomeMsg(welcomeMsg)
}
defineExpose({
  updateDescription
});

const createPromptInstructions = (userEnterInstructions) => {
  let instructionsToSend = ''
  if (currentDescription.value) {
    const extractedImagesRes = imgHandler.extractImages(currentDescription.value)
    const descriptionText = extractedImagesRes.hasImages ? extractedImagesRes.processedText : currentDescription.value
    const instructionsToKeepPlaceholders = extractedImagesRes.hasImages? imgHandler.instructionsToKeepPlaceholders() : ''
    instructionsToSend = instructionsGenerator.existingDescriptionInstructions(descriptionText, userEnterInstructions, instructionsToKeepPlaceholders)
    if (extractedImagesRes.hasImages) {
      extractedImageState.extractedImages = extractedImagesRes.extractedImages
    }
  } else {
    instructionsToSend = instructionsGenerator.newDescriptionInstructions(userEnterInstructions)
  }

  return instructionsToSend
}

const handleGeneratedChunk = (chunk) => {
  return {
    append: true,
    chunk: chunk
  }
}

const handleGenerationCompleted = (generated) => {
  let generatedValue = generated.generatedValue
  let generateValueChangedNotes = null

  const [newText, comments] = generatedValue.split('Here is what was changed');
  if (newText && comments) {
    const cleanedComments = comments.replace(/^[\s:]+/, '').trim()
    generatedValue = newText.replace(/\s*#+\s*$/gm, '') // Remove ### at end of lines.trim()
    generateValueChangedNotes = `### Here is what was changed\n\n${cleanedComments}`
  }

  if (extractedImageState.extractedImages) {
    const {
      text,
      unusedImages
    } = imgHandler.reinsertImages(generatedValue, extractedImageState.extractedImages)
    generatedValue = text
    extractedImageState.extractedImages = unusedImages
  }

  return {
    generatedValue,
    generateValueChangedNotes
  }
}

const useGenerated = (historyItem) => {
  emit('generated-desc', historyItem.generatedValue)
}

const validationService = useDescriptionValidatorService()
const handleAddPrefix = (historyItem, missingPrefix) => {
  if (historyItem?.generatedValue) {
    return validationService.addPrefixToInvalidParagraphs(historyItem?.generatedValue, missingPrefix).then((result) => {
      historyItem.generatedValue = result.newDescription
      return historyItem
    })
  }

  return historyItem
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
      :community-value="communityValue"
      @use-generated="useGenerated"
  >
    <template #generatedValue="{ historyItem }">
      <markdown-text :text="historyItem.generatedValue"
                     data-cy="generatedSegment"
                     :instanceId="`${historyItem.id}-desc`"/>
    </template>
  </ai-prompt-dialog>
</template>

<style scoped>

</style>
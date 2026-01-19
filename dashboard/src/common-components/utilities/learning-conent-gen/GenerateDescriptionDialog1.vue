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
import MarkdownText from "@/common-components/utilities/markdown/MarkdownText.vue";
import {useImgHandler} from "@/common-components/utilities/learning-conent-gen/UseImgHandler.js";
import {useInstructionGenerator} from "@/common-components/utilities/learning-conent-gen/UseInstructionGenerator.js";
import {useDescriptionValidatorService} from "@/common-components/validators/UseDescriptionValidatorService.js";
import AiPromptDialog from "@/common-components/utilities/learning-conent-gen/AiPromptDialog.vue";
import GenerateDescriptionType from "@/common-components/utilities/learning-conent-gen/GenerateDescriptionType.js";

const model = defineModel()
const props = defineProps({
  communityValue: {
    type: String,
    default: null
  },
  type: {
    type: String,
    default: GenerateDescriptionType.Skill,
    validator: (value) => {
      return GenerateDescriptionType.AllTypes.includes(value)
    }
  }
})
const emit = defineEmits(['generated-desc'])
const imgHandler = useImgHandler()
const instructionsGenerator = useInstructionGenerator()

const currentDescription = ref('')
const extractedImageState = { hasImages: false, extractedImages: null, }

const aiPromptDialogRef = ref(null)
const updateDescription = (newDesc) => {
  currentDescription.value = newDesc

  const welcomeMsg = newDesc ?
      `I can help improve your existing \`${props.type}\` description. Try these requests:
- "Make it more engaging"
- "Add more technical details"
- "Shorten while keeping key points"
- "Improve the flow"`
      : `Let's create an engaging \`${props.type}\` description together! What's the main idea you want to convey?`

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
    switch (props.type) {
      case GenerateDescriptionType.Badge:
        instructionsToSend = instructionsGenerator.newBadgeDescriptionInstructions(userEnterInstructions)
        break
      case GenerateDescriptionType.Subject:
        instructionsToSend = instructionsGenerator.newSubjectDescriptionInstructions(userEnterInstructions)
        break
      case GenerateDescriptionType.Project:
        instructionsToSend = instructionsGenerator.newProjectDescriptionInstructions(userEnterInstructions)
        break
      case GenerateDescriptionType.Quiz:
        instructionsToSend = instructionsGenerator.newQuizDescriptionInstructions(userEnterInstructions)
        break
      case GenerateDescriptionType.Survey:
        instructionsToSend = instructionsGenerator.newSurveyDescriptionInstructions(userEnterInstructions)
        break
      case GenerateDescriptionType.SkillGroup:
        instructionsToSend = instructionsGenerator.newSkillGroupDescriptionInstructions(userEnterInstructions)
        break
      default:
        instructionsToSend = instructionsGenerator.newSkillDescriptionInstructions(userEnterInstructions)
    }
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

  const [newText, comments] = generatedValue.split(/Here is what was changed/i);
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
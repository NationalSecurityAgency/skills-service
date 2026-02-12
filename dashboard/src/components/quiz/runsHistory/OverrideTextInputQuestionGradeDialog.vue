/*
Copyright 2026 SkillTree

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

import SkillsInputFormDialog from "@/components/utils/inputForm/SkillsInputFormDialog.vue";
import {object, string} from "yup";
import MarkdownEditor from "@/common-components/utilities/markdown/MarkdownEditor.vue";

const model = defineModel()
const props = defineProps({
  question: Object,
})

const schema = object({

})

const overrideGrade = () => {

}

const initialData = {}

const afterSave = () => {
  model.value = false
}
</script>

<template>
  <SkillsInputFormDialog
      id="correctTextInputGrade"
      header="Override Grade"
      v-model="model"
      :save-data-function="overrideGrade"
      @saved="afterSave"
      :validation-schema="schema"
      :initial-values="initialData"
      :enable-return-focus="true"
      data-cy="addSkillTagDialog"
      save-button-label="Override Grade"
      save-button-icon="fa-solid fa-hammer">
    <Message  severity="warn" :closable="false">
      <div v-if="question.isCorrect">This operation will change the answer to <Tag severity="danger">WRONG</tag> which is currently graded as <Tag>CORRECT</Tag>. Please proceed with caution.</div>
      <div v-else>This operation will change the answer to <Tag>CORRECT</tag> which is currently graded as <Tag severity="danger">WRONG</Tag>. Please proceed with caution.</div>
    </Message>
    <MarkdownEditor data-cy="feedback"
                    :id="`qFeedback-${question.id}`"
                    :allow-community-elevation="true"
                    markdownHeight="150px"
                    label="Your Feedback (optional)"
                    :disable-ai-prompt="true"
                    :allow-attachments="false"
                    :allow-insert-images="false"
                    :aria-label="`optionally provide feedback for answer of question # ${question.questionNumber}`"
                    name="feedbackTxt"/>
  </SkillsInputFormDialog>
</template>

<style scoped>

</style>
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

import SkillsInputFormDialog from "@/components/utils/inputForm/SkillsInputFormDialog.vue";
import {object, string} from "yup";
import {useAppConfig} from "@/common-components/stores/UseAppConfig.js";
import ExistingUserInput from "@/components/utils/ExistingUserInput.vue";
import LazyProjectSkillsSelector from "@/components/skills/LazyProjectSkillsSelector.vue";
import {useRoute} from "vue-router";

const model = defineModel()
const appConfig = useAppConfig
const route = useRoute()

const saveComment = () => {
}
const onCommentSaved = () => {
}
const close = () => {

}

const schema = object({
  'comment': string()
      .max(appConfig.descriptionMaxLength)
      .customDescriptionValidator('Subject Description')
      .label('Subject Description'),
})

const initialData = {}

</script>

<template>
  <SkillsInputFormDialog
      id="newCommentDialog"
      v-model="model"
      header="New User Comment"
      saveButtonLabel="Submit"
      save-button-icon="fas fa-paper-plane"
      :validation-schema="schema"
      :initial-values="initialData"
      :save-data-function="saveComment"
      :enable-return-focus="true"
      @saved="onCommentSaved"
      @cancelled="close"
      @close="close">
    <div class="flex flex-col gap-4">
      <lazy-project-skills-selector
          name="selectedSkill"
          label="Skill"/>
      <existing-user-input
          :project-id="route.params.projectId"
          label="User"
          name="selectedUser"
          data-cy="userIdInput"/>
      <SkillsTextarea
          label="User Comment"
          rows="3"
          max-rows="6"
          name="userCommentInput"
          data-cy="userCommentInput"
      />
    </div>
  </SkillsInputFormDialog>
</template>

<style scoped>

</style>
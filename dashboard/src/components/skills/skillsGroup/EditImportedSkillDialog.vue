/*
Copyright 2024 SkillTree

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
import { useRoute } from 'vue-router'
import SkillsInputFormDialog from '@/components/utils/inputForm/SkillsInputFormDialog.vue'
import SkillsService from '@/components/skills/SkillsService.js'
import InputSanitizer from '@/components/utils/InputSanitizer.js'
import { number, object } from 'yup'
import { useAppConfig } from '@/common-components/stores/UseAppConfig.js'

const show = defineModel()
const route = useRoute()
const props = defineProps({
  skill: Object,
})
const emit = defineEmits(['skill-updated'])
const appConfig = useAppConfig()

const formId = 'editImportedSkillDialog'

const schema = object({
  'pointIncrement': number()
    .required()
    .min(1)
    .max(appConfig.maxPointIncrement)
    .label('Point Increment'),
})

const initialSkillData = {
  pointIncrement: props.skill.pointIncrement,
}

const saveSkill = (values) => {
  const skilltoSave = {
    projectId: route.params.projectId,
    skillId: InputSanitizer.sanitize(props.skill.skillId),
    pointIncrement: values.pointIncrement,
  }

  return SkillsService.updateImportedSkill(skilltoSave)
    .then(() => {
      return {
        ...props.skill,
        pointIncrement: values.pointIncrement,
        originalSkillId: props.skill.skillId,
      }
    })
}

const onSkillSaved = (skill) => {
  emit('skill-updated', skill)
}
</script>

<template>
  <SkillsInputFormDialog
    :id="formId"
    v-model="show"
    :save-data-function="saveSkill"
    header="Edit Catalog Imported Skill"
    saveButtonLabel="Save"
    :validation-schema="schema"
    :initial-values="initialSkillData"
    :enable-return-focus="true"
    data-cy="EditImportedSkillModal"
    @saved="onSkillSaved">

    <Message :closable="false" icon="fas fa-book" data-cy="importedSkillMessage">
      This skill was imported from <span class="italic">{{ skill.copiedFromProjectName }}</span> and can only be modified in that project.
      You can change the <b>Point Increment</b> in order to scale the total points to your project's point layout.
    </Message>

    <SkillsNumberInput
      class="flex-1"
      style="min-width: 14rem;"
      :min="1"
      :is-required="true"
      label="Point Increment"
      name="pointIncrement" />

  </SkillsInputFormDialog>
</template>

<style scoped>

</style>
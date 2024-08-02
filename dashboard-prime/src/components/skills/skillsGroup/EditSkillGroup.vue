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
import { ref } from 'vue';
import { useRoute } from 'vue-router'
import SkillsInputFormDialog from '@/components/utils/inputForm/SkillsInputFormDialog.vue'
import SkillsNameAndIdInput from '@/components/utils/inputForm/SkillsNameAndIdInput.vue'
import SkillsService from '@/components/skills/SkillsService.js'
import InputSanitizer from '@/components/utils/InputSanitizer.js'
import { object, string } from 'yup'
import { useAppConfig } from '@/common-components/stores/UseAppConfig.js'
import { useSkillYupValidators } from '@/components/skills/UseSkillYupValidators.js'
import MarkdownEditor from '@/common-components/utilities/markdown/MarkdownEditor.vue'

const show = defineModel()
const route = useRoute()
const props = defineProps({
  skill: Object,
  isEdit: Boolean,
})
const emit = defineEmits(['skill-saved'])
const appConfig = useAppConfig()
const skillYupValidators = useSkillYupValidators()

const formId = props.isEdit ? `editGroupDialog-${props.skill.projectId}-${props.skill.skillId}` : 'newGroupDialog'
let modalTitle = props.isEdit ? 'Edit Skills Group' : 'New Skills Group'

const asyncLoadData = () => {
  if (props.isEdit) {
    return SkillsService.getSkillDetails(route.params.projectId, route.params.subjectId, props.skill.skillId)
      .then((resSkill) => {
        const loadedSkill = { ...resSkill, 'description': resSkill.description || ''};
        initialSkillData.value = { ...loadedSkill }
        return loadedSkill
      })
  }
  return Promise.resolve({})
}

const schema = object({
  'name': string()
    .trim()
    .required()
    .min(appConfig.minNameLength)
    .max(appConfig.maxSkillNameLength)
    .nullValueNotAllowed()
    .customNameValidator('Group Name')
    .test('uniqueName', 'The value for the Group Name is already taken', (value) => skillYupValidators.checkSkillNameUnique(value, props.skill.name, props.isEdit))
    .label('Group Name'),
  'skillId': string()
    .required()
    .min(appConfig.minIdLength)
    .max(appConfig.maxIdLength)
    .nullValueNotAllowed()
    .idValidator()
    .test('uniqueId', 'The value for the Group ID is already taken', (value) => skillYupValidators.checkSkillIdUnique(value, props.skill.skillId, props.isEdit))
    .label('Group ID'),
  'description': string()
    .max(appConfig.descriptionMaxLength)
    .customDescriptionValidator('Group Description')
    .label('Skill Description'),
})

const initialSkillData = ref({
  skillId: props.skill.skillId || '',
  name: props.skill.name || '',
  originalSkillId: props.skill.skillId || '',
  description: props.skill.description || ''
})

const saveSkill = (values) => {
  const skilltoSave = {
    ...values,
    type: 'SkillsGroup',
    subjectId: route.params.subjectId,
    projectId: route.params.projectId,
    isEdit: props.isEdit,
    name: InputSanitizer.sanitize(values.name),
    skillId: InputSanitizer.sanitize(values.skillId),
  }
  return SkillsService.saveSkill(skilltoSave)
    .then((skillRes) => {
      return {
        ...skillRes,
        originalSkillId: props.skill.skillId,
      }
    })
}

const onSkillSaved = (skill) => {
  emit('skill-saved', skill)
}
</script>

<template>
  <SkillsInputFormDialog
    :id="formId"
    v-model="show"
    :is-edit="isEdit"
    :async-load-data-function="asyncLoadData"
    :save-data-function="saveSkill"
    :header="modalTitle"
    saveButtonLabel="Save"
    :validation-schema="schema"
    :initial-values="initialSkillData"
    :enable-return-focus="true"
    :should-confirm-cancel="true"
    data-cy="EditSkillGroupModal"
    @saved="onSkillSaved">

    <SkillsNameAndIdInput
      name-label="Group Name"
      name-field-name="name"
      id-label="Group ID"
      id-field-name="skillId"
      id-suffix="Group"
      :name-to-id-sync-enabled="!props.isEdit" />

    <markdown-editor
      class="mt-5"
      name="description" />

  </SkillsInputFormDialog>
</template>

<style scoped>

</style>
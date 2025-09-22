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
import { computed, ref } from 'vue'
import { boolean, object, string, ValidationError } from 'yup'
import { useDebounceFn } from '@vueuse/core'
import InputSanitizer from '@/components/utils/InputSanitizer.js'
import ProjectService from '@/components/projects/ProjectService.js'
import { useAppConfig } from '@/common-components/stores/UseAppConfig.js'
import { useCommunityLabels } from '@/components/utils/UseCommunityLabels.js'
import MarkdownEditor from '@/common-components/utilities/markdown/MarkdownEditor.vue'
import SkillsNameAndIdInput from '@/components/utils/inputForm/SkillsNameAndIdInput.vue'
import SkillsInputFormDialog from '@/components/utils/inputForm/SkillsInputFormDialog.vue'
import { useAccessState } from '@/stores/UseAccessState.js'
import CommunityProtectionControls from '@/components/projects/CommunityProtectionControls.vue'
import { useDescriptionValidatorService } from '@/common-components/validators/UseDescriptionValidatorService.js'

const model = defineModel()
const props = defineProps(['project', 'isEdit', 'isCopy'])
const emit = defineEmits(['project-saved'])
const accessState = useAccessState()

let formId = 'newProjectDialog'
let modalTitle = 'New Project'
if (props.isEdit) {
  formId = `editProjectDialog-${props.project.projectId}`
  modalTitle = 'Editing Existing Project'
} else if (props.isCopy) {
  formId = `copyProjectDialog-${props.project.projectId}`
  modalTitle ='Copy Project'
}
const appConfig = useAppConfig()

const communityLabels = useCommunityLabels()
const initialValueForEnableProtectedUserCommunity = communityLabels.isRestrictedUserCommunity(props.project.userCommunity)
const enableProtectedUserCommunity = ref(initialValueForEnableProtectedUserCommunity)
// if (props.isCopy && initialValueForEnableProtectedUserCommunity) {
//   this.originalProject.enableProtectedUserCommunity = this.initialValueForEnableProtectedUserCommunity;
// }

const checkProjNameUnique = useDebounceFn((value) => {
  if (!value || value.length === 0) {
    return true
  }
  const origName = props.project.name
  if (props.isEdit && (origName === value || origName.localeCompare(value, 'en', { sensitivity: 'base' }) === 0)) {
    return true
  }
  return ProjectService.checkIfProjectNameExist(value).then((remoteRes) => !remoteRes)
}, appConfig.formFieldDebounceInMs)
const checkProjIdUnique = useDebounceFn((value) => {
  if (!value || value.length === 0 || (props.isEdit && props.project.projectId === value)) {
    return true
  }
  return ProjectService.checkIfProjectIdExist(value)
    .then((remoteRes) => !remoteRes)

}, appConfig.formFieldDebounceInMs)

const descriptionValidatorService = useDescriptionValidatorService()
const checkDescription = useDebounceFn((value, testContext) => {
  if (!value || value.trim().length === 0 || !appConfig.paragraphValidationRegex) {
    return true
  }
  return descriptionValidatorService.validateDescription(value, false, enableProtectedUserCommunity.value, false).then((result) => {
    if (result.valid) {
      return true
    }
    let fieldNameToUse = 'Project Description'
    if (result.msg) {
      return testContext.createError({ message: `${fieldNameToUse ? `${fieldNameToUse} - ` : ''}${result.msg}` })
    }
    return testContext.createError({ message: `${fieldNameToUse || 'Field'} is invalid` })
  })

}, appConfig.formFieldDebounceInMs)


const checkProjectCommunityRequirements =(value, testContext) => {
  if (!value || !props.isEdit) {
    return true;
  }
  return ProjectService.validateProjectForEnablingCommunity(props.project.projectId).then((result) => {
    if (result.isAllowed) {
      return true;
    }
    if (result.unmetRequirements) {
      // return `<ul><li>${result.unmetRequirements.join('</li><li>')}</li></ul>`;
      const errors = result.unmetRequirements.map((req) => {
        return testContext.createError({ message: `${req}` })
      })
      return new ValidationError(errors)
    }
    // return testContext.createError({ message: `${fieldNameToUse ? `${fieldNameToUse} - ` : ''}${result.msg}` })
    // return '{_field_} is invalid.';
    return true
  });
}

const schema = object({
  'projectName': string()
    .trim()
    .required()
    .min(appConfig.minNameLength)
    .max(appConfig.maxProjectNameLength)
    .nullValueNotAllowed()
    .test('uniqueName', 'Project Name already exists', (value) => checkProjNameUnique(value))
    .customNameValidator('Project Name')
    .label('Project Name'),
  'projectId': string()
    .required()
    .min(appConfig.minIdLength)
    .max(appConfig.maxIdLength)
    .idValidator()
    .nullValueNotAllowed()
    .test('uniqueId', 'Project ID already exists', (value) => checkProjIdUnique(value))
    .label('Project ID'),
  'enableProtectedUserCommunity': boolean()
    .test('communityReqValidation', 'Unmet community requirements', (value, testContext) => checkProjectCommunityRequirements(value, testContext))
    .label('Enable Protected User Community'),
  'description': string()
    .max(appConfig.descriptionMaxLength)
    .test('descriptionValidation', 'Description is invalid', (value, testContext) => checkDescription(value, testContext))
    .label('Project Description')
})


const initialProjData = ref({
  projectId: props.project.projectId || '',
  projectName: props.project.name || '',
  description: props.project.description || '',
  enableProtectedUserCommunity: false,
})

const asyncLoadData = () => {
  const loadDescription = () => {
    if(props.isEdit) {
      return ProjectService.loadDescription(props.project.projectId).then((data) => {
        initialProjData.value.description = data.description ? data.description : ''
        initialProjData.value = { ...initialProjData.value }
        return {'description': data.description || ''}
      })
    }
    return Promise.resolve({})
  }

  return loadDescription()
}

const close = () => { model.value = false }

const isRootUser = computed(() => accessState.isRoot)
const saveProject = (values) => {
  const projToSave = {
    ...values,
    originalProjectId: props.project.projectId,
    isEdit: props.isEdit,
    name: InputSanitizer.sanitize(values.projectName),
    projectId: InputSanitizer.sanitize(values.projectId)
  };

  if (initialValueForEnableProtectedUserCommunity) {
    projToSave.enableProtectedUserCommunity = initialValueForEnableProtectedUserCommunity
  }

  emit('project-saved', projToSave, props.isEdit, props.project.projectId)
  return Promise.resolve();
}

const onSavedProject = () => {
  close()
}

</script>

<template>
  <SkillsInputFormDialog
    :id="formId"
    v-model="model"
    :should-confirm-cancel="true"
    :is-edit="isEdit"
    :header="modalTitle"
    :saveButtonLabel="`${isCopy ? 'Copy Project' : 'Save'}`"
    :validation-schema="schema"
    :initial-values="initialProjData"
    @saved="onSavedProject"
    @close="close"
    :async-load-data-function="asyncLoadData"
    :save-data-function="saveProject"
  >
    <template #default>
      <SkillsNameAndIdInput
        :name-label="`${isCopy ? 'New Project Name' : 'Project Name'}`"
        name-field-name="projectName"
        :id-label="`${props.isCopy ? 'New Project ID' : 'Project ID'}`"
        id-field-name="projectId"
        :name-to-id-sync-enabled="!props.isEdit" />

      <community-protection-controls
        v-model:enable-protected-user-community="enableProtectedUserCommunity"
        :project="project"
        :is-edit="isEdit"
        :is-copy="isCopy" />
      <markdown-editor
        class="mt-8"
        :upload-url="isEdit   ? `/admin/projects/${props.project.projectId}/upload` : null"
        :allow-attachments="isEdit"
        :user-community="isEdit ? props.project.userCommunity : null"
        name="description" />

    </template>
  </SkillsInputFormDialog>
</template>

<style scoped>

</style>
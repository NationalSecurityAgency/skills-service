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
import { computed, onMounted, ref } from 'vue'
import SkillsInputFormDialog from '@/components/utils/inputForm/SkillsInputFormDialog.vue'
import { object, string } from 'yup'
import { useRoute } from 'vue-router'
import { useAppConfig } from '@/common-components/stores/UseAppConfig.js'
import SubjectsService from '@/components/subjects/SubjectsService'
import InputSanitizer from '@/components/utils/InputSanitizer'
import MarkdownEditor from '@/common-components/utilities/markdown/MarkdownEditor.vue'
import HelpUrlInput from '@/components/utils/HelpUrlInput.vue'
import SkillsNameAndIdInput from '@/components/utils/inputForm/SkillsNameAndIdInput.vue'
import IconPicker from '@/components/utils/iconPicker/IconPicker.vue'
import { useFocusState } from '@/stores/UseFocusState.js'

const focusState = useFocusState()
const model = defineModel()
const props = defineProps({
  subject: Object,
  isEdit: Boolean,
  value: Boolean
})
const appConfig = useAppConfig()
const emit = defineEmits(['hidden', 'subject-saved'])
const route = useRoute()

let formId = 'newSubjectDialog'
let modalTitle = 'New Subject'
if (props.isEdit) {
  formId = `editSubjectDialog-${route.params.projectId}-${props.subject.subjectId}`
  modalTitle = 'Editing Existing Subject'
}

let canAutoGenerateId = ref(true)
let restoredFromStorage = ref(false)
let currentFocus = ref(null)
let previousFocus = ref(null)
let originalFocusedElement = null;

onMounted(() => {
  originalFocusedElement = focusState.elementId;
  document.addEventListener('focusin', trackFocus)
})

const trackFocus = () => {
  previousFocus.value = currentFocus.value
  currentFocus.value = document.activeElement
}

const close = () => {
  model.value = false
  focusState.setElementId(originalFocusedElement);
}

const onSelectedIcon = (selectedIcon) => {
  currentIcon.value = selectedIcon.css
}


const checkSubjectNameUnique = (value) => {
  if (!value || value === props.subject.name) {
    return true
  }
  return SubjectsService.subjectWithNameExists(route.params.projectId, value)
}

const checkSubjectIdUnique = (value) => {
  if (!value || value === props.subject.subjectId) {
    return true
  }
  return SubjectsService.subjectWithIdExists(route.params.projectId, value)
}

const schema = object({
  'subjectName': string()
    .trim()
    .required()
    .min(appConfig.minNameLength)
    .max(appConfig.maxSubjectNameLength)
    .nullValueNotAllowed()
    .customNameValidator('Subject Name')
    .test('uniqueName', 'Subject Name is already taken', (value) => checkSubjectNameUnique(value))
    .label('Subject Name'),
  'subjectId': string()
    .trim()
    .required()
    .min(appConfig.minIdLength)
    .max(appConfig.maxIdLength)
    .nullValueNotAllowed()
    .idValidator()
    .test('uniqueName', 'Subject ID is already taken', (value) => checkSubjectIdUnique(value))
    .label('Subject ID'),
  'description': string()
    .max(appConfig.descriptionMaxLength)
    .customDescriptionValidator('Subject Description')
    .label('Subject Description'),
  'helpUrl': string()
    .urlValidator()
    .label('Help URL')
})

const initialSubjData = {
  projectId: route.params.projectId,
  subjectId: props.subject.subjectId || '',
  subjectName: props.subject.name || '',
  helpUrl: props.subject.helpUrl || '',
  description: props.subject.description || '',
  iconClass: props.subject.iconClass || 'fas fa-book'
}

const currentIcon = ref((props.subject.iconClass || 'fas fa-book'))

const updateSubject = (values) => {
  const subjToSave = {
    ...values,
    iconClass: currentIcon.value,
    originalSubjectId: props.subject.subjectId,
    projectId: route.params.projectId,
    isEdit: props.isEdit,
    name: InputSanitizer.sanitize(values.subjectName),
    subjectId: InputSanitizer.sanitize(values.subjectId)
  }
  return SubjectsService.saveSubject(subjToSave).then((subjRes) => {
    return {
      ...subjRes,
      originalSubjectId: props.subject.subjectId
    }
  })
}
const onSubjectSaved = (subject) => {
  emit('subject-saved', subject)
  close()
}

</script>

<template>
  <SkillsInputFormDialog
    :id="formId"
    v-model="model"
    :is-edit="isEdit"
    :header="modalTitle"
    :should-confirm-cancel="true"
    saveButtonLabel="Save"
    :validation-schema="schema"
    :initial-values="initialSubjData"
    :save-data-function="updateSubject"
    :enable-return-focus="true"
    :isEdit="isEdit"
    @saved="onSubjectSaved"
    @cancelled="close"
    @close="close">
    <template #default>
      <SkillsNameAndIdInput
        name-label="Subject Name"
        name-field-name="subjectName"
        id-label="Subject ID"
        id-field-name="subjectId"
        id-suffix="Subject"
        :name-to-id-sync-enabled="!props.isEdit">
        <template #beforeName>
          <div class="flex justify-content-center">
            <icon-picker
              class="mb-3"
              :startIcon="currentIcon"
              @selected-icon="onSelectedIcon"
            />
          </div>
        </template>
      </SkillsNameAndIdInput>

      <markdown-editor class="" name="description" />
      <help-url-input class="mt-3"
                      :next-focus-el="previousFocus"
                      name="helpUrl"
                      @keydown-enter="emit('keydown-enter')" />
    </template>
  </SkillsInputFormDialog>
</template>

<style scoped>

.icon-button {
  cursor: pointer;
  height: 100px;
  width: 100px;
  background-color: transparent;
}

.icon-button:disabled {
  background-color: lightgrey;
  cursor: none;
}
</style>

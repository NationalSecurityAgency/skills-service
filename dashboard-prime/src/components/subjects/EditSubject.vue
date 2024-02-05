<script setup>
import { ref, computed, onMounted, nextTick } from 'vue';
import SkillsInputFormDialog from "@/components/utils/inputForm/SkillsInputFormDialog.vue";
import {string} from "yup";
import { useAppConfig } from '@/components/utils/UseAppConfig.js'
import SubjectsService from '@/components/subjects/SubjectsService';
import InputSanitizer from '@/components/utils/InputSanitizer';
import MarkdownEditor from '@/common-components/utilities/markdown/MarkdownEditor.vue'
import HelpUrlInput from '@/components/utils/HelpUrlInput.vue';

const model = defineModel()
const props = defineProps({
  subject: Object,
  isEdit: Boolean,
  value: Boolean,
});
const appConfig = useAppConfig()
const emit = defineEmits(['hidden', 'subject-saved']);

const subjectIdInput = ref(null)

let formId = 'newSubjectDialog'
let modalTitle = 'New Subject'
if (props.isEdit) {
  formId = `editSubjectDialog-${props.subject.projectId}-${props.subject.subjectId}`
  modalTitle = 'Editing Existing Subject'
}

let canAutoGenerateId = ref(true);
let displayIconManager = ref(false);
let tooltipShowing = ref(false);
let restoredFromStorage = ref(false);
let currentFocus = ref(null);
let previousFocus = ref(null);
let helpUrl = ref('');

onMounted(() => {
  document.addEventListener('focusin', trackFocus);
})

const trackFocus = () => {
  previousFocus.value = currentFocus.value;
  currentFocus.value = document.activeElement;
};

const title = computed(() => {
  return props.isEdit ? 'Editing Existing Subject' : 'New Subject';
});

const discardChanges = (reload = false) => {
  if (reload) {
    restoredFromStorage = false;
    // loadComponent();
  }
};


const close = () => { model.value = false }

const updateSubject = (values) => {
  const subjToSave = {
    ...values,
    isEdit: props.isEdit,
    name: InputSanitizer.sanitize(values.name),
    subjectId: InputSanitizer.sanitize(values.subjectId)
  }

  emit('subject-saved', subjToSave);
  close()
};

const updateSubjectId = (value) => {
  if (!props.isEdit && canAutoGenerateId) {
    let id = InputSanitizer.removeSpecialChars(value);
    // Subjects, skills and badges can not have same id under a project
    // by default append Subject to avoid id collision with other entities,
    // user can always override in edit mode
    if (id) {
      id = `${id}Subject`;
    }
    subjectIdInput.value.updateIdValue(id);
  }
};

const onSelectedIcon = (selectedIcon) => {
  // subjectInternal.iconClass = `${selectedIcon.css}`;
  displayIconManager = false;
};

const toggleIconDisplay = (shouldDisplay) => {
  displayIconManager = shouldDisplay;
};

const checkSubjectNameUnique = (value) => {
  if (value === props.subject.name) {
    return true;
  }
  return SubjectsService.subjectWithNameExists(props.subject.projectId, value);
}

const checkSubjectIdUnique = (value) => {
  if (value === props.subject.subjectId) {
    return true;
  }
  return SubjectsService.subjectWithIdExists(props.subject.projectId, value);
}

const validateHelpUrl = (value) => {
  if (!value) {
    return true;
  }
  return value.startsWith('http') || value.startsWith('https') || value.startsWith('/');
}

const schema = {
  'name': string()
      .trim()
      .required()
      .min(appConfig.minNameLength)
      .max(appConfig.maxSubjectNameLength)
      .nullValueNotAllowed()
      .test('uniqueName', 'Subject Name is already taken', (value) => checkSubjectNameUnique(value))
      .customNameValidator()
      .label('Subject Name'),
  'subjectId': string()
      .trim()
      .required()
      .min(appConfig.minIdLength)
      .max(appConfig.maxIdLength)
      .nullValueNotAllowed()
      .test('uniqueName', 'Subject ID is already taken', (value) => checkSubjectIdUnique(value))
      .customNameValidator()
      .label('Subject ID'),
  'description': string()
      .max(appConfig.descriptionMaxLength)
      .label('Subject Description'),
  'helpUrl': string()
      .test('help_url', 'Help URL must use http://, https://, or be a relative url.', (value) => {
        if (!value) {
          return true;
        }
        return value.startsWith('http') || value.startsWith('https') || value.startsWith('/');
      })
  // .test('customDescriptionValidator', (value, context) => customProjectDescriptionValidator(value, context))
};

const initialSubjData = {
  subjectId: props.subject.subjectId,
  name: props.subject.name,
  helpUrl: props.subject.helpUrl,
  description: props.subject.description,
  originalSubjectId: props.subject.subjectId,
  ...props.subject,
};
const asyncLoadData = () => { return; };

</script>

<template>
  <SkillsInputFormDialog
      :id="formId"
      v-model="model"
      :header="modalTitle"
      saveButtonLabel="Save"
      :validation-schema="schema"
      :initial-values="initialSubjData"
      @saved="updateSubject"
      @close="close">
    <template #default>

      <div v-if="displayIconManager === false">
<!--            <icon-picker :startIcon="subjectInternal.iconClass" @select-icon="toggleIconDisplay(true)"-->
<!--                         class="mr-3"></icon-picker>-->

        <SkillsTextInput label="Subject Name" name="name" :is-required="true" :autofocus="true" @input="updateSubjectId" @keydown-enter="emit('keydown-enter')" data-cy="subjectNameInput" />
        <SkillsIdInput ref="subjectIdInput" name="subjectId" @can-edit="canAutoGenerateId=!$event" label="Subject ID" @keydown-enter="emit('keydown-enter')" />

        <markdown-editor class="mt-5" name="description" />
<!--          <div class="mt-3">-->
<!--            <ValidationProvider rules="maxDescriptionLength|customDescriptionValidator" :debounce="250" v-slot="{ errors }" name="Subject Description">-->
<!--              <markdown-editor v-model="subjectInternal.description"-->
<!--                               :project-id="subjectInternal.projectId"-->
<!--                               :skill-id="isEdit ? subjectInternal.subjectId : null"-->
<!--                               aria-errormessage="subjectDescError"-->
<!--                               aria-describedby="subjectDescError"-->
<!--                               :aria-invalid="errors && errors.length > 0"/>-->
<!--              <small role="alert" id="subjectDescError" class="form-text text-danger" data-cy="subjectDescError">{{ errors[0] }}</small>-->
<!--            </ValidationProvider>-->
<!--          </div>-->

          <help-url-input class="mt-3"
                          :next-focus-el="previousFocus"
                          name="helpUrl"
                          @keydown-enter="emit('keydown-enter')" />

        </div>
        <div v-else>
<!--          <icon-manager @selected-icon="onSelectedIcon"></icon-manager>-->
          <div class="text-right mr-2">
            <SkillsButton variant="secondary" @click="toggleIconDisplay(false)" class="mt-4">Cancel Icon Selection</SkillsButton>
          </div>
        </div>
    </template>
  </SkillsInputFormDialog>
</template>

<style scoped></style>

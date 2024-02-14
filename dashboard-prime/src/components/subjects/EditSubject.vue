<script setup>
import { ref, computed, onMounted } from 'vue';
import SkillsInputFormDialog from "@/components/utils/inputForm/SkillsInputFormDialog.vue";
import {string} from "yup";
import {useRoute} from 'vue-router'
import { useAppConfig } from '@/components/utils/UseAppConfig.js'
import SubjectsService from '@/components/subjects/SubjectsService';
import InputSanitizer from '@/components/utils/InputSanitizer';
import MarkdownEditor from '@/common-components/utilities/markdown/MarkdownEditor.vue'
import HelpUrlInput from '@/components/utils/HelpUrlInput.vue';
import SkillsNameAndIdInput from '@/components/utils/inputForm/SkillsNameAndIdInput.vue'
import IconPicker from '@/components/utils/iconPicker/IconPicker.vue';
import IconManager from '@/components/utils/iconPicker/IconManager.vue';
import OverlayPanel from 'primevue/overlaypanel';

const model = defineModel()
const props = defineProps({
  subject: Object,
  isEdit: Boolean,
  value: Boolean,
});
const appConfig = useAppConfig()
const emit = defineEmits(['hidden', 'subject-saved']);
const route = useRoute()

// const subjectId = defineModel('subjectId');
// const subjectName = defineModel('subjectName');

let formId = 'newSubjectDialog'
let modalTitle = 'New Subject'
if (props.isEdit) {
  formId = `editSubjectDialog-${route.params.projectId}-${props.subject.subjectId}`
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


const close = () => { model.value = false }

const onSelectedIcon = (selectedIcon) => {
  displayIconManager.value = false;
  currentIcon.value = selectedIcon.css;
  op.value.hide();
};

const toggleIconDisplay = (event) => {
  // displayIconManager.value = shouldDisplay;
  op.value.toggle(event);
};

const checkSubjectNameUnique = (value) => {
  if (!value || value === props.subject.name) {
    return true;
  }
  return SubjectsService.subjectWithNameExists(route.params.projectId, value);
}

const checkSubjectIdUnique = (value) => {
  if (!value || value === props.subject.subjectId) {
    return true;
  }
  return SubjectsService.subjectWithIdExists(route.params.projectId, value);
}

const validateHelpUrl = (value) => {
  if (!value) {
    return true;
  }
  return value.startsWith('http') || value.startsWith('https') || value.startsWith('/');
}

const schema = {
  'subjectName': string()
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
      .customDescriptionValidator('Subject Description')
      .label('Subject Description'),
  'iconClass': string().required(),
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
  projectId: route.params.projectId,
  subjectId: props.subject.subjectId || '',
  subjectName: props.subject.name || '',
  helpUrl: props.subject.helpUrl || '',
  description: props.subject.description || '',
  iconClass: props.subject.iconClass || 'fas fa-book',
};

let currentIcon = ref((props.subject.iconClass || 'fas fa-book'));
let op = ref();

const updateSubject = (values) => {
  console.log(values);
  const subjToSave = {
    ...values,
    // iconClass: currentIcon,
    originalSubjectId: props.subject.subjectId,
    projectId: route.params.projectId,
    isEdit: props.isEdit,
    name: InputSanitizer.sanitize(values.subjectName),
    subjectId: InputSanitizer.sanitize(values.subjectId)
  }
  return SubjectsService.saveSubject(subjToSave).then((subjRes) => {
    return {
      ...subjRes,
      originalSubjectId: props.subject.subjectId,
    }
  })
};
const onSubjectSaved = (subject) =>{
  emit('subject-saved', subject);
  close()
}
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
      :save-data-function="updateSubject"
      :enable-return-focus="true"
      @saved="onSubjectSaved"
      @close="close">
    <template #default>

<!--      <div v-if="displayIconManager === false">-->
<!--        <icon-picker :startIcon="currentIcon" @select-icon="toggleIconDisplay"-->
<!--                     class="mr-3"></icon-picker>-->

      <button class="icon-button"
              @click="toggleIconDisplay"
              id="iconPicker"
              @keypress.enter="toggleIconDisplay"
              role="button"
              aria-roledescription="icon selector button"
              aria-label="icon selector"
              tabindex="0"
              data-cy="iconPicker">
        <div class="text-primary" style="min-height: 4rem;">
          <i :class="[currentIcon]" />
        </div>
      </button>

        <SkillsNameAndIdInput
          name-label="Subject Name"
          name-field-name="subjectName"
          id-label="Subject ID"
          id-field-name="subjectId"
          id-suffix="Subject"
          :name-to-id-sync-enabled="!props.isEdit" />

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

<!--        </div>-->
<!--        <div v-else>-->
      <OverlayPanel ref="op" appendTo="body">
          <icon-manager @selected-icon="onSelectedIcon" name="iconClass"></icon-manager>
          <div class="text-right mr-2">
            <SkillsButton variant="secondary" @click="toggleIconDisplay" class="mt-4">Cancel Icon Selection</SkillsButton>
          </div>
      </OverlayPanel>
<!--        </div>-->
    </template>
  </SkillsInputFormDialog>
</template>

<style scoped>
i {
  font-size: 3rem;
}

.icon-button {
  border: 1px solid rgba(0, 0, 0, 0.125);
  border-radius: 0.25em;
  background-color: #fff;
}

.icon-button:disabled {
  background-color: lightgrey;
  cursor: none;
}
</style>

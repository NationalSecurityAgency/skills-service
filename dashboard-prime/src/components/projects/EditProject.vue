<script setup>
import { ref, computed } from 'vue'
import { object, string } from 'yup'
import { useDebounceFn } from '@vueuse/core'
import InputSanitizer from '@/components/utils/InputSanitizer.js'
import ProjectService from '@/components/projects/ProjectService.js'
import { useAppConfig } from '@/components/utils/UseAppConfig.js'
import { useCommunityLabels } from '@/components/utils/UseCommunityLabels.js'
import MarkdownEditor from '@/common-components/utilities/markdown/MarkdownEditor.vue'
import SkillsNameAndIdInput from '@/components/utils/inputForm/SkillsNameAndIdInput.vue'
import SkillsInputFormDialog from '@/components/utils/inputForm/SkillsInputFormDialog.vue'
import SettingsService from '@/components/settings/SettingsService.js'
import { useStore } from 'vuex'

const model = defineModel()
const props = defineProps(['project', 'isEdit', 'isCopy'])
const emit = defineEmits(['project-saved'])
const store = useStore();

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

const schema = object({
  'projectName': string()
    .trim()
    .required()
    .min(appConfig.minNameLength)
    .max(appConfig.maxProjectNameLength)
    .nullValueNotAllowed()
    .test('uniqueName', 'Project Name already exist', (value) => checkProjNameUnique(value))
    .customNameValidator()
    .label('Project Name'),
  'projectId': string()
    .required()
    .min(appConfig.minIdLength)
    .max(appConfig.maxIdLength)
    .idValidator()
    .nullValueNotAllowed()
    .test('uniqueId', 'Project ID already exist', (value) => checkProjIdUnique(value))
    .label('Project ID'),
  'description': string()
    .max(appConfig.descriptionMaxLength)
    .customDescriptionValidator('Project Description', false, enableProtectedUserCommunity.value)
    .label('Project Description')
})
const initialProjData = {
  projectId: props.project.projectId || '',
  projectName: props.project.name || '',
  description: props.project.description || '',
}

const loadDescription = () => {
  return ProjectService.loadDescription(props.project.projectId).then((data) => {
    return { 'description': data.description || '' }
  })
}
const asyncLoadData = props.isEdit ? loadDescription : null

const close = () => { model.value = false }

const isRootUser = computed(() => {
  return store.getters['access/isRoot'];
});
const saveProject = (values) => {
  const projToSave = {
    ...values,
    originalProjectId: props.project.projectId,
    isEdit: props.isEdit,
    name: InputSanitizer.sanitize(values.projectName),
    projectId: InputSanitizer.sanitize(values.projectId)
  }
  return ProjectService.saveProject(projToSave)
    .then((projRes) => {
      if (!props.isEdit && isRootUser.value) {
        SettingsService.pinProject(projToSave.projectId)
          .then(() => {
            return {  ...projRes, originalProjectId: props.project.projectId }
          })
      }
      return {  ...projRes, originalProjectId: props.project.projectId }
    })
}

const onSavedProject = (savedProj) => {
  emit('project-saved', savedProj)
  close()
}

</script>

<template>
  <SkillsInputFormDialog
    :id="formId"
    v-model="model"
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
      <markdown-editor
        class="mt-5"
        name="description" />

      <!--      <div v-if="showManageUserCommunity" class="border rounded p-2 mt-3 mb-2" data-cy="restrictCommunityControls">-->
      <!--        <div v-if="isCopyAndCommunityProtected">-->
      <!--          <i class="fas fa-shield-alt text-danger" aria-hidden="true" /> Copying project whose access is restricted to <b class="text-primary">{{ userCommunityRestrictedDescriptor }}</b> users only and <b>cannot</b> be lifted/disabled-->
      <!--        </div>-->
      <!--        <div v-if="isEditAndCommunityProtected">-->
      <!--          <i class="fas fa-shield-alt text-danger" aria-hidden="true" /> Access is restricted to <b class="text-primary">{{ userCommunityRestrictedDescriptor }}</b> users only and <b>cannot</b> be lifted/disabled-->
      <!--        </div>-->
      <!--        <div v-if="!isEditAndCommunityProtected && !isCopyAndCommunityProtected">-->
      <!--          <ValidationObserver v-slot="{ pending, invalid }">-->
      <!--            <div class="row">-->
      <!--              <div class="col-lg">-->
      <!--                <ValidationProvider rules="projectCommunityRequirements"-->
      <!--                                    name="Failed Minimum Requirement" v-slot="{ errors }">-->
      <!--                  <b-form-checkbox v-model="internalProject.enableProtectedUserCommunity"-->
      <!--                                   @change="userCommunityChanged"-->
      <!--                                   name="check-button" inline switch data-cy="restrictCommunity">-->
      <!--                    Restrict <i class="fas fa-shield-alt text-danger" aria-hidden="true" /> Access to <b class="text-primary">{{ userCommunityRestrictedDescriptor }}</b> users only-->
      <!--                  </b-form-checkbox>-->

      <!--                  <div v-if="invalid" class="alert alert-danger mb-3 mt-1" data-cy="communityValidationErrors" role="alert">-->
      <!--                    <div>-->
      <!--                      <i class="fas fa-exclamation-triangle text-danger mr-1" aria-hidden="true" />-->
      <!--                      <span>Unable to restrict access to {{ userCommunityRestrictedDescriptor }} users only:</span>-->
      <!--                    </div>-->
      <!--                    <span v-html="errors[0]"/>-->
      <!--                  </div>-->
      <!--                </ValidationProvider>-->
      <!--              </div>-->
      <!--              <div v-if="userCommunityDocsLink" class="col-lg-auto" data-cy="userCommunityDocsLink">-->
      <!--                <a :href="userCommunityDocsLink" target="_blank" style="text-decoration: underline">{{ userCommunityDocsLabel }}</a>-->
      <!--                <i class="fas fa-external-link-alt ml-1" aria-hidden="true" style="font-size: 0.9rem;"/>-->
      <!--              </div>-->
      <!--            </div>-->
      <!--            <div v-if="!pending">-->
      <!--              <div v-if="internalProject.enableProtectedUserCommunity && !invalid" class="alert-warning alert mb-0 mt-1" data-cy="communityRestrictionWarning">-->
      <!--                <i class="fas fa-exclamation-triangle text-danger" aria-hidden="true" /> Please note that once the restriction is enabled it <b>cannot</b> be lifted/disabled.-->
      <!--              </div>-->
      <!--            </div>-->
      <!--          </ValidationObserver>-->
      <!--        </div>-->
      <!--      </div>-->
      <!--      <div class="row">-->
      <!--        <div class="mt-2 col-12">-->
      <!--          <ValidationProvider rules="maxDescriptionLength|customProjectDescriptionValidator" :debounce="250" v-slot="{errors}"-->
      <!--                              name="Project Description">-->
      <!--            <markdown-editor v-if="!isEdit || descriptionLoaded"-->
      <!--                             v-model="internalProject.description"-->
      <!--                             :project-id="internalProject.projectId"-->
      <!--                             :allow-attachments="isEdit || !showManageUserCommunity"-->
      <!--                             @input="updateDescription" />-->
      <!--            <small role="alert" class="form-text text-danger mb-3" data-cy="projectDescriptionError">{{ errors[0] }}</small>-->
      <!--          </ValidationProvider>-->
      <!--        </div>-->
      <!--      </div>-->
      <!--      <p v-if="invalid && overallErrMsg" class="text-center text-danger mt-2" aria-live="polite"><small>***{{ overallErrMsg }}***</small></p>-->

    </template>
  </SkillsInputFormDialog>
</template>

<style scoped>

</style>
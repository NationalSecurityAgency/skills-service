<script setup>
import { ref } from 'vue'
import * as yup from 'yup'
import { useForm } from 'vee-validate'
import SkillsSpinner from '@/components/utils/SkillsSpinner.vue'
import InputSanitizer from '@/components/utils/InputSanitizer.js'
import ProjectService from '@/components/projects/ProjectService.js'
import debounce from 'lodash.debounce'
import IdInput from '@/components/utils/inputForm/IdInput.vue'

const model = defineModel()
const props = defineProps(['project', 'isEdit', 'isCopy'])
const getTitle = () => {
  if (props.isCopy) {
    return 'Copy Project'
  }
  return props.isEdit ? 'Editing Existing Project' : 'New Project'
}
const loadingComponent = ref(false)
// const internalProject = ref({
//   originalProjectId: props.project.projectId,
//   projectId: props.project.projectId,
//   isEdit: props.isEdit,
//   description: '',
//   ...props.project,
// })

const checkProjNameUnique = debounce((projName) => {
  if (!projName || projName.length === 0) {
    return true
  }
  const origName = props.project.projectId
  if (props.isEdit && (origName === value || origName.localeCompare(value, 'en', { sensitivity: 'base' }) === 0)) {
    return true
  }
  return ProjectService.checkIfProjectNameExist(projName).then((remoteRes) => !remoteRes)
}, 500)

const schema = yup.object({
  'name': yup.string().required().min(5)
    .test('uniquename', 'Project Name already exist', (value) => checkProjNameUnique(value))
    .label('Project Name'),
  'projectId': yup.string()
    .required()
    .min(5)
    .label('Projet Id')
})
const { values, defineField, errors, meta, handleSubmit } = useForm({
  validationSchema: schema,
  initialValues: props.project
})
const [name, nameAttrs] = defineField('name')
const [projectId, projectIdAttrs] = defineField('projectId', {
  props: state => ({
    error: state.errors[0],
  }),
})

const canEditProjectId = ref(false)

function updateProjectId() {
  // if (!props.isEdit && !canEditProjectId) {
  //   internalProject.projectId = InputSanitizer.removeSpecialChars(internalProject.name);
  // }
}

function close() {
  // this.clearComponentState(this.componentName);
  // this.hideModal(e);
  model.value = false
}

const onSubmit = handleSubmit(values => {
  console.log(JSON.stringify(values, null, 2))
})

</script>

<template>
  <Dialog modal
          v-model:visible="model"
          :maximizable="true"
          :header="getTitle()"
          class="w-11 lg:w-10 xl:w-9"
  >
    <skills-spinner :is-loading="loadingComponent" />

    <div v-if="!loadingComponent" v-focustrap>
      <!--      <ReloadMessage v-if="restoredFromStorage" @discard-changes="discardChanges" />-->
<!--            <pre> {{ errors }}</pre>-->
<!--      <pre>{{ meta }}</pre>-->

      <div class="field text-left">
        <label for="projectIdInput">* {{ isCopy ? 'New Project Name' : 'Project Name' }}</label>
        <!--            <ValidationProvider rules="required|minNameLength|maxProjectNameLength|uniqueName|customNameValidator|nullValueNotAllowed"-->
        <!--                                v-slot="{errors}"-->
        <!--                                :debounce="250"-->
        <!--                                name="Project Name">-->
        <InputText
          class="w-full"
          type="text"
          title="Name"
          v-model="name"
          v-bind="nameAttrs"
          v-on:input="updateProjectId"
          v-on:keydown.enter="handleSubmit(updateProject)"
          data-cy="projectName"
          autofocus
          id="name"
          :class="{ 'p-invalid': errors.name }"
          :aria-invalid="errors.name ? null : true"
          aria-errormessage="projectNameError"
          aria-describedby="projectNameError" />
        <small role="alert" class="p-error" data-cy="projectNameError"
               id="projectNameError">{{ errors.name || '&nbsp;' }}</small>
        <!--            <small class="p-error" id="username-error">{{ errors.username || '&nbsp;' }}</small>-->
        <!--            </ValidationProvider>-->
      </div>

      <div class="">
        <id-input
          name="projectId"
          v-model="projectId"
          v-bind="projectIdAttrs"
          :label="`${props.isCopy ? 'New Project ID' : 'Project ID'}`"
        />
<!--          additional-validation-rules="uniqueId"-->
<!--          @can-edit="canEditProjectId=$event"-->
<!--          v-on:keydown.enter.native="handleSubmit(updateProject)"-->
<!--          :next-focus-el="previousFocus"-->
<!--          @shown="tooltipShowing=true"-->
<!--          @hidden="tooltipShowing=false" />-->
      </div>

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


    <div class="text-right mt-5">
      <Button severity="warning"
              outlined size="small"
              class="float-right mr-2"
              @click="close"
              data-cy="closeProjectButton">
        <span>Cancel</span><i class="far fa-times-circle ml-1" aria-hidden="true"></i>
      </Button>
      <Button severity="success"
              outlined size="small"
              class="float-right"
              @click="onSubmit"
              :disabled="!meta.valid"
              data-cy="saveProjectButton">
        <span>{{ isCopy ? 'Copy Project' : 'Save' }}</span><i class="far fa-save ml-1" aria-hidden="true"></i>
      </Button>
    </div>
    </div>
  </Dialog>
</template>

<style scoped>

</style>
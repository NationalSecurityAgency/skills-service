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
import { computed, onMounted, ref } from 'vue'
import { useSkillsAnnouncer } from '@/common-components/utilities/UseSkillsAnnouncer.js'
import MyProgressService from '@/components/myProgress/MyProgressService.js'
import SkillsInputFormDialog from '@/components/utils/inputForm/SkillsInputFormDialog.vue'
import { object, string } from 'yup'
import { useAppConfig } from '@/common-components/stores/UseAppConfig.js'
import { useRoute } from 'vue-router'
import SkillsSpinner from '@/components/utils/SkillsSpinner.vue'
import { useMyProgressState } from '@/stores/UseMyProgressState.js'

const model = defineModel()
const announcer = useSkillsAnnouncer()
const appConfig = useAppConfig()
const route = useRoute()
const myProgressState = useMyProgressState()

console.log(appConfig);

const props = defineProps({
  projectId: {
    type: String,
    required: false
  },
  useProjectIdAsName: {
    type: Boolean,
    required: false
  },
  saveButtonLabel: {
    type: String,
    default: 'Send'
  }
})

const showSelectProjectComponents = ref(false)

const showProjectContactComponents = computed(() => projectInfo.value?.projectName)
const preparingMyProjectSelector = ref(true)
const projectInfo = ref({})
const loadingData = ref(true)
onMounted(() => {
  const projectId = props.projectId || route.params.projectId;
  if (projectId) {
    preparingMyProjectSelector.value = false
    if (props.useProjectIdAsName) {
      projectInfo.value = {projectId: projectId, projectName: projectId};
      loadingData.value = false;
    } else {
      MyProgressService.findProjectName(projectId)
          .then((proj) => {
            projectInfo.value = {...proj, projectName: proj.name};
          })
          .finally(() => {
            loadingData.value = false;
          })
    }
  } else {
    myProgressState.afterMyProjectsLoaded()
        .then((myProjects) => {
          if (myProjects?.length === 1) {
            projectInfo.value = myProjects[0]
            showSelectProjectComponents.value = false;
          } else {
            showSelectProjectComponents.value = true;
          }
        }).finally(() => {
      loadingData.value = false;
      preparingMyProjectSelector.value = false
    })
  }
})

const projectName = computed(() => projectInfo.value.projectName || "...")

const schema = object({
  'message': string()
      .trim()
      .required()
      .min(10)
      .max(appConfig.maxContactOwnersMessageLength)
      .customDescriptionValidator('Message', false)
      .label('Message')
})
const sent = ref(false)
const contactProjectAdmins = (values) => {
  return MyProgressService.contactOwners(projectInfo.value.projectId, values.message).then(() => {
    announcer.polite(`Message has been sent to owners of project ${props.projectName}`)
    sent.value = true
    return {}
  })
}
const close = () => {

  model.value = false
}
</script>

<template>
  <SkillsInputFormDialog
      id="contactProjectAdmins"
      v-model="model"
      save-button-label="Contact"
      save-button-icon="fas fa-envelope-open-text"
      :cancelButtonLabel="sent ? 'OK' : 'Cancel'"
      :cancel-button-icon="sent ? 'fas fa-check' : 'fas fa-times'"
      :cancel-button-severity="sent ? 'success' : 'warn'"
      :show-save-button="!sent"
      :save-data-function="contactProjectAdmins"
      header="Contact"
      :saveButtonLabel="saveButtonLabel"
      :validation-schema="schema"
      :enable-return-focus="true"
      :initialValues="{}"
      :close-on-success="false"
      data-cy="contactProjectOwnerDialog"
  >
    <skills-spinner v-if="preparingMyProjectSelector" :is-loading="preparingMyProjectSelector"/>
    <div v-if="!preparingMyProjectSelector && showSelectProjectComponents && !sent" class="mb-5">
      <div class="card flex flex-col gap-2 justify-center">
        <label>Which training program would you like to ask about?</label>
        <Select
            v-model="projectInfo"
            :options="myProgressState.myProjects"
            optionLabel="projectName"
            data-cy="myProjectSelector"
            placeholder="Select a training to contact"
            class="w-full lg:w-[50rem]"/>
      </div>
    </div>
    <div v-if="showProjectContactComponents">
      <div v-if="!sent">
        <SkillsTextarea
            :label="`Message for Admins of ${projectName} project`"
            :is-required="true"
            :submit-on-enter="false"
            data-cy="contactOwnersMsgInput"
            :max-num-chars="appConfig.maxContactOwnersMessageLength ? Number(appConfig.maxContactOwnersMessageLength) : 0"
            rows="10"
            name="message">
          <template #label>
            <span>Send your message to the administrators of <strong class="text-primary"
                                                                     v-if="!loadingData">{{ projectName }}</strong><skills-spinner
                v-else :is-loading="true" :size-in-rem="1" :is-inline="true"/> training</span>
          </template>
        </SkillsTextarea>
      </div>

      <div v-if="sent" data-cy="contactOwnerSuccessMsg">
        <Message :closable="false" severity="success" icon="fa fa-check">
          Message sent!
        </Message>
        <p v-if="projectName" class="mb-8">The Project Administrator(s) of <strong class="text-primary">{{ projectName }}</strong>
          will be notified of your question via email.</p>
        <p v-else>The Project Administrator(s) will be notified of your question via email.</p>
      </div>
    </div>

    <div class="mt-4 text-center" v-if="appConfig.contactSupportEnabled">
      <hr
          class="mb-2 h-px border-t-0 bg-transparent bg-gradient-to-r from-transparent via-neutral-500 to-transparent opacity-25 dark:via-neutral-400"/>
      Found a software bug or a SkillTree system-wide issue?
      <router-link to="/support" class="underline" @click="close">Contact SkillTree Support <i
          class="fa-solid fa-up-right-from-square" aria-hidden="true"></i></router-link>
    </div>

  </SkillsInputFormDialog>
</template>

<style scoped>

</style>
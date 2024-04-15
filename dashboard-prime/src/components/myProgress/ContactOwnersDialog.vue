<script setup>
import { ref } from 'vue'
import { useSkillsAnnouncer } from '@/common-components/utilities/UseSkillsAnnouncer.js'
import MyProgressService from '@/components/myProgress/MyProgressService.js'
import SkillsInputFormDialog from '@/components/utils/inputForm/SkillsInputFormDialog.vue'
import { object, string } from 'yup'
import { useAppConfig } from '@/common-components/stores/UseAppConfig.js'

const model = defineModel()
const announcer = useSkillsAnnouncer()
const appConfig = useAppConfig()

const props = defineProps({
  projectName: {
    type: String,
    required: false
  },
  projectId: {
    type: String,
    required: true
  }
})
const schema = object({
  'message': string()
    .trim()
    .required()
    .min(10)
    .max(appConfig.maxContactOwnersMessageLength)
    .label('Message')
})
const sent = ref(false)
const contactProjectAdmins = (values) => {
  return MyProgressService.contactOwners(props.projectId, values.message).then(() => {
    announcer.polite(`Message has been sent to owners of project ${props.projectName}`)
    sent.value = true
    return {}
  })
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
    :cancel-button-severity="sent ? 'success' : 'warning'"
    :show-save-button="!sent"
    :save-data-function="contactProjectAdmins"
    :header="`Contact ${props.projectName ? props.projectName : 'Project'}`"
    saveButtonLabel="Save"
    :validation-schema="schema"
    :enable-return-focus="true"
    :close-on-success="false"
    data-cy="contactProjectOwnerDialog"
  >
    <div v-if="!sent">
      <SkillsTextarea
        :label="`Message for Admins of ${projectName} project`"
        :is-required="true"
        data-cy="contactOwnersMsgInput"
        :max-num-chars="appConfig.maxContactOwnersMessageLength ? Number(appConfig.maxContactOwnersMessageLength) : 0"
        rows="10"
        name="message">
        <template #label>
          <span>Message for Admins of <strong class="text-primary">{{projectName}}</strong> project</span>
        </template>
      </SkillsTextarea>
    </div>

    <div v-if="sent" data-cy="contactOwnerSuccessMsg">
      <Message :closable="false" severity="success" icon="fa fa-check">
        Message sent!
      </Message>
      <p v-if="projectName">The Project Administrator(s) of <strong class="text-primary">{{projectName}}</strong> will be notified of your question via email.</p>
      <p v-else>The Project Administrator(s) will be notified of your question via email.</p>
    </div>
  </SkillsInputFormDialog>
</template>

<style scoped>

</style>
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
import { ref, computed, nextTick, toRaw } from 'vue'
import { useAppConfig } from '@/common-components/stores/UseAppConfig.js'
import { useSkillsAnnouncer } from '@/common-components/utilities/UseSkillsAnnouncer.js'
import AccessService from '@/components/access/AccessService.js'
import { useRoute } from 'vue-router'
import EmailField from "@/components/access/invite-only/EmailField.vue";

const emit = defineEmits(['invites-sent'])

const appConfig = useAppConfig()
const announcer = useSkillsAnnouncer()
const route = useRoute()

const loading = ref(false)
const sending = ref(false)

const expirationTime = ref({ value: 'P7D', text: '7 days' })
const expirationOptions = ref([
  { value: 'PT30M', text: '30 minutes' },
  { value: 'PT8H', text: '8 hours' },
  { value: 'PT24H', text: '24 hours' },
  { value: 'P7D', text: '7 days' },
  { value: 'P30D', text: '30 days' }
])
const currentEmails = ref('')
const ccEmails = ref('')
const invalidEmails = ref('')
const failedEmails = ref('')
const failedEmailsErrors = ref(null)
const successMsg = ref('')
const showSuccessMsg = ref(false)

const inviteRecipients = ref([])
const ccRecipients = ref([])

const hasEmails = computed(() => currentEmails.value.length > 0 || ccEmails.value.length > 0)
const tooManyEmails = computed(() => inviteRecipients.value.length >= appConfig.maxProjectInviteEmails)
const splitCurrentEmails = (emails) => {
  return emails.value.split(/;|,|\r?\n/)
}

const validEmail = /^[a-zA-Z0-9_!#$%&'*+/=?`{|}~^.-]{1,64}@[a-zA-Z0-9.-]{1,64}$/i
const isValidEmail = (pmail) => {
  return validEmail.test(pmail)
}

const stripNames = /<([^\s<>@]+@[^\s<>@]+)>/
const removeName = (pmail) => {
  if (stripNames.test(pmail)) {
    return pmail.match(stripNames)[1]
  }
  return pmail
}
const addEmails = () => {
  invalidEmails.value = ''
  if(currentEmails.value.length > 0) {
    const emailsToAdd = addEmailGroup(currentEmails)
    inviteRecipients.value = Array.from(new Set([...emailsToAdd, ...inviteRecipients.value]))
  }
  if(ccEmails.value.length > 0) {
    const emailsToAdd = addEmailGroup(ccEmails)
    ccRecipients.value = Array.from(new Set([...emailsToAdd, ...ccRecipients.value]))
  }
}

const addEmailGroup = (emails) => {
  if (emails.value.length > 0) {
    const potentialEmails = splitCurrentEmails(emails)
    const invalid = []
    let successful = 0
    const recipients = []

    const maxReached = potentialEmails.filter((pmail) => {
      if (tooManyEmails.value) {
        return true
      }
      let email = removeName(pmail)
      email = email.trim()
      const isValid = isValidEmail(email)
      if (!isValid) {
        invalid.push(pmail)
      } else if (!recipients.find((invited) => invited === email)) {
        successful += 1
        recipients.push(email)
      }
      return false
    })

    failedEmails.value = ''
    failedEmailsErrors.value = null

    if (successful > 0) {
      nextTick(() => announcer.polite(`added ${successful} project invite email recipients`))
    }
    if (invalid.length > 0) {
      if(invalidEmails.value !== '') {
        invalidEmails.value += ', '
      }
      invalidEmails.value += invalid.join(', ')
    }
    if (maxReached.length > 0 || invalid.length > 0) {
      emails.value = [...invalid, ...maxReached].join('\n')
    } else {
      emails.value = ''
    }

    return recipients
  }
}

const removeRecipient = (email) => {
  const idx = inviteRecipients.value.indexOf(email)
  inviteRecipients.value.splice(idx, 1)
}
const removeCcRecipient = (email) => {
  const idx = ccRecipients.value.indexOf(email)
  ccRecipients.value.splice(idx, 1)
}

const maxRecipients = 50
const sendInviteDisabled = computed(() => {
  return inviteRecipients.value.length === 0 || inviteRecipients.value.length > maxRecipients || !expirationTime.value?.value || sending.value
})

const sendInvites = () => {
  failedEmails.value = ''
  failedEmailsErrors.value = null
  sending.value = true

  const inviteRequest = {
    validityDuration: toRaw(expirationTime.value?.value),
    recipients: toRaw(inviteRecipients.value),
    ccRecipients: toRaw(ccRecipients.value)
  }

  AccessService.sendProjectInvites(route.params.projectId, inviteRequest).then((resp) => {
    inviteRecipients.value = []
    if (resp.unsuccessful) {
      failedEmails.value = resp.unsuccessful.join(', ')
      failedEmailsErrors.value = resp.unsuccessfulErrors
      if (currentEmails.value) {
        currentEmails.value += `\n${resp.unsuccessful.join('\n')}`
      } else {
        currentEmails.value += resp.unsuccessful.join('\n')
      }
      ccEmails.value = ccRecipients.value
    }
    if (resp.successful.length > 0) {
      successMsg.value = `Successfully sent ${resp.successful.length} project invite(s)`
      showSuccessMsg.value = true
      setTimeout(() => {
        showSuccessMsg.value = false
      }, 4000)
      emit('invites-sent')
    }
  }).finally(() => {
    sending.value = false
    ccRecipients.value = []
  })
}

const inviteEmailsUpdated = (newEmails) => {
  currentEmails.value = newEmails;
}

const ccEmailsUpdated = (newEmails) => {
 ccEmails.value = newEmails;
}
</script>

<template>
  <div class="">
    <skills-spinner :is-loading="loading" />
    <div v-if="!loading">
      <div class="mb-4 field" data-cy="inviteExpiration">
        <label class="" id="inviteExpirationLabel" for="expirationTime">
          Invite Expiration:
        </label>
        <Select id="expirationTime"
                  v-model="expirationTime"
                  :options="expirationOptions"
                  optionLabel="text"
                  placeholder="Select expiration time"
                  aria-labelledby="inviteExpirationLabel"
                  data-cy="inviteExpirationSelect"
                  class="w-full" />
        <small class="italic">
          ** How long the project invite will be valid for before it expires.
        </small>
      </div>

      <div class="field pt-4" data-cy="inviteEmail">
        <EmailField label="Email Addresses" description="** Email Addresses of users to invite to access this project. Must be unique as each email address will be
          sent a one-time use invite token. Comma separated, semi-colon separated, and one email per line input formats
          are supported." field="invite" @updateAddresses="inviteEmailsUpdated" v-model="currentEmails" />

        <EmailField label="CC" description="** Optional. Users to CC on the invites. Each user will receive a copy of each invite sent.
          Comma separated, semi-colon separated, and one email per line input formats
          are supported." field="cc" @updateAddresses="ccEmailsUpdated" v-model="ccEmails" :rows="1" />

        <div class="mt-2">
          <SkillsButton
            @click="addEmails"
            label="Add Recipients"
            icon="fas fa-plus-circle"
            severity="info"
            size="small"
            class="mb-2"
            data-cy="addEmails"
            :disabled="!hasEmails"
            aria-label="Add email addresses to the list of recipients" />
        </div>
        <Message
          v-if="tooManyEmails"
          data-cy="maxEmailRecipients"
          severity="error"
          :closeable="false">
          Only {{ appConfig.maxProjectInviteEmails }} recipients can be invited at one time
        </Message>
        <Message v-if="invalidEmails"
                 severity="error"
                 :closeable="false"
                 data-cy="invalidEmails">
          Unable to add the following invalid email recipients: {{ invalidEmails }}
        </Message>

      </div>

      <div class="mb-4" data-cy="inviteRecipients">
        <Chip v-for="(email) of inviteRecipients"
              :key="email"
              :label="email"
              class="mr-2"
              removable
              @remove="removeRecipient(email)"
              data-cy="inviteRecipient">
        </Chip>
      </div>
      <label v-if="ccRecipients?.length > 0">CC To:</label>
      <div class="mb-4" data-cy="ccRecipients">
        <Chip v-for="(email) of ccRecipients"
              :key="email"
              :label="email"
              class="mr-2"
              removable
              @remove="removeCcRecipient(email)"
              data-cy="ccRecipient">
        </Chip>
      </div>
      <hr />
      <SkillsButton @click="sendInvites"
                    label="Send Invites"
                    class="mt-2"
                    icon="fas fa-paper-plane"
                    :disabled="sendInviteDisabled"
                    variant="outline-info"
                    :aria-label="`send project invites to ${inviteRecipients.length} users`"
                    data-cy="sendInvites-btn" />
      <Message
        icon="fa fa-check"
        v-if="showSuccessMsg"
        severity="success"
        data-cy="invitationsSentAlert">
        {{ successMsg }}
      </Message>
      <Message v-if="failedEmails"
               severity="error"
               :closable="false"
               data-cy="failedEmails">
        <div>
          Unable to send invites to:
        </div>
        <div v-for="failedEmailError in failedEmailsErrors" :key="failedEmailError" class="pl-1">
          - {{ failedEmailError }}
        </div>
      </Message>
    </div>
  </div>
</template>

<style scoped>

</style>
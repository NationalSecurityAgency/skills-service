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
import { onMounted, ref } from 'vue'
import MarkdownText from '@/common-components/utilities/markdown/MarkdownText.vue'
import AccessService from '@/components/access/AccessService.js'
import { useRoute, useRouter } from 'vue-router'
import { useAppInfoState } from '@/stores/UseAppInfoState.js'
import SettingsService from '@/components/settings/SettingsService.js'
import { useAuthState } from '@/stores/UseAuthState.js'

const router = useRouter()
const route = useRoute()
const appInfoState = useAppInfoState()
const authState = useAuthState()

const loading = ref(true)
const userAgreement = ref('')
const uaVersion = ref('')
const isSaving = ref(false)

onMounted(() => {
  loadAgreement()
})

const loadAgreement = () => {
  loading.value = true
  AccessService.getUserAgreement().then((ua) => {
    if (ua) {
      userAgreement.value = ua.userAgreement
      uaVersion.value = ua.currentVersion
    }
  }).finally(() => {
    loading.value = false
  })
}

const acknowledgeUa = () => {
  isSaving.value = true
  const ack = {
    settingGroup: 'user',
    setting: 'viewed_user_agreement',
    value: uaVersion.value
  }
  SettingsService.saveUserSettings([ack]).then(() => {
    appInfoState.setShowUa(false)
    // redirect to original page
    router.push(route.query.redirect || '/')
  }).finally(() => {
    isSaving.value = false
  })
}

const signOut = () => {
  authState.logout()
}
</script>

<template>
  <Card class="mt-4">
    <template #content>
      <skills-spinner :is-loading="loading" v-if="loading" />

      <div v-if="!loading">
        <markdown-text data-cy="userAgreement" :text="userAgreement" />
      </div>
    </template>

    <template #footer>
      <SkillsButton
        label="No Thanks"
        icon="fas fa-ban"
        severity="danger"
        size="small"
        :loading="isSaving"
        v-on:click="signOut"
        data-cy="rejectUserAgreement" />

      <SkillsButton
        label="I Agree"
        icon="fas fa-arrow-circle-right"
        size="small"
        class="ml-2"
        :loading="isSaving"
        @click="acknowledgeUa"
        data-cy="acknowledgeUserAgreement" />
    </template>
  </Card>
</template>

<style scoped>

</style>
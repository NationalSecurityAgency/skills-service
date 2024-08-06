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
import SkillsCardHeader from '@/components/utils/cards/SkillsCardHeader.vue'
import { useRoute } from 'vue-router'
import { computed, onMounted, ref } from 'vue'
import AccessService from '@/components/access/AccessService.js'
import { useDialogMessages } from '@/components/utils/modal/UseDialogMessages.js'

const route = useRoute()
const dialogMessages = useDialogMessages()

const projectId = computed(() => route.params.projectId)

const loadingSecret = ref(true)
const clientSecret = ref('')

onMounted(() => {
  loadClientSecret()
})

const loadClientSecret = () => {
  loadingSecret.value = true
  AccessService.getClientSecret(projectId.value)
    .then((clientSecretRes) => {
      clientSecret.value = clientSecretRes
    }).finally(() => {
    loadingSecret.value = false
  })
}

const resetClientSecret = () => {
  const message = 'Are you sure you want reset the client secret? Your current client secret will no longer work after reset and you will need to update any application configuration using the old secret.'
  dialogMessages.msgConfirm({
    message: message,
    header: 'Reset Secret?',
    accept: () => {
      AccessService.resetClientSecret(projectId.value)
          .then((clientSecretRes) => {
            clientSecret.value = clientSecretRes
          })
    }
  });
}
</script>

<template>
  <Card data-cy="trusted-client-props-panel">
    <template #header>
      <SkillsCardHeader title="Trusted Client Properties"></SkillsCardHeader>
    </template>
    <template #content>
      <skills-spinner v-if="loadingSecret" :is-loading="loadingSecret" class="my-5" />
      <div v-if="!loadingSecret">
        <div class="flex flex-column sm:flex-row gap-2">
          <div class="text-color-secondary">Client ID:</div>
          <div class="">{{ projectId }}</div>
        </div>
        <div class="mt-2 flex flex-column sm:flex-row gap-2">
          <div class="text-color-secondary">Client Secret:</div>
          <div style="text-wrap: wrap; overflow-wrap: break-word;" class="max-w-12rem sm:max-w-max">{{ clientSecret }}</div>
        </div>
        <SkillsButton
          label="Reset Client Secret"
          icon="fas fa-sync-alt"
          size="small"
          id="resetClientButton"
          :track-for-focus="true"
          @click="resetClientSecret"
          class="mt-3" />
      </div>
    </template>
  </Card>
</template>

<style scoped>

</style>
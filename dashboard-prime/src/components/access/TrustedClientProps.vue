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
  dialogMessages.msgConfirm(message, 'Reset Secret?', () => {
    AccessService.resetClientSecret(projectId.value)
      .then((clientSecretRes) => {
        clientSecret.value = clientSecretRes
      })
  })
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
        <div>
          <span class="text-color-secondary">Client ID:</span>
          <span class="ml-2">{{ projectId }}</span>
        </div>
        <div class="mt-2">
          <span class="text-color-secondary">Client Secret:</span>
          <span class="ml-2">{{ clientSecret }}</span>
        </div>
        <SkillsButton
          label="Reset Client Secret"
          icon="fas fa-sync-alt"
          size="small"
          @click="resetClientSecret"
          class="mt-3" />
      </div>
    </template>
  </Card>
</template>

<style scoped>

</style>
<script setup>
import { onMounted, ref } from 'vue'
import BootstrapService from '@/components/access/BootstrapService.js'
import Logo1 from '@/components/brand/Logo1.vue'

const isLoading = ref(true)

const refresh = () => {
  window.location.reload()
}

onMounted(() => {
  BootstrapService.grantRoot()
    .then(() => {
      isLoading.value = false
    })
})
</script>

<template>
  <div class="flex justify-content-center">
    <Card class="mt-3 text-center w-11">
    <template #content>
      <logo1 />
      <div v-if="isLoading" class="justify-content-center mt-4">
        <skills-spinner :is-loading="true" />
        <div class="mt-2 text-primary text-xl">Getting Things Ready!</div>
        <div class="text-color-secondary">This may take just a second...</div>
      </div>
      <div v-else>
        <Message icon="far fa-check-square" severity="success" :closable="false">
          The root account has been successfully created!
        </Message>

        <Message icon="far fa-check-square" severity="success" :closable="false">
          Inception self-training project created!
        </Message>
        <p class="mt-2">Please proceed to the SkillTree Dashboard.</p>
        <SkillsButton
          label="Let's Get Started!"
          icon="far fa-smile-beam"
          @click="refresh"
          class="mt-2"
          severity="success" />
      </div>
    </template>
  </Card>
  </div>
</template>

<style scoped>

</style>
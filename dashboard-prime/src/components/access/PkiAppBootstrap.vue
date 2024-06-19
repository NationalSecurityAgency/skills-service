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
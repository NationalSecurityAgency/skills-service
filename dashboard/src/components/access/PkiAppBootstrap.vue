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
import AccessPageCard from '@/components/access/AccessPageCard.vue'

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
  <AccessPageCard
    :icon="isLoading ? 'fas fa-gears' : 'fas fa-circle-check'"
    labelled-by="bootstrap-title">
    <div v-if="isLoading">
      <p class="mb-2 text-xs font-bold uppercase tracking-[0.12em] text-blue-600">Setting up SkillTree</p>
      <h1 id="bootstrap-title" class="m-0 text-[clamp(1.5rem,4vw,2rem)] leading-[1.2] text-gray-900">
        Getting things ready
      </h1>
      <div class="mt-6 flex flex-col items-center">
        <skills-spinner :is-loading="true" />
        <p class="mb-0 mt-4 text-gray-600">This may take just a second...</p>
      </div>
    </div>

    <div v-else>
      <p class="mb-2 text-xs font-bold uppercase tracking-[0.12em] text-blue-600">Setup complete</p>
      <h1 id="bootstrap-title" class="m-0 text-[clamp(1.5rem,4vw,2rem)] leading-[1.2] text-gray-900">
        SkillTree is ready
      </h1>
      <p class="mx-auto mb-7 mt-4 max-w-108 leading-[1.7] text-gray-600">
        Your SkillTree environment has been successfully initialized.
      </p>

      <div class="flex flex-col gap-3 text-left">
        <Message icon="far fa-check-square" severity="success" :closable="false">
          The root account has been successfully created!
        </Message>

        <Message icon="far fa-check-square" severity="success" :closable="false">
          Inception self-training project created!
        </Message>
      </div>

      <p class="mt-7 text-gray-600">Please proceed to the SkillTree Dashboard.</p>
      <div class="text-center">
        <SkillsButton
          label="Let's Get Started!"
          icon="far fa-smile-beam"
          @click="refresh"
          severity="success" />
      </div>
    </div>
  </AccessPageCard>
</template>

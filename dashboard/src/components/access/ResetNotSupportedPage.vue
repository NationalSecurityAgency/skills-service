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
import AccessPageCard from '@/components/access/AccessPageCard.vue';
</script>

<template>
  <AccessPageCard
    data-cy="resetNotSupported"
    icon="fas fa-lock"
    labelled-by="reset-not-supported-title">
    <p class="mb-2 text-xs font-bold uppercase tracking-[0.12em] text-blue-600">Password reset unavailable</p>
    <h1 id="reset-not-supported-title" class="m-0 text-[clamp(1.5rem,4vw,2rem)] leading-[1.2] text-gray-900">
      Password reset is not enabled
    </h1>
    <p class="mx-auto mb-7 mt-4 max-w-108 leading-[1.7] text-gray-600">
      Password reset is not currently enabled on this system. Please contact your SkillTree administrator for assistance.
    </p>

    <div class="text-center">
      <router-link class="inline-block no-underline" :to="{ name: 'Login' }" data-cy="loginPage">
        <SkillsButton
          icon="fas fa-arrow-left"
          outlined
          label="Return to Login Page" />
      </router-link>
    </div>
  </AccessPageCard>
</template>

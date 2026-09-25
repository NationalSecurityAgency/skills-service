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
    data-cy="emailVerificationSentConfirmation"
    icon="fas fa-envelope-circle-check"
    labelled-by="verification-title">
    <p class="mb-2 text-xs font-bold uppercase tracking-[0.12em] text-blue-600">Registration request received</p>
    <h1 id="verification-title" class="m-0 text-[clamp(1.75rem,5vw,2.25rem)] leading-[1.2] text-gray-900">Check your inbox</h1>
    <p class="mx-auto mb-0 mt-4 max-w-108 leading-[1.7] text-gray-600">
      If an eligible account exists, an email has been sent with the next steps to complete your registration.
    </p>

    <div class="my-7 flex items-center gap-3 rounded-xl bg-blue-50 p-4 text-left text-sm leading-6 text-gray-600">
      <i class="fas fa-circle-info text-2xl text-blue-600" aria-hidden="true"></i>
      <span>It may take a few minutes to arrive. Be sure to check your spam or junk folder.</span>
    </div>

    <router-link class="block no-underline" to="/skills-login">
      <SkillsButton
        data-cy="loginPage"
        icon="fas fa-arrow-left"
        label="Return to Login Page"
        outlined />
    </router-link>
  </AccessPageCard>
</template>

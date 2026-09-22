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
import { onMounted, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import AccessPageCard from '@/components/access/AccessPageCard.vue';

const router = useRouter();
const route = useRoute();

const email = ref(route.params.email);
const timer = ref(-1);

onMounted(() => {
  timer.value = 10;
});

watch(timer, (value) => {
  if (value > 0) {
    setTimeout(() => {
      timer.value -= 1;
    }, 1000);
  } else {
    router.push({ name: 'Login' });
  }
})
</script>

<template>
  <AccessPageCard
    data-cy="resetRequestConfirmation"
    icon="fas fa-envelope-circle-check"
    labelled-by="reset-request-title">
    <p class="mb-2 text-xs font-bold uppercase tracking-[0.12em] text-blue-600">Password reset requested</p>
    <h1 id="reset-request-title" class="m-0 text-[clamp(1.5rem,4vw,2rem)] leading-[1.2] text-gray-900">
      Check your inbox
    </h1>
    <p class="mx-auto mb-0 mt-4 max-w-108 leading-[1.7] text-gray-600">
      A password reset link has been sent to
      <span class="font-semibold text-gray-800">{{ email }}</span>.
    </p>

    <div class="my-7 flex items-center gap-3 rounded-xl bg-blue-50 p-4 text-left text-sm leading-6 text-gray-600">
      <i class="fas fa-clock text-2xl text-blue-600" aria-hidden="true"></i>
      <span>You will be forwarded to the login page in {{ timer }} seconds.</span>
    </div>

    <div class="text-center">
      <router-link class="inline-block no-underline" :to="{ name: 'Login' }">
        <SkillsButton
          id="loginPageBtn"
          icon="fas fa-sign-in-alt"
          outlined
          size="small"
          data-cy="loginPage"
          label="Return to Login Page" />
      </router-link>
    </div>
  </AccessPageCard>
</template>

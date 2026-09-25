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
import { ref, onMounted, watch } from 'vue';
import { useRouter } from 'vue-router';
import AccessService from "@/components/access/AccessService.js";
import LoadingContainer from "@/components/utils/LoadingContainer.vue";
import AccessPageCard from '@/components/access/AccessPageCard.vue';
import {useEmailVerificationInfo} from "@/components/access/UseEmailVerificationInfo.js";

const emailVerificationInfo = useEmailVerificationInfo()
const router = useRouter();

const props = defineProps({
  countDown: {
    type: Number,
    default: 10,
  },
  token: {
    type: String,
    default: '',
  },
  email: {
    type: String,
    default: '',
  },
});

const timer = ref(-1);
const loading = ref(true);

onMounted(() => {
  verifyEmail();
});

const verifyEmail = () => {
  const verification = { token: props.token, email: props.email };
  AccessService.verifyEmail(verification).then(() => {
    loading.value = false;
    timer.value = props.countDown;
  }).catch((err) => {
    const params = {
      email: props.email,
      explanation: 'GeneralError',
    };
    if (err && err.response && err.response.data && err.response.data.errorCode === 'UserTokenExpired') {
      params.explanation = 'UserTokenExpired';
    }
    emailVerificationInfo.setEmail(params.email)
    emailVerificationInfo.setReason(params.explanation)
    router.push({ name: 'RequestEmailVerification' });
  });
};

watch(() => timer.value, (newValue) => {
  if (newValue > 0) {
    setTimeout(() => {
      timer.value -= 1;
    }, 1000);
  } else {
    router.push({ name: 'Login' });
  }
})
</script>

<template>
  <loading-container :is-loading="loading">
    <AccessPageCard
      data-cy="emailConfirmation"
      icon="fas fa-envelope-circle-check"
      labelled-by="email-confirmation-title">
      <p class="mb-2 text-xs font-bold uppercase tracking-[0.12em] text-blue-600">Email verified</p>
      <h1 id="email-confirmation-title" class="m-0 text-[clamp(1.5rem,4vw,2rem)] leading-[1.2] text-gray-900">
        Email address confirmed
      </h1>
      <p class="mx-auto mb-0 mt-4 max-w-108 leading-[1.7] text-gray-600">
        Your email address has been confirmed. Your SkillTree account is ready to use.
      </p>

      <div class="my-7 flex items-center gap-3 rounded-xl bg-blue-50 p-4 text-left text-sm leading-6 text-gray-600">
        <i class="fas fa-clock text-2xl text-blue-600" aria-hidden="true"></i>
        <span>You will be forwarded to the login page in {{ timer }} seconds.</span>
      </div>

      <div class="text-center">
        <router-link class="inline-block no-underline" to="/skills-login">
          <SkillsButton
            data-cy="loginPage"
            icon="fas fa-sign-in-alt"
            label="Return to Login Page"
            outlined />
        </router-link>
      </div>
    </AccessPageCard>
  </loading-container>
</template>

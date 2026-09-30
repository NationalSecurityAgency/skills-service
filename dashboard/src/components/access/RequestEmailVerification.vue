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
import { computed } from 'vue';
import { useRouter } from 'vue-router';
import AccessService from "@/components/access/AccessService.js";
import AccessPageCard from '@/components/access/AccessPageCard.vue';
import {useEmailVerificationInfo} from "@/components/access/UseEmailVerificationInfo.js";

const emailVerificationInfo = useEmailVerificationInfo()

const router = useRouter();

const explanationReason = computed(() => {
  if( emailVerificationInfo.reason === 'UserTokenExpired' ) {
    return 'Your email verification code has expired. Please click the button below to resend a new verification code.';
  } else if ( emailVerificationInfo.reason === 'GeneralError' ) {
    return 'An error occurred while verifying your email address. Please click the button below to resend a new verification code.';
  }
  return '';
})

const resend = () => {
  AccessService.resendEmailVerification(emailVerificationInfo.email).then(() => {
    router.push({ name: 'EmailVerificationSent' });
  });
};
</script>

<template>
  <AccessPageCard icon="fas fa-envelope-open-text" labelled-by="verification-required-title">
    <div data-cy="confirmEmailTitle">
      <p class="mb-2 text-xs font-bold uppercase tracking-[0.12em] text-blue-600">Account verification</p>
      <h1 id="verification-required-title" class="m-0 text-[clamp(1.5rem,4vw,2rem)] leading-[1.2] text-gray-900">
        Email verification required
      </h1>
    </div>

    <div class="mt-4 text-gray-600" data-cy="confirmEmailExplanation">
      <p v-if="explanationReason" class="mx-auto max-w-108 leading-[1.7]">
        {{ explanationReason }}
      </p>
      <div v-else class="mx-auto max-w-108 leading-[1.7]">
        <p>You must first validate your email address in order to start using SkillTree.</p>
        <p>
          An email verification code has been sent to
          <span class="font-semibold text-gray-800">{{ emailVerificationInfo.email }}</span>.
        </p>
        <p>Please check your email and confirm your email address to complete your SkillTree account creation, or resend a new verification code.</p>
      </div>
    </div>

    <div class="mt-7 text-center">
      <SkillsButton
        variant="outline-primary"
        data-cy="resendConfirmationCodeButton"
        aria-label="Resend Email Confirmation Code"
        icon="fas fa-arrow-circle-right"
        label="Resend Email Confirmation Code"
        @click="resend" />
    </div>
  </AccessPageCard>
</template>

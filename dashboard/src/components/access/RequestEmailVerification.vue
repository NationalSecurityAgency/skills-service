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
import Logo1 from "@/components/brand/Logo1.vue";
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
  <div>
    <div class="flex w-full justify-center">
      <div class="flex flex-col" style="min-width: 20rem;">
        <div class="mt-8 mb-8 flex flex-col items-center" data-cy="confirmEmailTitle">
          <logo1 />
          <div class="h3 mt-6 text-primary">Email Verification is Required!</div>
        </div>
        <Card data-cy="confirmEmailExplanation">
          <template #content>
            <div v-if="explanationReason">
              <p>{{explanationReason}}</p>
            </div>
            <div v-else>
              <p>You must first validate your email address in order to start using SkillTree.</p>
              <p>An email verification code has been sent to {{ emailVerificationInfo.email }}.</p>
              <p>Please check your email and confirm your email address to complete your SkillTree account creation, or you can click the button below to resend a new verification code.</p>
            </div>
            <div class="text-center">
              <SkillsButton variant="outline-primary" @click="resend" data-cy="resendConfirmationCodeButton" aria-label="Resend Email Confirmation Code" icon="fas fa-arrow-circle-right" label="Resend Email Confirmation Code">
              </SkillsButton>
            </div>
          </template>
        </Card>
      </div>
    </div>
  </div>
</template>

<style scoped>

</style>
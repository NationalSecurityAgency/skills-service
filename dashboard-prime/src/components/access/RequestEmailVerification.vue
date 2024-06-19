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
    <div class="flex w-full justify-content-center">
      <div class="flex flex-column" style="min-width: 20rem;">
        <div class="mt-5 mb-5 flex flex-column align-items-center" data-cy="confirmEmailTitle">
          <logo1 />
          <div class="h3 mt-4 text-primary">Email Verification is Required!</div>
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
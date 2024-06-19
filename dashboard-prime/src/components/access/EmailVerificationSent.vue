<script setup>
import Logo1 from "@/components/brand/Logo1.vue";
import {useEmailVerificationInfo} from "@/components/access/UseEmailVerificationInfo.js";

const emailVerificationInfo = useEmailVerificationInfo()

</script>

<template>
  <div>
    <div class="flex justify-content-center text-center" data-cy="emailVerificationSentConfirmation">
      <div class="" style="min-width: 20rem;">
        <div class="mt-5">
          <logo1 />
          <div class="h3 mt-4 text-primary">Email Verification Sent!</div>
        </div>
        <Card>
          <template #content>
            <p>An email verification code has been sent to <span class="text-primary font-weight-bold">{{ emailVerificationInfo.email }}</span>.</p>
            <p>Please check your email and confirm your email address to complete your SkillTree account creation.</p>
            <div class="text-center">
              <router-link to="/skills-login">
                <SkillsButton class="p-2" data-cy="loginPage" icon="fas fa-sign-in-alt" label="Return to Login Page"></SkillsButton>
              </router-link>
            </div>
          </template>
        </Card>

      </div>
    </div>
  </div>
</template>

<style scoped>

</style>
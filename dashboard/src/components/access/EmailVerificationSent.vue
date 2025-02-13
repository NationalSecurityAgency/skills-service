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
import Logo1 from "@/components/brand/Logo1.vue";
import {useEmailVerificationInfo} from "@/components/access/UseEmailVerificationInfo.js";

const emailVerificationInfo = useEmailVerificationInfo()

</script>

<template>
  <div>
    <div class="flex justify-center text-center" data-cy="emailVerificationSentConfirmation">
      <div class="" style="min-width: 20rem;">
        <div class="mt-8">
          <logo1 />
          <div class="h3 mt-6 text-primary">Email Verification Sent!</div>
        </div>
        <Card>
          <template #content>
            <p>An email verification code has been sent to <span class="text-primary font-weight-bold">{{ emailVerificationInfo.email }}</span>.</p>
            <p>Please check your email and confirm your email address to complete your SkillTree account creation.</p>
            <div class="text-center">
              <router-link to="/skills-login" tabindex="-1">
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
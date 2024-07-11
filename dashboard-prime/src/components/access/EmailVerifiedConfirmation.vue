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
import Logo1 from "@/components/brand/Logo1.vue";
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
    <div class="flex justify-content-center text-center" data-cy="emailConfirmation">
      <div class="" style="min-width: 20rem;">
        <div class="mt-5">
          <logo1 />
          <div class="h3 mt-4 text-primary">Email Address Successfully Confirmed!</div>
        </div>
        <Card>
          <template #content>
            <p>Your email address has been confirmed! You will be forwarded to the login page in {{ timer }} seconds.</p>
            <div class="text-center">
              <router-link to="/skills-login" tabindex="-1">
                <SkillsButton class="p-2" data-cy="loginPage" icon="fas fa-sign-in-alt" label="Return to Login Page"></SkillsButton>
              </router-link>
            </div>
          </template>
        </Card>
      </div>
    </div>
  </loading-container>
</template>

<style scoped>

</style>
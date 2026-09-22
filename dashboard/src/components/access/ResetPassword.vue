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
import { ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useForm } from 'vee-validate';
import { useAppConfig } from '@/common-components/stores/UseAppConfig.js';
import * as yup from 'yup';
import { string } from 'yup';
import AccessPageCard from '@/components/access/AccessPageCard.vue';
import AccessService from '@/components/access/AccessService.js';

const route = useRoute()
const router = useRouter()
const appConfig = useAppConfig()

const resetToken = ref(route.params.resetToken);
const resetInProgress = ref(false);
const resetFailed = ref(false);
const resetSuccessful = ref(false);
const remoteError = ref(null);

const changePassword = (email, password) => {
  resetInProgress.value = true;
  const reset = { resetToken: resetToken.value, userId: email, password: password };

  resetFailed.value = false;
  resetSuccessful.value = false;
  remoteError.value = null;

  AccessService.resetPassword(reset).then(() => {
    resetInProgress.value = false;
    router.push({ name: 'ResetConfirmation' });
  }).catch((err) => {
    if (err && err.response && err.response.data && err.response.data.explanation) {
      remoteError.value = err.response.data.explanation;
    } else {
      remoteError.value = `Password reset failed due to ${err.response.status}`;
    }
    resetFailed.value = true;
    resetInProgress.value = false;
  });
};

const schema = yup.object().shape({
  email: string().required().email().min(appConfig.minUsernameLength).label('Email Address'),
  password: string().required().min(appConfig.minPasswordLength).max(appConfig.maxPasswordLength).label('Password'),
  passwordConfirmation: string().required().oneOf([yup.ref('password')], 'Passwords must match').label('Confirm Password'),
})

const { values, meta, handleSubmit, validate, errors } = useForm({
  validationSchema: schema,
  initialValues: {
    email: '',
    password: '',
    passwordConfirmation: '',
  }
})
const onSubmit = handleSubmit((values) => {
  changePassword(values.email, values.password);
});
</script>

<template>
  <AccessPageCard icon="fas fa-key" labelled-by="reset-password-title">
    <p class="mb-2 text-xs font-bold uppercase tracking-[0.12em] text-blue-600">Secure your account</p>
    <h1 id="reset-password-title" class="m-0 text-[clamp(1.5rem,4vw,2rem)] leading-[1.2] text-gray-900">
      Choose a new password
    </h1>
    <p class="mx-auto mb-7 mt-4 max-w-108 leading-[1.7] text-gray-600">
      Confirm your account email and enter the new password you would like to use.
    </p>

    <form class="text-left" @submit="onSubmit">
      <div class="flex flex-col gap-2">
        <SkillsTextInput
          id="email"
          label="Email Address"
          size="small"
          autocomplete="username"
          :is-required="true"
          :disabled="resetInProgress"
          placeholder="Enter email"
          data-cy="resetPasswordEmail"
          name="email" />
        <SkillsTextInput
          id="password"
          label="New Password"
          size="small"
          type="password"
          autocomplete="new-password"
          :is-required="true"
          :disabled="resetInProgress"
          placeholder="Enter new password"
          data-cy="resetPasswordNewPassword"
          name="password" />
        <SkillsTextInput
          id="passwordConfirmation"
          label="Confirm New Password"
          size="small"
          type="password"
          autocomplete="new-password"
          :is-required="true"
          :disabled="resetInProgress"
          placeholder="Confirm new password"
          data-cy="resetPasswordConfirm"
          name="passwordConfirmation" />
      </div>
      <small v-if="remoteError" class="text-red-700" data-cy="resetError" role="alert">{{ remoteError }}</small>
      <div class="mt-4 flex justify-end">
        <SkillsButton
          variant="outline-success"
          type="submit"
          label="Reset Password"
          icon="fas fa-arrow-circle-right"
          :loading="resetInProgress"
          :disabled="!meta.valid || resetInProgress || remoteError"
          data-cy="resetPasswordSubmit" />
      </div>
    </form>
  </AccessPageCard>
</template>

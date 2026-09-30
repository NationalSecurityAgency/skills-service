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
import { computed, onMounted, ref, watch } from 'vue';
import { useRouter } from 'vue-router';
import { useForm } from 'vee-validate';
import { useAppConfig } from '@/common-components/stores/UseAppConfig.js';
import * as yup from 'yup';
import AccessPageCard from '@/components/access/AccessPageCard.vue';
import AccessService from '@/components/access/AccessService.js';
import { string } from 'yup';

const router = useRouter()
const appConfig = useAppConfig()

const username = ref('');
const serverError = ref('');


onMounted(() => {
  AccessService.isResetSupported().then((response) => {
    if (response === false) {
      router.replace({ name: 'ResetNotSupportedPage' });
    }
  });
})

watch(username, (newVal, oldVal) => {
  if (newVal.trim() !== oldVal.trim()) {
    serverError.value = '';
  }
})

const disabled = computed(() => {
  return !meta.value.valid || serverError.value !== '';
})
const schema = yup.object().shape({
  username: string().required().email().min(appConfig.minUsernameLength).label('Email Address'),
})

const { values, meta, handleSubmit, validate, errors } = useForm({
  validationSchema: schema,
  initialValues: {
    username: '',
  }
})

const reset = (username) => {
  AccessService.requestPasswordReset(username).then((response) => {
    serverError.value = '';
    if (response.success) {
      router.push({ name: 'RequestResetConfirmation', params: { email: username } });
    }
  }).catch((err) => {
    if (err && err.response && err.response.data && err.response.data.explanation) {
      serverError.value = err.response.data.explanation;
    } else {
      serverError.value = `Password reset request failed due to ${err.response.status}`;
    }
  });
}

const resetPassword = handleSubmit((values) => {
  reset(values.username)
});

</script>

<template>
  <AccessPageCard icon="fas fa-key" labelled-by="reset-password-title">
    <p class="mb-2 text-xs font-bold uppercase tracking-[0.12em] text-blue-600">Account recovery</p>
    <h1 id="reset-password-title" class="m-0 text-[clamp(1.75rem,5vw,2.25rem)] leading-[1.2] text-gray-900">Reset your password</h1>
    <p class="mx-auto mb-7 mt-4 max-w-108 leading-[1.7] text-gray-600">
      Enter your account email address and we'll send you instructions to choose a new password.
    </p>

    <form class="text-left" @submit.prevent="resetPassword">
      <div class="w-full">
        <SkillsTextInput
          id="username"
          v-model="username"
          label="Email Address"
          size="small"
          :is-required="true"
          placeholder="Enter email"
          data-cy="forgotPasswordEmail"
          name="username" />
      </div>
      <small v-if="serverError" class="text-red-700" data-cy="resetFailedError" role="alert">{{ serverError }}</small>
      <div class="mt-4 flex justify-end">
        <SkillsButton
          type="submit"
          variant="outline-success"
          label="Reset Password"
          icon="fas fa-arrow-circle-right"
          :disabled="disabled"
          data-cy="resetPassword" />
      </div>
    </form>
  </AccessPageCard>
</template>

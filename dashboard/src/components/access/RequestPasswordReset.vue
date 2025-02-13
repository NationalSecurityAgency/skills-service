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
import Logo1 from '@/components/brand/Logo1.vue';
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
<div>
  <div class="grid grid-cols-12 gap-4 justify-center text-center">
    <div class="col md:col-span-8 lg:col-span-7 xl:col-span-4 mt-4" style="min-width: 20rem;">
      <div class="mt-8">
        <logo1 />
        <div class="text-3xl mt-6 text-primary">Reset Password For SkillTree Dashboard</div>
      </div>
      <Card class="mt-4 text-left">
        <template #content>
          <div class="w-full">
            <SkillsTextInput
                label="Email Address"
                size="small"
                :is-required="true"
                @keyup.enter="resetPassword"
                placeholder="Enter email"
                v-model="username"
                data-cy="forgotPasswordEmail"
                id="username"
                name="username"/>
          </div>
          <small class="text-danger text-red-500" v-if="serverError" data-cy="resetFailedError" role="alert">{{ serverError }}</small>
          <div class="flex justify-end mt-2">
            <SkillsButton variant="outline-success"
                          label="Reset Password"
                          icon="fas fa-arrow-circle-right"
                          @click="resetPassword"
                          :disabled="disabled"
                          data-cy="resetPassword">
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
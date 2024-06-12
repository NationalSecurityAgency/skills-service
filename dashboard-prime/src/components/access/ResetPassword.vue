<script setup>
import { computed, onMounted, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useForm } from 'vee-validate';
import { useAppConfig } from '@/common-components/stores/UseAppConfig.js';
import * as yup from 'yup';
import Logo1 from '@/components/brand/Logo1.vue';
import AccessService from '@/components/access/AccessService.js';
import { string } from 'yup';

const route = useRoute()
const router = useRouter()
const appConfig = useAppConfig()

const resetToken = ref(route.params.resetToken);
const resetFields = ref({
  email: '',
      password: '',
});
const resetInProgress = ref(false);
const resetFailed = ref(false);
const resetSuccessful = ref(false);
const remoteError = ref(null);
const passwordConfirmation = ref('');

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
const handleChangePassword = handleSubmit((values) => {
  changePassword(values.email, values.password);
});
</script>

<template>
  <div>
    <div class="grid justify-content-center text-center">
      <div class="col md:col-8 lg:col-7 xl:col-4 mt-3" style="min-width: 20rem;">
        <div class="mt-5">
          <logo1 />
          <div class="text-3xl mt-4 text-primary">Reset Password For SkillTree Dashboard</div>
        </div>
        <Card class="mt-3 text-left">
          <template #content>
            <form @submit="handleChangePassword">
              <div class="w-full">
                <SkillsTextInput
                    label="Email Address"
                    size="small"
                    autocomplete="username"
                    :is-required="true"
                    :disabled="resetInProgress"
                    @keyup.enter="handleChangePassword"
                    placeholder="Enter email"
                    v-model="resetFields.email"
                    data-cy="resetPasswordEmail"
                    id="email"
                    name="email"/>
                <SkillsTextInput
                    label="New Password"
                    size="small"
                    type="password"
                    autocomplete="current-password"
                    :is-required="true"
                    :disabled="resetInProgress"
                    @keyup.enter="handleChangePassword"
                    placeholder="Enter new password"
                    v-model="resetFields.password"
                    data-cy="resetPasswordNewPassword"
                    id="password"
                    name="password"/>
                <SkillsTextInput
                    label="Confirm New Password"
                    size="small"
                    type="password"
                    autocomplete="new-password"
                    :is-required="true"
                    :disabled="resetInProgress"
                    @keyup.enter="handleChangePassword"
                    placeholder="Confirm new password"
                    v-model="resetFields.passwordConfirmation"
                    data-cy="resetPasswordConfirm"
                    id="passwordConfirmation"
                    name="passwordConfirmation"/>
              </div>
              <small class="text-danger text-red-500" v-if="remoteError" data-cy="resetError" role="alert">{{ remoteError }}</small>
              <div class="flex justify-content-end mt-2">
                <SkillsButton variant="outline-success"
                              type="submit"
                              label="Reset Password"
                              icon="fas fa-arrow-circle-right"
                              :disabled="!meta.valid || resetInProgress || remoteError"
                              data-cy="resetPasswordSubmit">
                </SkillsButton>
              </div>
            </form>
          </template>
        </Card>
      </div>
    </div>
  </div>
</template>

<style scoped>

</style>
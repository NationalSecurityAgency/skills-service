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
import { computed, onBeforeMount, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useForm } from 'vee-validate';
import { useAppConfig } from '@/common-components/stores/UseAppConfig.js';
import { useAuthState } from '@/stores/UseAuthState.js';
import * as yup from 'yup';
import { string } from 'yup';
import AccessPageCard from '@/components/access/AccessPageCard.vue';
import AccessService from '@/components/access/AccessService.js';
import {useEmailVerificationInfo} from "@/components/access/UseEmailVerificationInfo.js";

const authState = useAuthState()
const route = useRoute()
const router = useRouter()
const appConfig = useAppConfig()
const emailVerificationInfo = useEmailVerificationInfo()

const isRootAccount = route.meta.isRootAccount;
const createInProgress = ref(false);
const createError = ref('');
const oAuthProviders = ref([]);

const oAuthOnly = computed(() => {
  return appConfig.oAuthOnly;
})
const isProgressAndRankingEnabled = computed(() => {
  return appConfig.rankingAndProgressViewsEnabled === true || appConfig.rankingAndProgressViewsEnabled === 'true';
})
const verifyEmailAddresses = computed(() => {
  return appConfig.verifyEmailAddresses === true || appConfig.verifyEmailAddresses === 'true';
})

onBeforeMount(() => {
  if (!appConfig.isPkiAuthenticated) {
    AccessService.getOAuthProviders()
        .then((result) => {
          oAuthProviders.value = result;
        });
  }
})

const createAccount = (firstName, lastName, email, password) => {
  createInProgress.value = true;
  createError.value = '';
  authState.signup({isRootAccount, firstName, lastName, email, password}).then(() => {
    if (isRootAccount) {
        if (route.query.redirect) {
          router.push(route.query.redirect);
        } else if (!isProgressAndRankingEnabled.value) {
          router.push({name: 'AdminHomePage'});
        } else {
          const defaultHomePage = appConfig.defaultLandingPage;
          const pageName = defaultHomePage === 'progress' ? 'MyProgressPage' : 'AdminHomePage';
          router.push({name: pageName});
        }
    } else if (verifyEmailAddresses.value) {
      router.push({name: 'EmailVerificationSent'});
    } else {
      router.push({
        name: 'Login',
        ...(route.query?.redirect && { query: { redirect: route.query.redirect } }),
      });
    }
  }).catch((error) => {
    console.error(error)
    createError.value = 'Unable to create your account. Please try again.';
  }).finally(() => {
    createInProgress.value = false;
  });
}
const oAuth2Login = (registrationId) => {
  createInProgress.value = true;
  authState.oAuth2Login(registrationId);
}
const schema = yup.object().shape({
  firstName: string().required().max(appConfig.maxFirstNameLength).label('First Name'),
  lastName: string().required().max(appConfig.maxFirstNameLength).label('Last Name'),
  email: string().required().email().min(appConfig.minUsernameLength).label('Email'),
  password: string().required().min(appConfig.minPasswordLength).max(appConfig.maxPasswordLength).label('Password'),
  passwordConfirmation: string().required().oneOf([yup.ref('password')], 'Passwords must match').label('Confirm Password'),
})

const { values, meta, handleSubmit, validate, errors } = useForm({
  validationSchema: schema,
  initialValues: {
    firstName: '',
    lastName: '',
    email: '',
    password: '',
    passwordConfirmation: '',
  }
})
const onSubmit = handleSubmit((values) => {
  createAccount(values.firstName, values.lastName, values.email, values.password);
});
</script>

<template>
  <AccessPageCard icon="fas fa-user-plus" labelled-by="create-account-title">
    <p class="mb-2 text-xs font-bold uppercase tracking-[0.12em] text-blue-600">Join SkillTree</p>
    <h1 id="create-account-title" class="m-0 text-[clamp(1.5rem,4vw,2rem)] leading-[1.2] text-gray-900 uppercase">
      New <span v-if="isRootAccount">Root </span>Account
    </h1>
    <p class="mx-auto mb-7 mt-4 max-w-108 leading-[1.7] text-gray-600">
      Enter your details to create your SkillTree account.
    </p>

    <form v-if="!oAuthOnly" class="text-left" @submit="onSubmit">
      <div class="flex w-full flex-col gap-2">
        <SkillsTextInput
          id="firstName"
          label="First Name"
          size="small"
          autocomplete="given-name"
          :is-required="true"
          :disabled="createInProgress"
          data-cy="requestAccountFirstName"
          name="firstName">
          <template #addOnBefore>
            <i class="fas fa-user" aria-hidden="true"></i>
          </template>
        </SkillsTextInput>
        <SkillsTextInput
          id="lastName"
          label="Last Name"
          size="small"
          autocomplete="family-name"
          :is-required="true"
          :disabled="createInProgress"
          data-cy="requestAccountLastName"
          name="lastName">
          <template #addOnBefore>
            <i class="fas fa-user-tie" aria-hidden="true"></i>
          </template>
        </SkillsTextInput>
        <SkillsTextInput
          id="email"
          label="Email"
          size="small"
          autocomplete="username"
          :is-required="true"
          :disabled="createInProgress"
          data-cy="requestAccountEmail"
          name="email">
          <template #addOnBefore>
            <i class="fas fa-envelope" aria-hidden="true"></i>
          </template>
        </SkillsTextInput>
        <SkillsTextInput
          id="password"
          label="New Password"
          size="small"
          type="password"
          autocomplete="new-password"
          :is-required="true"
          :disabled="createInProgress"
          data-cy="requestAccountPassword"
          name="password">
          <template #addOnBefore>
            <i class="fas fa-key" aria-hidden="true"></i>
          </template>
        </SkillsTextInput>
        <SkillsTextInput
          id="passwordConfirmation"
          label="Confirm New Password"
          size="small"
          type="password"
          autocomplete="new-password"
          :is-required="true"
          :disabled="createInProgress"
          data-cy="requestAccountConfirmPassword"
          name="passwordConfirmation">
          <template #addOnBefore>
            <i class="fas fa-key" aria-hidden="true"></i>
          </template>
        </SkillsTextInput>
      </div>
      <div class="my-4 flex justify-center">
        <SkillsButton
          variant="outline-success"
          type="submit"
          label="Create Account"
          icon="fas fa-arrow-circle-right"
          :loading="createInProgress"
          :disabled="!meta.valid || createInProgress"
          data-cy="createAccountButton" />
      </div>
      <Message
        v-if="createError"
        severity="error"
        :closable="false"
        data-cy="createAccountError">{{ createError }}</Message>
      <div v-if="createInProgress && isRootAccount" class="mt-2 text-center text-gray-700">
        Bootstrapping! May take a second...
      </div>
      <div v-if="!isRootAccount">
        <Divider />
        <p class="mb-0 text-center text-sm text-gray-700">
          Already have an account?
          <router-link class="font-semibold text-blue-700 underline" :to="{ name: 'Login' }">Sign in</router-link>
        </p>
      </div>
    </form>

    <div v-if="oAuthProviders && oAuthProviders.length > 0" class="flex flex-col gap-4" data-cy="oAuthProviders">
      <Button
        v-for="oAuthProvider in oAuthProviders"
        :key="oAuthProvider.registrationId"
        class="w-full text-center"
        outlined
        :icon="oAuthProvider.iconClass"
        :label="`Login via ${ oAuthProvider.clientName }`"
        @click="oAuth2Login(oAuthProvider.registrationId)" />
    </div>
  </AccessPageCard>
</template>

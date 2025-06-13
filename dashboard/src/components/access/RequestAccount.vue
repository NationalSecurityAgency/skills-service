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
import { useDebounceFn } from '@vueuse/core';
import { useAuthState } from '@/stores/UseAuthState.js';
import * as yup from 'yup';
import { string } from 'yup';
import Logo1 from '@/components/brand/Logo1.vue';
import AccessService from '@/components/access/AccessService.js';
import {useEmailVerificationInfo} from "@/components/access/UseEmailVerificationInfo.js";

const authState = useAuthState()
const route = useRoute()
const router = useRouter()
const appConfig = useAppConfig()
const emailVerificationInfo = useEmailVerificationInfo()

const isRootAccount = route.meta.isRootAccount;
const createInProgress = ref(false);
const oAuthProviders = ref([]);

const oAuthOnly = computed(() => {
  return appConfig.oAuthOnly;
})
const verifyEmailAddresses = computed(() => {
  return appConfig.verifyEmailAddresses;
})
const isProgressAndRankingEnabled = computed(() => {
  return appConfig.rankingAndProgressViewsEnabled === true || appConfig.rankingAndProgressViewsEnabled === 'true';
})

onBeforeMount(() => {
  if (!appConfig.isPkiAuthenticated) {
    AccessService.getOAuthProviders()
        .then((result) => {
          oAuthProviders.value = result;
        });
  }
})

const login = (firstName, lastName, email, password) => {
  createInProgress.value = true;
  authState.signup({isRootAccount, firstName, lastName, email, password}).then(() => {
    authState.configureSkillsClientForInception()
        .then(() => {
          if (verifyEmailAddresses.value) {
            emailVerificationInfo.setEmail(email)
            router.push({name: 'EmailVerificationSent'});
          } else if (route.query.redirect) {
            router.push(route.query.redirect);
          } else if (!isProgressAndRankingEnabled.value) {
            router.push({name: 'AdminHomePage'});
          } else {
            const defaultHomePage = appConfig.defaultLandingPage;
            const pageName = defaultHomePage === 'progress' ? 'MyProgressPage' : 'AdminHomePage';
            router.push({name: pageName});
          }
        });
  });
}
const oAuth2Login = (registrationId) => {
  createInProgress.value = true;
  authState.oAuth2Login(registrationId);
}
const uniqueEmail = useDebounceFn(async (value, context) => {
  if (!value) {
    return true;
  }
  try {
    await yup.string().email().validate(value);
    const isUnique = await AccessService.userWithEmailExists(value);
    if (isUnique) {
      return true
    }
    return context.createError({message: 'This email address is already used for another account'});
  } catch ({message}) {
    return context.createError({message});
  }
}, appConfig.formFieldDebounceInMs)

const schema = yup.object().shape({
  firstName: string().required().max(appConfig.maxFirstNameLength).label('First Name'),
  lastName: string().required().max(appConfig.maxFirstNameLength).label('Last Name'),
  email: string().required().email().min(appConfig.minUsernameLength).test((value, context) => uniqueEmail(value, context)).label('Email'),
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
  login(values.firstName, values.lastName, values.email, values.password);
});
</script>

<template>
  <div>
    <div class="pt-10">
      <div class="max-w-md lg:max-w-xl mx-auto" style="min-width: 20rem;">
        <h1 class="sr-only">SkillTree New Account</h1>
        <div class="text-center">
          <logo1 class="mb-4" />
          <Message :closable="false">New <span v-if="isRootAccount">Root </span>Account</Message>
        </div>
        <Card v-if="!oAuthOnly" class="mt-4 text-left">
          <template #content>
            <form @submit="onSubmit">
              <div class="w-full flex flex-col gap-2">
                <SkillsTextInput
                    label="First Name"
                    size="small"
                    autocomplete="given-name"
                    :is-required="true"
                    :disabled="createInProgress"
                    @keyup.enter="onSubmit"
                    data-cy="requestAccountFirstName"
                    id="firstName"
                    name="firstName">
                  <template #addOnBefore>
                    <i class="fas fa-user" aria-hidden="true"></i>
                  </template>
                </SkillsTextInput>
                  <SkillsTextInput
                      label="Last Name"
                      size="small"
                      autocomplete="family-name"
                      :is-required="true"
                      :disabled="createInProgress"
                      @keyup.enter="onSubmit"
                      data-cy="requestAccountLastName"
                      id="lastName"
                      name="lastName">
                    <template #addOnBefore>
                      <i class="fas fa-user-tie" aria-hidden="true"></i>
                    </template>
                  </SkillsTextInput>
                  <SkillsTextInput
                      label="Email"
                      size="small"
                      autocomplete="username"
                      :is-required="true"
                      :disabled="createInProgress"
                      @keyup.enter="onSubmit"
                      data-cy="requestAccountEmail"
                      id="email"
                      name="email">
                    <template #addOnBefore>
                      <i class="fas fa-envelope" aria-hidden="true"></i>
                    </template>
                  </SkillsTextInput>
                  <SkillsTextInput
                      label="New Password"
                      size="small"
                      type="password"
                      autocomplete="new-password"
                      :is-required="true"
                      :disabled="createInProgress"
                      @keyup.enter="onSubmit"
                      data-cy="requestAccountPassword"
                      id="password"
                      name="password">
                    <template #addOnBefore>
                      <i class="fas fa-key" aria-hidden="true"></i>
                    </template>
                  </SkillsTextInput>
                  <SkillsTextInput
                      label="Confirm New Password"
                      size="small"
                      type="password"
                      autocomplete="new-password"
                      :is-required="true"
                      :disabled="createInProgress"
                      @keyup.enter="onSubmit"
                      data-cy="requestAccountConfirmPassword"
                      id="passwordConfirmation"
                      name="passwordConfirmation">
                    <template #addOnBefore>
                      <i class="fas fa-key" aria-hidden="true"></i>
                    </template>
                  </SkillsTextInput>
              </div>
              <div class="flex justify-center my-2">
                <SkillsButton variant="outline-success"
                              type="submit"
                              label="Create Account"
                              icon="fas fa-arrow-circle-right"
                              :loading="createInProgress"
                              :disabled="!meta.valid || createInProgress"
                              data-cy="createAccountButton">
                </SkillsButton>
              </div>
              <div v-if="createInProgress && isRootAccount" class="mt-2 text-center">
                Bootstrapping! May take a second...
              </div>
              <div v-if="!isRootAccount" class="p-1">
                <hr/>
                <p class="text-center mt-2"><small>Already have an account?
                  <strong class="underline"><router-link :to="{ name: 'Login' }">Sign in</router-link></strong></small>
                </p>
              </div>
            </form>
          </template>
        </Card>

        <Card v-if="oAuthProviders && oAuthProviders.length > 0"
              class="mt-4"
              data-cy="oAuthProviders">
          <template #content>
            <div v-for="oAuthProvider in oAuthProviders"
                 :key="oAuthProvider.registrationId"
                 class="col-span-12 mb-4">
              <Button
                  class="w-full text-center"
                  outlined
                  :icon="oAuthProvider.iconClass"
                  :label="`Login via ${ oAuthProvider.clientName }`"
                  @click="oAuth2Login(oAuthProvider.registrationId)" />
            </div>
          </template>
        </Card>

      </div>
    </div>
  </div>
</template>

<style scoped>

</style>
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
import InputGroupAddon from 'primevue/inputgroupaddon';

const authState = useAuthState()
const route = useRoute()
const router = useRouter()
const appConfig = useAppConfig()

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
            router.push({name: 'EmailVerificationSent', params: {email}});
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
    <div class="grid justify-content-center text-center">
      <div class="col md:col-8 lg:col-7 xl:col-4 mt-3" style="min-width: 20rem;">
        <div class="mt-5">
          <logo1 />
          <div class="text-3xl mt-4 text-primary">
            New <span v-if="isRootAccount">Root </span>Account
          </div>
        </div>
        <Card v-if="!oAuthOnly" class="mt-3 text-left">
          <template #content>
            <form @submit="onSubmit">
              <div class="w-full">
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
                    <InputGroupAddon class="p-0 m-0">
                      <i class="fas fa-user" aria-hidden="true"></i>
                    </InputGroupAddon>
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
                      <InputGroupAddon class="p-0 m-0">
                        <i class="fas fa-user-tie" aria-hidden="true"></i>
                      </InputGroupAddon>
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
                      <InputGroupAddon class="p-0 m-0">
                        <i class="fas fa-envelope" aria-hidden="true"></i>
                      </InputGroupAddon>
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
                      <InputGroupAddon class="p-0 m-0">
                        <i class="fas fa-key" aria-hidden="true"></i>
                      </InputGroupAddon>
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
                      <InputGroupAddon class="p-0 m-0">
                        <i class="fas fa-key" aria-hidden="true"></i>
                      </InputGroupAddon>
                    </template>
                  </SkillsTextInput>
              </div>
              <div class="flex justify-content-end mt-2">
                <SkillsButton variant="outline-success"
                              type="submit"
                              label="Create Account"
                              icon="fas fa-arrow-circle-right"
                              :loading="createInProgress"
                              :disabled="!meta.valid || createInProgress"
                              data-cy="createAccountButton">
                </SkillsButton>
              </div>
              <div v-if="createInProgress && isRootAccount" class="mt-2 text-primary">
                Bootstrapping! May take a second...
              </div>
              <div v-if="!isRootAccount" class="p-1">
                <hr/>
                <p class="text-center"><small>Already have an account?
                  <strong><router-link :to="{ name: 'Login' }">Sign in</router-link></strong></small>
                </p>
              </div>
            </form>
          </template>
        </Card>

        <Card v-if="oAuthProviders && oAuthProviders.length > 0"
              class="mt-3"
              data-cy="oAuthProviders">
          <template #content>
            <div v-for="oAuthProvider in oAuthProviders"
                 :key="oAuthProvider.registrationId"
                 class="col-12 mb-3">
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
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
import { onBeforeMount, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useForm } from 'vee-validate'
import { useAppConfig } from '@/common-components/stores/UseAppConfig.js'
import { useAuthState } from '@/stores/UseAuthState.js'
import { object, string } from 'yup'
import AccessPageCard from '@/components/access/AccessPageCard.vue'
import AccessService from '@/components/access/AccessService.js'
import InputGroup from 'primevue/inputgroup'
import InputGroupAddon from 'primevue/inputgroupaddon'

const appConfig = useAppConfig()

const schema = object({
  username: string().required().email().min(appConfig.minUsernameLength).label('Email Address'),
  password: string().required().min(appConfig.minPasswordLength).max(appConfig.maxPasswordLength).label('Password')
})

const { defineField, errors, meta, handleSubmit } = useForm({
  validationSchema: schema
})

const [username, usernameAttrs] = defineField('username')
const [password, passwordAttrs] = defineField('password')

const authState = useAuthState()
const router = useRouter()
const route = useRoute()
const loginFailed = ref(false)
const authenticating = ref(false)
const performFormLogin = (values) => {
  authenticating.value = true
  loginFailed.value = false
  const formData = new FormData()
  formData.append('username', values.username)
  formData.append('password', values.password)
  authState.login(formData)
    .then(() => {
      loginFailed.value = false
      appConfig.loadConfigState()
      const pathToPush = route.query.redirect || '/'
      router.push(pathToPush)
    })
    .catch((error) => {
      if (error.response.status === 401) {
        loginFailed.value = true
      } else {
        const errorMessage =
          error.response && error.response.data && error.response.data.message
            ? error.response.data.message
            : undefined
        router.push({ name: 'ErrorPage', query: { errorMessage } })
      }
    })
    .finally(() => {
      authenticating.value = false
    })
}
const onSubmit = handleSubmit((values) => {
  performFormLogin(values)
})

const oAuthProviders = ref([])
onBeforeMount(() => {
  if (!appConfig.isPkiAuthenticated) {
    AccessService.getOAuthProviders()
      .then((result) => {
        oAuthProviders.value = result;
      });
  }
})
const oAuth2Login = (registrationId) => {
  authState.oAuth2Login(registrationId)
}
const saml2Login = (registrationId) => {
  authState.saml2Login(registrationId)
}

</script>

<template>
  <AccessPageCard icon="fas fa-sign-in-alt" labelled-by="login-title">
    <p class="mb-2 text-xs font-bold uppercase tracking-[0.12em] text-blue-600">SkillTree dashboard</p>
    <h1 id="login-title" class="m-0 text-[clamp(1.5rem,4vw,2rem)] leading-[1.2] text-gray-900 uppercase">Sign in</h1>
    <p class="mx-auto mb-7 mt-4 max-w-108 leading-[1.7] text-gray-600">Sign in to continue building and managing skills.</p>

    <form v-if="!appConfig.oAuthOnly && !appConfig.saml2RegistrationId" @submit="onSubmit">
      <Message v-if="loginFailed" data-cy="loginFailed" severity="error">Invalid Username or Password</Message>
      <div class="flex flex-col gap-2 text-left">
        <label for="username">Email Address</label>
        <InputGroup>
          <InputGroupAddon>
            <i class="far fa-envelope-open" aria-hidden="true"></i>
          </InputGroupAddon>
          <InputText
            id="username"
            v-model="username"
            v-bind="usernameAttrs"
            size="small"
            placeholder="Enter email"
            type="text"
            :class="{ 'p-invalid': errors.username }"
            autocomplete="username"
            :aria-invalid="!!errors.username"
            aria-describedby="username-error"
            aria-errormessage="username-error" />
        </InputGroup>
        <Message
          id="username-error"
          severity="error"
          variant="simple"
          size="small"
          :closable="false"
          data-cy="usernameError">{{ errors.username || '&nbsp;' }}
        </Message>
      </div>

      <div class="flex flex-col gap-1 text-left">
        <div class="mb-2 flex">
          <label for="inputPassword" class="flex">Password</label>
          <div class="flex-1 text-right">
            <small>
              <router-link class="text-blue-700 underline" data-cy="forgotPassword" :to="{ name:'ForgotPassword', query: route.query }">Forgot Password?</router-link>
            </small>
          </div>
        </div>
        <InputGroup>
          <InputGroupAddon>
            <i class="fas fa-key" aria-hidden="true"></i>
          </InputGroupAddon>
          <InputText
            id="inputPassword"
            v-model="password"
            v-bind="passwordAttrs"
            size="small"
            placeholder="Enter password"
            type="password"
            :class="{ 'p-invalid': errors.password }"
            autocomplete="current-password"
            :aria-invalid="!!errors.password"
            aria-describedby="password-error"
            aria-errormessage="password-error" />
        </InputGroup>
        <Message
          id="password-error"
          severity="error"
          variant="simple"
          size="small"
          :closable="false"
          data-cy="passwordError">{{ errors.password || '&nbsp;' }}
        </Message>
      </div>

      <div class="mt-1">
        <SkillsButton
          type="submit"
          label="Login"
          icon="far fa-arrow-alt-circle-right"
          data-cy="login"
          :disabled="!meta.valid"
          :loading="authenticating"
          outlined />
      </div>

      <Divider />
      <p class="text-center">
        <small>
          Don't have a SkillTree account? <router-link class="text-blue-700 underline" data-cy="signUpButton" :to="{ name: 'RequestAccount', query: route.query }">Sign up</router-link>
        </small>
      </p>
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

    <Button
      v-if="appConfig.saml2RegistrationId"
      class="w-full text-center"
      outlined
      icon="far fa-arrow-alt-circle-right"
      label="Login with SAML2"
      @click="saml2Login(appConfig.saml2RegistrationId)" />
  </AccessPageCard>
</template>

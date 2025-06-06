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
import Logo1 from '@/components/brand/Logo1.vue'
import AccessService from '@/components/access/AccessService.js'
import InputGroup from 'primevue/inputgroup'
import InputGroupAddon from 'primevue/inputgroupaddon'
import { useEmailVerificationInfo } from '@/components/access/UseEmailVerificationInfo.js'

const appConfig = useAppConfig()
const emailVerificationInfo = useEmailVerificationInfo()

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
  if (appConfig.verifyEmailAddresses) {
    AccessService.userEmailIsVerified(values.username).then((result) => {
      if (!result) {
        emailVerificationInfo.setEmail(values.username)
        emailVerificationInfo.setReason('NotVerified')
        router.push({ name: 'RequestEmailVerification' })
      } else {
        performFormLogin(values)
      }
    })
  } else {
    performFormLogin(values)
  }
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
  <div class="pt-20">
    <div class="text-center">
      <h1 class="sr-only">SkillTree Login Page</h1>
      <div class="mt-8 justify-center">
        <logo1 />
      </div>
      <div class="">
        <div class="max-w-md lg:max-w-xl mx-auto">
          <Card v-if="!appConfig.oAuthOnly && !appConfig.saml2RegistrationId" class="mt-4">
            <template #content>
              <form @submit="onSubmit">
                <Message v-if="loginFailed" data-cy="loginFailed" severity="error">Invalid Username or Password</Message>
                <div class="flex flex-col gap-2 text-left">
                  <label for="username" class="">Email Address</label>
                  <InputGroup>
                    <InputGroupAddon>
                      <i class="far fa-envelope-open" aria-hidden="true"></i>
                    </InputGroupAddon>
                    <InputText
                      id="username"
                      size="small"
                      placeholder="Enter email"
                      type="text"
                      v-model="username"
                      v-bind="usernameAttrs"
                      :class="{ 'p-invalid': errors.username }"
                      autocomplete="username"
                      :aria-invalid="!!errors.username"
                      aria-describedby="username-error"
                      aria-errormessage="username-error" />
                  </InputGroup>
                  <Message
                      severity="error"
                      variant="simple"
                      size="small"
                      :closable="false"
                      data-cy="usernameError"
                      id="username-error">{{ errors.username || '&nbsp;' }}
                  </Message>
                </div>

                <div class="flex flex-col gap-1 text-left">
                  <div class="flex mb-2">
                    <label for="inputPassword" class="flex">Password</label>
                    <div class="flex-1 text-right">
                      <small class="text-muted">
                        <router-link data-cy="forgotPassword" :to="{ name:'ForgotPassword', query: route.query }">Forgot Password?</router-link>
                      </small>
                    </div>
                  </div>
                  <InputGroup>
                    <InputGroupAddon>
                      <i class="fas fa-key" aria-hidden="true"></i>
                    </InputGroupAddon>
                    <InputText
                      id="inputPassword"
                      size="small"
                      placeholder="Enter password"
                      type="password"
                      v-model="password"
                      v-bind="passwordAttrs"
                      :class="{ 'p-invalid': errors.password }"
                      autocomplete="current-password"
                      :aria-invalid="!!errors.password"
                      aria-describedby="password-error"
                      aria-errormessage="password-error" />
                  </InputGroup>
                  <Message
                      severity="error"
                      variant="simple"
                      size="small"
                      :closable="false"
                      data-cy="passwordError"
                      id="password-error">{{ errors.password || '&nbsp;' }}
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
              </form>

              <Divider />
              <p class="text-center">
                <small>
                  Don't have a SkillTree account? <router-link data-cy="signUpButton" :to="{ name: 'RequestAccount', query: route.query }" class="underline">Sign up</router-link>
                </small>
              </p>
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

          <Card v-if="appConfig.saml2RegistrationId" class="mt-4">
            <template #content>
              <div
                   :key="appConfig.saml2RegistrationId"
                   class="col-span-12 mb-4">
                <Button
                    class="w-full text-center"
                    outlined
                    icon="far fa-arrow-alt-circle-right"
                    label="`Login with SAML2`"
                    @click="saml2Login(appConfig.saml2RegistrationId)" />
              </div>
            </template>
          </Card>
        </div>
      </div>

    </div>
  </div>
</template>

<style scoped></style>

<script setup>
import { ref, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useStore } from 'vuex'
import { useForm } from 'vee-validate'
import * as yup from 'yup'
import Logo1 from '@/components/brand/Logo1.vue'
import AccessService from '@/components/access/AccessService.js'

const store = useStore()
const oAuthOnly = computed(() => {
  return store.state.projectId
})
const verifyEmailAddresses = computed(() => {
  return store.getters.config && store.getters.config.verifyEmailAddresses
})

const schema = yup.object({
  username: yup.string().required().email().min(5),
  password: yup.string().required().min(8).max(30)
})

const { values, defineField, errors, meta, handleSubmit } = useForm({
  validationSchema: schema
})

const [username, usernameAttrs] = defineField('username')
const [password, passwordAttrs] = defineField('password')

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
  store
    .dispatch('login', formData)
    .then(() => {
      loginFailed.value = false
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
      store.dispatch('loadConfigState')
      authenticating.value = false
    })
}
const onSubmit = handleSubmit((values) => {
  if (verifyEmailAddresses) {
    AccessService.userEmailIsVerified(values.username).then((result) => {
      if (!result) {
        router.push({ name: 'RequestEmailVerification', params: { email: values.username } })
      } else {
        performFormLogin(values)
      }
    })
  } else {
    performFormLogin(values)
  }
})
</script>

<template>
  <div class="container-fluid">
    <div class="row justify-content-center text-center">
      <div class="col col-md-8 col-lg-7 col-xl-4 mt-3" style="min-width: 20rem">
        <div class="mt-5">
          <logo1 />
        </div>
        <div v-if="!oAuthOnly" class="card text-start">
          <div class="card-body p-4 text-left">
            <form @submit="onSubmit">
              <Message v-if="loginFailed" severity="error">Invalid Username or Password</Message>
              <div class="">
                <label for="value" class="form-label">Email Address</label>
                <span class="p-input-icon-left w-100">
                  <i class="pi pi-envelope" />
                  <InputText
                    id="username"
                    size="small"
                    placeholder="Enter email"
                    class="form-control"
                    type="text"
                    v-model="username"
                    v-bind="usernameAttrs"
                    :class="{ 'p-invalid': errors.username }"
                    :aria-invalid="errors.username ? null : true"
                    aria-describedby="username-error"
                    aria-errormessage="username-error" />
                </span>
                <small class="p-error" id="username-error">{{ errors.username || '&nbsp;' }}</small>
              </div>

              <div class="form-group">
                <div class="row">
                  <div class="col">
                    <label for="inputPassword" class="form-label">Password</label>
                  </div>
                  <div class="col text-end">
                    <small class="text-muted">
                      <router-link data-cy="forgotPassword" to="/">Forgot Password?</router-link>
                      <!--                      <b-link tabindex="0" @click="forgotPassword" data-cy="forgotPassword"-->
                      <!--                        >Forgot Password?</b-link-->
                      <!--                      >-->
                    </small>
                  </div>
                </div>
                <span class="p-input-icon-left w-100">
                  <i class="pi pi-key" />
                  <InputText
                    id="password"
                    size="small"
                    placeholder="Enter password"
                    class="form-control"
                    type="password"
                    v-model="password"
                    v-bind="passwordAttrs"
                    :class="{ 'p-invalid': errors.password }"
                    :aria-invalid="errors.password ? null : true"
                    aria-describedby="password-error"
                    aria-errormessage="password-error" />
                </span>
                <small class="p-error" id="password-error">{{ errors.password || '&nbsp;' }}</small>
              </div>

              <div class="row">
                <div class="col text-end">
                  <Button
                    type="submit"
                    label="Login"
                    icon="pi pi-arrow-circle-right"
                    data-cy="login"
                    :disabled="!meta.valid"
                    :loading="authenticating"
                    outlined />
                </div>
              </div>
            </form>

            <Divider />
            <p class="text-center">
              <small
                >Don't have a SkillTree account?
                <router-link data-cy="signUpButton" to="/">Sign up</router-link>
                <!--              <strong><b-link data-cy="signUpButton" @click="requestAccountPage">Sign up</b-link></strong>-->
              </small>
            </p>
          </div>
        </div>

        <!--          <div v-if="oAuthProviders && oAuthProviders.length > 0" class="card mt-3" data-cy="oAuthProviders">-->
        <!--            <div class="card-body">-->
        <!--              <div class="row">-->
        <!--                <div v-for="oAuthProvider in oAuthProviders" :key="oAuthProvider.registrationId" class="col-12 mb-3">-->
        <!--                  <button type="button" class="btn btn-outline-primary w-100"-->
        <!--                          @click="oAuth2Login(oAuthProvider.registrationId)" aria-label="oAuth authentication link">-->
        <!--                    <i :class="oAuthProvider.iconClass" aria-hidden="true" class="mr-1 text-info" />-->
        <!--                    Login via {{ oAuthProvider.clientName }}-->
        <!--                  </button>-->
        <!--                </div>-->
        <!--              </div>-->
        <!--            </div>-->
        <!--          </div>-->

        <!--        </Form>-->
      </div>
    </div>
  </div>
</template>

<style scoped></style>

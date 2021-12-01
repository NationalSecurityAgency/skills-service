/*
Copyright 2020 SkillTree

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
<template>
  <div class="container-fluid">
    <div class="row justify-content-center text-center">
    <div class="col col-md-8 col-lg-7 col-xl-4 mt-3" style="min-width: 20rem;">
      <div class="mt-5">
        <logo1 />
      </div>
      <ValidationObserver ref="form" slim v-slot="{invalid, handleSubmit}">
        <form @submit.prevent="handleSubmit(login)">
          <transition name="fade" mode="out-in">
            <b-alert :aria-live="loginFailed ? 'assertive' : 'off'" v-if="loginFailed"
                     variant="danger" @dismissed="loginFailed=false" show dismissible>Invalid Username
              or Password
            </b-alert>
          </transition>

          <div v-if="!oAuthOnly" class="card text-left">
            <div class="card-body p-4">

              <div class="form-group">
                <label for="username" class="text-primary">Email Address</label>
                <ValidationProvider name="Email Address" rules="required|minUsernameLength|email" :debounce=500 v-slot="{errors}">
                  <div class="input-group">
                    <div class="input-group-prepend">
                      <span class="input-group-text"><i class="far fa-envelope"></i></span>
                    </div>
                    <input type="text" class="form-control" id="username" tabindex="0" placeholder="Enter email"
                           :aria-invalid="errors && errors.length > 0"
                           aria-errormessage="emailHelp"
                           aria-describedby="emailHelp"
                           v-model="loginFields.username">
                  </div>
                  <small id="emailHelp" class="form-text text-danger" v-show="errors[0]">{{
                    errors[0]}}
                  </small>
                </ValidationProvider>
              </div>
              <div class="form-group">
                <div class="row">
                  <div class="col">
                    <label for="inputPassword" class="text-primary">Password</label>
                  </div>
                  <div class="col text-right">
                    <small class="text-muted"><b-link tabindex="0" @click="forgotPassword" data-cy="forgotPassword">Forgot Password?</b-link></small>
                  </div>
                </div>
                <ValidationProvider name="Password" rules="required|minPasswordLength|maxPasswordLength" :debounce=500 v-slot="{errors}">
                  <div class="input-group">
                    <div class="input-group-prepend">
                      <span class="input-group-text"><i class="fas fa-key"></i></span>
                    </div>
                    <input type="password" class="form-control" id="inputPassword" tabindex="0" placeholder="Password"
                           v-model="loginFields.password" name="password"
                           :aria-invalid="errors && errors.length > 0"
                           aria-errormessage="passwordHelp"
                           aria-describedby="passwordHelp"
                           @animationstart="onAnimationStart">
                  </div>
                  <small id="passwordHelp" class="form-text text-danger" v-show="errors[0]">{{
                    errors[0]}}
                  </small>
                </ValidationProvider>
              </div>
              <div class="row">
                <div class="col text-right">
                  <button type="submit" class="btn btn-outline-primary float-right" tabindex="0" :disabled="invalid||disabled" data-cy="login">
                    Login <i class="fas fa-arrow-circle-right"/>
                  </button>
                </div>
              </div>

              <hr/>
              <p class="text-center"><small>Don't have a SkillTree account?
                <strong><b-link @click="requestAccountPage">Sign up</b-link></strong>
              </small>
              </p>
            </div>
          </div>

          <div v-if="oAuthProviders && oAuthProviders.length > 0" class="card mt-3" data-cy="oAuthProviders">
            <div class="card-body">
              <div class="row">
                <div v-for="oAuthProvider in oAuthProviders" :key="oAuthProvider.registrationId" class="col-12 mb-3">
                  <button type="button" class="btn btn-outline-primary w-100"
                          @click="oAuth2Login(oAuthProvider.registrationId)" aria-label="oAuth authentication link">
                    <i :class="oAuthProvider.iconClass" aria-hidden="true" class="mr-1 text-info" />
                    Login via {{ oAuthProvider.clientName }}
                  </button>
                </div>
              </div>
            </div>
          </div>

        </form>
      </ValidationObserver>
    </div>
  </div>
  </div>
</template>

<script>
  import { extend } from 'vee-validate';
  import { required, email } from 'vee-validate/dist/rules';
  import AccessService from './AccessService';
  import Logo1 from '../brand/Logo1';
  import NavigationErrorMixin from '../utils/NavigationErrorMixin';

  extend('required', {
    ...required,
    message: '{_field_} is required',
  });
  extend('email', email);

  export default {
    name: 'LoginForm',
    components: { Logo1 },
    mixins: [NavigationErrorMixin],
    data() {
      return {
        loginFields: {
          username: '',
          password: '',
        },
        isAutoFilled: false,
        loginFailed: false,
        oAuthProviders: [],
      };
    },
    methods: {
      login() {
        this.loginFailed = false;
        const formData = new FormData();
        formData.append('username', this.loginFields.username);
        formData.append('password', this.loginFields.password);
        this.$store.dispatch('login', formData)
          .then(() => {
            this.loginFailed = false;
            this.handlePush(this.$route.query.redirect || '/');
          })
          .catch((error) => {
            if (error.response.status === 401) {
              this.resetAfterFailedLogin();
            } else {
              const errorMessage = (error.response && error.response.data && error.response.data.message) ? error.response.data.message : undefined;
              this.handlePush({ name: 'ErrorPage', query: { errorMessage } });
            }
          });
      },
      oAuth2Login(registrationId) {
        this.$store.dispatch('oAuth2Login', registrationId);
      },
      resetAfterFailedLogin() {
        this.loginFailed = true;
        delete this.loginFields.password;
        this.errors.clear();
      },
      requestAccountPage() {
        this.handlePush({ name: 'RequestAccount' });
      },
      forgotPassword() {
        this.handlePush({ name: 'ForgotPassword' });
      },
      onAnimationStart(event) {
        // required to work around chrome auto-fill issue (see see https://stackoverflow.com/a/41530164)
        if (event && event.animationName && event.animationName.startsWith('onAutoFillStart')) {
          this.isAutoFilled = true;
        } else {
          this.isAutoFilled = false;
        }
      },
    },
    computed: {
      disabled() {
        return (!this.isAutoFilled && (!this.loginFields.username || !this.loginFields.password));
      },
      oAuthOnly() {
        return this.$store.getters.config.oAuthOnly;
      },
    },
    created() {
      if (!this.$store.getters.isPkiAuthenticated) {
        AccessService.getOAuthProviders()
          .then((result) => {
            this.oAuthProviders = result;
          });
      }
    },
  };
</script>

<style lang="css" scoped>
  :-webkit-autofill {
    animation-name: onAutoFillStart;
  }

  :not(:-webkit-autofill) {
    animation-name: onAutoFillCancel;
  }

  @keyframes onAutoFillStart {
    from {
    }
    to {
    }
  }

  @keyframes onAutoFillCancel {
    from {
    }
    to {
    }
  }
</style>

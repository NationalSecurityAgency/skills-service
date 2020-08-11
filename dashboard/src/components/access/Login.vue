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
  <div class="row justify-content-center text-center">
    <div class="col col-md-8 col-lg-7 col-xl-4 mt-3" style="min-width: 20rem;">
      <div class="mt-5">
        <i class="fa fa-users fa-4x text-secondary"></i>
        <h2 class="mt-4 text-info">Sign in to SkillTree Dashboard</h2>
      </div>
      <form @submit.prevent="login()">
        <transition name="fade" mode="out-in">
          <b-alert v-if="loginFailed" variant="danger" @dismissed="loginFailed=false" show dismissible>Invalid Username
            or Password
          </b-alert>
        </transition>

        <div class="card text-left">
          <div class="card-body p-4">

            <div class="form-group">
              <label for="username" class="text-secondary font-weight-bold">Email Address</label>
              <input type="text" class="form-control" id="username" tabindex="1" placeholder="Enter email"
                     aria-describedby="emailHelp"
                     v-model="loginFields.username" v-validate="'required|minUsernameLength|email'" data-vv-delay="500" data-vv-name="email">
              <small id="emailHelp" class="form-text text-danger" v-show="errors.has('email')">{{
                errors.first('email')}}
              </small>
            </div>
            <div class="form-group">
              <div class="row">
                <div class="col">
                  <label for="inputPassword" class="text-secondary font-weight-bold">Password</label>
                </div>
                <div class="col text-right">
                  <small class="text-muted"><b-link tabindex="4" @click="forgotPassword" data-cy="forgotPassword">Forgot Password?</b-link></small>
                </div>
              </div>
              <input type="password" class="form-control" id="inputPassword" tabindex="2" placeholder="Password"
                     v-model="loginFields.password" name="password" aria-describedby="passwordHelp"
                     @animationstart="onAnimationStart" v-validate="'required|minPasswordLength|maxPasswordLength'" data-vv-delay="500" data-vv-name="password">
              <small id="passwordHelp" class="form-text text-danger" v-show="errors.has('password')">{{
                errors.first('password')}}
              </small>
            </div>
            <button type="submit" class="btn btn-outline-primary" tabindex="3" :disabled="disabled" data-cy="login">
              Login <i class="fas fa-arrow-circle-right"/>
            </button>

            <hr/>
            <p class="text-center"><small>Don't have a SkillTree account?
              <strong><b-link @click="requestAccountPage">Sign up</b-link></strong>
            </small>
            </p>
          </div>
        </div>

        <div v-if="oAuthProviders && oAuthProviders.length > 0" class="card mt-3">
          <div class="card-body">
            <div class="row">
              <div v-for="oAuthProvider in oAuthProviders" :key="oAuthProvider.registrationId" class="col">
                <button type="button" class="btn btn-outline-secondary w-100 h-100 text-dark"
                        @click="oAuth2Login(oAuthProvider.registrationId)">
                  <small><i :class="oAuthProvider.iconClass" aria-hidden="true" class="text-info"/>
                  Continue with {{ oAuthProvider.clientName }}
                  </small>
                </button>
              </div>
            </div>
          </div>
        </div>


      </form>
    </div>
  </div>
</template>

<script>
  import { Validator } from 'vee-validate';
  import AccessService from './AccessService';

  const dictionary = {
    en: {
      attributes: {
        password: 'Password',
        username: 'Username',
      },
    },
  };
  Validator.localize(dictionary);

  export default {
    name: 'LoginForm',
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
        this.$validator.validate()
          .then((valid) => {
            if (valid) {
              this.$validator.pause();
              this.loginFailed = false;
              const formData = new FormData();
              formData.append('username', this.loginFields.username);
              formData.append('password', this.loginFields.password);
              this.$store.dispatch('login', formData)
                .then(() => {
                  this.loginFailed = false;
                  this.$router.push(this.$route.query.redirect || '/');
                })
                .catch((error) => {
                  if (error.response.status === 401) {
                    this.resetAfterFailedLogin();
                  } else {
                    const errorMessage = (error.response && error.response.data && error.response.data.message) ? error.response.data.message : undefined;
                    this.$router.push({ name: 'ErrorPage', query: { errorMessage } });
                  }
                });
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
        this.$router.push({ name: 'RequestAccount' });
      },
      forgotPassword() {
        this.$router.push({ name: 'ForgotPassword' });
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
        return this.errors.any() || (!this.isAutoFilled && (!this.loginFields.username || !this.loginFields.password));
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

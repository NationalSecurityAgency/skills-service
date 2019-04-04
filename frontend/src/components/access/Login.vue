<template>
  <div class="columns section">
    <div class="column is-half is-offset-one-quarter">
      <div class="has-text-centered skills-pad-bottom-1-rem">
        <i class="fa fa-users fa-3x"></i>
        <h2 class="title is-5 skills-pad-top-2-rem">Sign in to Skills Dashboard</h2>
      </div>
      <form @submit.prevent="login()">
        <div class="columns">

          <div class="column is-three-fifths is-offset-one-fifth">

            <transition name="fade" mode="out-in">
              <div v-if="loginFailed" class="notification is-danger">
                <button class="delete" @click="loginFailed = false"/>
                Invalid Username or Password
              </div>
            </transition>

            <div class="box">

              <div class="field">
                <label class="label">Email</label>
                <input class="input" type="text" v-model="loginFields.username" name="username"
                       v-validate="'required|min:5'" data-vv-delay="500"/>
                <p class="help is-danger" v-show="errors.has('username')">{{ errors.first('username')}}</p>
              </div>
              <div class="field">
                <label class="label">Password</label>
                <input class="input" type="password" v-model="loginFields.password" name="password"
                       @animationstart="onAnimationStart" v-validate="'required|min:8|max:15'" data-vv-delay="500"/>
                <p class="help is-danger" v-show="errors.has('password')">{{ errors.first('password')}}</p>
              </div>
              <div class="field ">
                <div class="control">
                  <button class="button is-primary is-outlined" :disabled="disabled">
                    <span class="icon is-small">
                      <i class="fas fa-arrow-circle-right"/>
                    </span>
                    <span>Login</span>
                  </button>
                </div>
              </div>
              <div>
                  <p class="info" style="font-size: 0.8rem"><a @click="forgotPassword">Forgot Password?</a></p>
              </div>

              <div class="skills-pad-bottom-1-rem">
                <hr/>
                <p class="info has-text-centered">Don't have a User Skills account?
                  <a style="font-weight: bold" @click="requestAccountPage">Sign up</a>
                </p>
              </div>

            </div>

            <div class="box">
              <div v-if="oAuthProviders">
                <div v-for="oAuthProvider in oAuthProviders" :key="oAuthProvider.registrationId" class="field">
                  <a class="button is-outlined" style="width: 100%"
                     @click="oAuth2Login(oAuthProvider.registrationId)">
                    <span class="icon is-small">
                      <i :class="oAuthProvider.iconClass" aria-hidden="true"/>
                    </span>
                    <span>Continue with {{ oAuthProvider.clientName }}</span>
                  </a>
                </div>
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
        this.$validator.validate().then((valid) => {
          if (valid) {
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
        // TODO - add forgot password page
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
      AccessService.getOAuthProviders()
        .then((result) => {
          this.oAuthProviders = result;
        });
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
    from { }
    to { }
  }
  @keyframes onAutoFillCancel {
    from { }
    to { }
  }
</style>

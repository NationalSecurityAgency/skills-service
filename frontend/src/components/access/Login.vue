<template>
  <div class="columns section">
    <div class="column is-half is-offset-one-quarter">
      <div class="card">
        <div class="card-header has-background-grey-lighter">
          <span class="icon is-large">
            <i class="card-header-icon fas fa-award"/>
          </span>
          <p class="card-header-title">User Skills Management Portal</p>
        </div>
        <div class="card-content">
          <p>
            Welcome to the User Skills Management Portal.  User Skills is the corporate solution
            for adding gamification based training to your application(s).  User Skills offers a
            standard and consistent approach to gamified user training that strongly encourages
            user engagement to become domain and application experts.
          </p>
          <p style="margin-top:2em">
            Please login to view and manage your Projects, or you can Create an Account to get started. </p>
        </div>

        <p v-if="loginFailed" class="help is-danger has-text-centered">***Invalid Username or Password, please try again***</p>

        <div>
          <form @submit.prevent="login()">
            <div class="columns">

              <div class="column is-three-fifths is-offset-one-fifth">
                <div v-if="oAuthProviders" class="skills-pad-bottom-1-rem">
                  <div v-for="oAuthProvider in oAuthProviders" :key="oAuthProvider.registrationId" class="field">
                    <a class="button is-outlined" style="width: 100%"
                       @click="oAuth2Login(oAuthProvider.registrationId)">
                      <span class="icon is-small">
                        <i :class="oAuthProvider.iconClass" aria-hidden="true"/>
                      </span>
                      <span>Continue with {{ oAuthProvider.clientName }}</span>
                    </a>
                  </div>
                  <hr/>
                </div>

                <div class="field">
                  <label class="label">Email</label>
                  <input class="input" type="text" v-model="loginFields.username" name="username"
                         v-validate="'required|min:5'" data-vv-delay="500"/>
                  <p class="help is-danger" v-show="errors.has('username')">{{ errors.first('username')}}</p>
                </div>
                <div class="field">
                  <label class="label">Password</label>
                  <input class="input" type="password" v-model="loginFields.password" name="password"
                         v-validate="'required|min:8|max:15'" data-vv-delay="500"/>
                  <p class="help is-danger" v-show="errors.has('password')">{{ errors.first('password')}}</p>
                </div>
                <div class="field is-grouped">
                  <div class="control">
                    <button class="button is-primary is-outlined"
                            :disabled="errors.any() || !loginFields.username || !loginFields.password">
                      <span class="icon is-small">
                        <i class="fas fa-arrow-circle-right"/>
                      </span>
                      <span>Login</span>
                    </button>
                  </div>
                </div>
                <div>
                    <p class="info skills-pad-top-1-rem" style="font-size: 0.8rem"><a @click="forgotPassword">Forgot Password?</a></p>
                </div>

                <div class="skills-pad-bottom-1-rem">
                  <hr/>
                  <p class="info has-text-centered">Don't have a User Skills account?
                    <a style="font-weight: bold" @click="requestAccountPage">Sign up</a>
                  </p>
                </div>
              </div>

            </div>
          </form>
        </div>

      </div>
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
        loginFailed: false,
        oAuthProviders: [],
      };
    },
    methods: {
      login() {
        this.$validator.validate().then((valid) => {
          if (valid) {
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
        delete this.loginFields.username;
        delete this.loginFields.password;
        this.errors.clear();
      },
      requestAccountPage() {
        this.$router.push({ name: 'HomePage', query: { requestAccount: true } });
      },
      forgotPassword() {
        // TODO - add forgot password page
      },
    },
    created() {
      AccessService.getOAuthProviders()
        .then((result) => {
          // this.isLoading = false;
          this.oAuthProviders = result;
      });
    },
  };
</script>

<style scoped>

</style>

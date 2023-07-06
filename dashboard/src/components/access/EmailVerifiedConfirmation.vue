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
  <loading-container :is-loading="loading" class="container-fluid">
    <div class="row justify-content-center text-center" data-cy="emailConfirmation">
      <div class="col col-md-8 col-lg-7 col-xl-4 mt-3" style="min-width: 20rem;">
        <div class="mt-5">
          <logo1 />
          <div class="h3 mt-4 text-primary">Email Address Successfully Confirmed!</div>
        </div>
        <div class="card text-left">
          <div class="card-body p-4">
            <p>Your email address has been confirmed! You will be forwarded to the login page in {{ timer }} seconds.</p>
            <div class="text-center">
              <b-button href="/skills-login" variant="outline-primary" class="p-2" data-cy="loginPage"><i class="fas fa-sign-in-alt mr-1"/>Return to Login Page</b-button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </loading-container>
</template>

<script>
  import Logo1 from '../brand/Logo1';
  import AccessService from './AccessService';
  import LoadingContainer from '../utils/LoadingContainer';
  import NavigationErrorMixin from '../utils/NavigationErrorMixin';

  export default {
    name: 'EmailVerifiedConfirmation',
    components: { Logo1, LoadingContainer },
    mixins: [NavigationErrorMixin],
    props: {
      countDown: {
        type: Number,
        default: 10,
      },
      token: {
        type: String,
        default: '',
      },
      email: {
        type: String,
        default: '',
      },
    },
    data() {
      return {
        timer: -1,
        loading: true,
      };
    },
    mounted() {
      this.verifyEmail();
    },
    methods: {
      verifyEmail() {
        const verification = { token: this.token, email: this.email };
        AccessService.verifyEmail(verification).then(() => {
          this.loading = false;
          this.timer = this.countDown;
        }).catch((err) => {
          const params = {
            email: this.email,
            explanation: 'An error occurred while verifying your email address. Please click the button below to resend a new verification code.',
          };
          if (err && err.response && err.response.data && err.response.data.errorCode === 'UserTokenExpired') {
            params.explanation = 'Your email verification code has expired. Please click the button below to resend a new verification code.';
          }
          this.handlePush({ name: 'RequestEmailVerification', params });
        });
      },
    },
    watch: {
      timer(value) {
        if (value > 0) {
          setTimeout(() => {
            this.timer -= 1;
          }, 1000);
        } else {
          this.handlePush({ name: 'Login' });
        }
      },
    },
  };
</script>

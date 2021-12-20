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
      <div class="mt-5" data-cy="confirmEmailTitle">
        <logo1 />
        <h3 class="mt-4 text-primary">Email Verification is Required!</h3>
      </div>
      <div class="card text-left" data-cy="confirmEmailExplanation">
        <div class="card-body p-4">
          <div v-if="explanation">
            <p>{{explanation}}</p>
          </div>
          <div v-else>
            <p>You must first validate your email address in order to start using SkillTree.</p>
            <p>An email verification code has been sent to {{ email }}.</p>
            <p>Please check your email and confirm your email address to complete your SkillTree account creation, or you can click the button below to resend a new verification code.</p>
          </div>
          <div class="text-center">
              <b-button variant="outline-primary" @click="resend" data-cy="resendConfirmationCodeButton" aria-label="Resend Email Confirmation Code">
                Resend Email Confirmation Code <i class="fas fa-arrow-circle-right"/>
              </b-button>
            </div>
        </div>
      </div>
    </div>
  </div>
  </div>
</template>

<script>
  import AccessService from './AccessService';
  import Logo1 from '../brand/Logo1';
  import NavigationErrorMixin from '../utils/NavigationErrorMixin';

  export default {
    name: 'RequestEmailVerification',
    components: { Logo1 },
    mixins: [NavigationErrorMixin],
    props: {
      explanation: {
        type: String,
        default: '',
      },
      email: {
        type: String,
        required: true,
      },
    },
    data() {
      return {
      };
    },
    mounted() {
    },
    methods: {
      resend() {
        const { email } = this;
        AccessService.resendEmailVerification(email).then(() => {
          this.handlePush({ name: 'EmailVerificationSent', params: { email } });
        });
      },
    },
  };
</script>

<style lang="css" scoped>
</style>

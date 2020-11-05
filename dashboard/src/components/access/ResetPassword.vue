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
  <div class="container">
    <div class="row justify-content-center">
      <div class="col-md-6 mt-3">
        <div class="text-center mt-5">
          <i class="fa fa-users fa-4x text-secondary"></i>
          <h2 class="mt-4 text-info">
            <span>Reset Account Password</span>
          </h2>
        </div>
        <ValidationObserver ref="resetForm" v-slot="{invalid, handleSubmit}" slim>
          <form @submit.prevent="handleSubmit(changePassword)">
            <div class="card">
              <div class="card-body p-4">
                <div class="form-group">
                  <label for="email" class="text-secondary font-weight-bold">* Email</label>
                  <ValidationProvider name="Email" :debounce=500 rules="required|email" v-slot="{errors}">
                    <input class="form-control" type="text" v-model="resetFields.email" id="email" :disabled="resetInProgress"
                           name="email" data-cy="resetPasswordEmail" aria-required="true"/>
                    <small class="form-text text-danger" v-show="errors[0]">{{ errors[0]}}</small>
                  </ValidationProvider>
                </div>
                <div class="form-group">
                  <label for="password" class="text-secondary font-weight-bold">* New Password</label>
                  <ValidationProvider vid="password" name="New Password" :debounce=500 rules="required|minPasswordLength|maxPasswordLength" v-slot="{errors}">
                    <input class="form-control" type="password" v-model="resetFields.password" id="password" :disabled="resetInProgress"
                           name="password" data-cy="resetPasswordNewPassword" aria-required="true"/>
                    <small class="form-text text-danger" v-show="errors[0]">{{ errors[0] }}</small>
                  </ValidationProvider>
                </div>
                <div class="form-group">
                  <label for="password_confirmation" class="text-secondary font-weight-bold">* Confirm New Password</label>
                  <ValidationProvider name="Confirm New Password" :debounce=500 rules="required|confirmed:password" v-slot="{errors}">
                    <input class="form-control" type="password" v-model="passwordConfirmation" id="password_confirmation" :disabled="resetInProgress"
                           name="password_confirmation" data-cy="resetPasswordConfirm" aria-required="true"/>
                    <small class="form-text text-danger" v-show="errors[0]">{{ errors[0]}}</small>
                  </ValidationProvider>
                </div>
                <small class="text-danger" v-if="resetFailed" data-cy="resetError">{{remoteError}}</small>
                <div class="field is-grouped">
                  <div class="control">
                    <button type="submit" class="btn btn-outline-primary" :disabled="invalid || missingRequiredValues() || resetInProgress || remoteError" data-cy="resetPasswordSubmit">
                      Reset Password <i v-if="!resetInProgress" class="fas fa-arrow-circle-right" aria-hidden="true"/>
                      <b-spinner v-if="resetInProgress" label="Loading..." style="width: 1rem; height: 1rem;" variant="primary"/>
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
  import { required, email, confirmed } from 'vee-validate/dist/rules';
  import AccessService from './AccessService';

  extend('required', required);
  extend('email', email);
  extend('confirmed', confirmed);

  extend('uniqueEmail', {
    message: 'The email address is already used for another account.',
    validate(value) {
      return AccessService.userWithEmailExists(value);
    },
  });

  export default {
    name: 'RequestAccount',
    props: {
      resetToken: {
        type: String,
        default: '',
      },
    },
    data() {
      return {
        resetFields: {
          email: '',
          password: '',
        },
        resetInProgress: false,
        resetFailed: false,
        resetSuccessful: false,
        remoteError: null,
        passwordConfirmation: '',
      };
    },
    watch: {
      resetFields: {
        deep: true,
        handler() {
          if (this.remoteError) {
            this.remoteError = '';
          }
        },
      },
    },
    methods: {
      changePassword() {
        this.resetInProgress = true;
        const reset = { resetToken: this.resetToken, userId: this.resetFields.email, password: this.resetFields.password };
        this.resetFailed = false;
        this.resetSuccessful = false;
        this.remoteError = null;
        AccessService.resetPassword(reset).then(() => {
          this.resetInProgress = false;
          this.$router.push({ name: 'ResetConfirmation', params: { countDown: 10 } });
        }).catch((err) => {
          if (err && err.response && err.response.data && err.response.data.explanation) {
            this.remoteError = err.response.data.explanation;
          } else {
            this.remoteError = `Password reset failed due to ${err.response.status}`;
          }
          this.resetFailed = true;
          this.resetInProgress = false;
        });
      },
      missingRequiredValues() {
        return !this.resetFields.email || !this.resetFields.password;
      },
    },
  };
</script>

<style scoped>

</style>

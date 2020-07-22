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
        <form @submit.prevent="changePassword()">
          <div class="card">
            <div class="card-body p-4">
              <div class="form-group">
                <label for="email" class="text-secondary font-weight-bold">Email</label>
                <input class="form-control" type="text" v-model="resetFields.email" id="email" :disabled="resetInProgress"
                       name="email" v-validate="'required|email'" data-vv-delay="500" data-cy="resetPasswordEmail"/>
                <small class="form-text text-danger" v-show="errors.has('email')">{{ errors.first('email')}}</small>
              </div>
              <div class="form-group">
                <label for="password" class="text-secondary font-weight-bold">New Password</label>
                <input class="form-control" type="password" v-model="resetFields.password" id="password" :disabled="resetInProgress"
                       name="password" v-validate="'required|minPasswordLength|maxPasswordLength'" data-vv-delay="500" ref="password" data-cy="resetPasswordNewPassword"/>
                <small class="form-text text-danger" v-show="errors.has('password')">{{ errors.first('password')}}</small>
              </div>
              <div class="form-group">
                <label for="password_confirmation" class="text-secondary font-weight-bold">Confirm New Password</label>
                <input class="form-control" type="password" id="password_confirmation" :disabled="resetInProgress"
                       name="password_confirmation" v-validate="'required|confirmed:password'" data-vv-delay="500" data-vv-as="Password Confirmation" data-cy="resetPasswordConfirm"/>
                <small class="form-text text-danger" v-show="errors.has('password_confirmation')">{{ errors.first('password_confirmation')}}</small>
              </div>
              <small class="text-danger" v-if="this.resetFailed" data-cy="resetError">{{error}}</small>
              <div class="field is-grouped">
                <div class="control">
                  <button type="submit" class="btn btn-outline-primary" :disabled="errors.any() || missingRequiredValues() || resetInProgress" data-cy="resetPasswordSubmit">
                    Reset Password <i v-if="!resetInProgress" class="fas fa-arrow-circle-right"/>
                    <b-spinner v-if="resetInProgress" label="Loading..." style="width: 1rem; height: 1rem;" variant="primary"/>
                  </button>
                </div>
              </div>
            </div>
          </div>
        </form>
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
        password_confirmation: 'Password Confirmation',
        email: 'Email',
      },
    },
  };
  Validator.localize(dictionary);
  Validator.extend('uniqueEmail', {
    getMessage: 'The email address is already used for another account.',
    validate(value) {
      return AccessService.userWithEmailExists(value);
    },
  }, {
    immediate: false,
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
        error: null,
      };
    },
    methods: {
      changePassword() {
        this.$validator.validate().then((valid) => {
          if (valid) {
            this.resetInProgress = true;
            const reset = { resetToken: this.resetToken, userId: this.resetFields.email, password: this.resetFields.password };
            this.resetFailed = false;
            this.resetSuccessful = false;
            this.error = null;
            AccessService.resetPassword(reset).then(() => {
              this.resetInProgress = false;
              this.$router.push({ name: 'ResetConfirmation', params: { countDown: 30 } });
            }).catch((err) => {
              if (err && err.response && err.response.data && err.response.data.explanation) {
                this.error = err.response.data.explanation;
              } else {
                this.error = `Password reset failed due to ${err.response.status}`;
              }
              this.resetFailed = true;
              this.resetInProgress = false;
            });
          }
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

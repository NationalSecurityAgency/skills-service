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
        <h3 class="mt-4 text-primary">Reset Password For SkillTree Dashboard</h3>
      </div>
      <ValidationObserver ref="resetForm" v-slot="{invalid, handleSubmit}" slim>
        <form @submit.prevent="handleSubmit(reset)">
          <div class="card text-left">
            <div class="card-body p-4">

              <div class="form-group">
                <label for="username" class="text-secondary font-weight-bold">* Email Address</label>
                <ValidationProvider name="Email Address" rules="required|minUsernameLength|email" :debounce=300 v-slot="{errors}">
                  <input type="text" class="form-control" id="username" tabindex="1" placeholder="Enter email"
                         aria-errormessage="emailHelp"
                         :aria-invalid="errors && errors.length > 0"
                         v-model="username"
                         data-cy="forgotPasswordEmail"
                        aria-required="true">
                    <small id="emailHelp" class="form-text text-danger" v-show="errors[0]">{{
                      errors[0] }}
                    </small>
                    <small class="text-danger" v-if="serverError" data-cy="resetFailedError" role="alert">{{ serverError }}</small>
                </ValidationProvider>
              </div>
              <button type="submit" class="btn btn-outline-primary" tabindex="3" :disabled="invalid || (disabled === true)" data-cy="resetPassword">
                Reset Password <i class="fas fa-arrow-circle-right"/>
              </button>
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

  extend('required', required);
  extend('email', email);

  export default {
    name: 'RequestPasswordResetForm',
    components: { Logo1 },
    data() {
      return {
        username: '',
        resetSent: false,
        isAutoFilled: false,
        serverError: '',
      };
    },
    mounted() {
      AccessService.isResetSupported().then((response) => {
        if (response === false) {
          this.$router.replace({ name: 'ResetNotSupportedPage' });
        }
      });
    },
    watch: {
      username(newVal, oldVal) {
        if (newVal.trim() !== oldVal.trim()) {
          this.serverError = '';
        }
      },
    },
    methods: {
      reset() {
        AccessService.requestPasswordReset(this.username).then((response) => {
          this.serverError = '';
          if (response.success) {
            this.$router.push({ name: 'RequestResetConfirmation', params: { countDown: 10, email: this.username } });
          }
        }).catch((err) => {
          if (err && err.response && err.response.data && err.response.data.explanation) {
            this.serverError = err.response.data.explanation;
          } else {
            this.serverError = `Password reset request failed due to ${err.response.status}`;
          }
        });
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
        return (!this.isAutoFilled && (!this.username)) || this.serverError !== '';
      },
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

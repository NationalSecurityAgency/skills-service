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
        <h2 class="mt-4 text-info">Reset Password For SkillTree Dashboard</h2>
      </div>
      <form @submit.prevent="reset()">
        <div class="card text-left">
          <div class="card-body p-4">

            <div class="form-group">
              <label for="username" class="text-secondary font-weight-bold">Email Address</label>
              <input type="text" class="form-control" id="username" tabindex="1" placeholder="Enter email"
                     aria-describedby="emailHelp"
                     v-model="resetFields.username" v-validate="'required|minUsernameLength|email'"
                     data-vv-delay="500" data-vv-name="email"
                      data-cy="forgotPasswordEmail">
              <small id="emailHelp" class="form-text text-danger" v-show="errors.has('email')">{{
                errors.first('email')}}
              </small>
              <small class="text-center" data-cy="resetSent" v-if="this.resetSent">
              A password reset link as been sent, you will be forwarded to the login page in {{countDown}} seconds
              </small>
            </div>
            <button type="submit" class="btn btn-outline-primary" tabindex="3" :disabled="disabled" data-cy="resetPassword">
              Reset Password <i class="fas fa-arrow-circle-right"/>
            </button>
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
        username: 'Username',
      },
    },
  };
  Validator.localize(dictionary);

  export default {
    name: 'RequestPasswordResetForm',
    data() {
      return {
        resetFields: {
          username: '',
        },
        resetSent: false,
        isAutoFilled: false,
        countDown: -1,
      };
    },
    watch: {
      countDown(value) {
        if (value > 0) {
          setTimeout(() => {
            this.countDown -= 1;
          }, 1000);
        } else if (this.resetSent) {
          this.$router.push({ name: 'Login' });
        }
      },
    },
    methods: {
      reset() {
        this.$validator.validate()
          .then((valid) => {
            if (valid) {
              this.$validator.pause();

              AccessService.requestPasswordReset(this.resetFields.username).then((response) => {
                if (response.success) {
                  this.resetSent = true;
                  this.countDown = 15;
                }
              });
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
        return this.errors.any() || (!this.isAutoFilled && (!this.resetFields.username));
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

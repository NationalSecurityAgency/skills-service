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
            <span v-if="isRootAccount">New SkillTree Root Account</span>
            <span v-else>New SkillTree Account</span>
          </h2>
        </div>
        <form @submit.prevent="login()">
          <div class="card">
            <div class="card-body p-4">
              <div class="form-group">
                <label for="firstName" class="text-secondary font-weight-bold">First Name</label>
                <input class="form-control" type="text" v-model="loginFields.firstName" id="firstName" :disabled="createInProgress"
                       name="firstName" v-validate="'required|maxFirstNameLength'" data-vv-delay="500"/>
                <small class="form-text text-danger" v-show="errors.has('firstName')">{{ errors.first('firstName')}}</small>
              </div>
              <div class="form-group">
                <label for="lastName" class="text-secondary font-weight-bold">Last Name</label>
                <input class="form-control" type="text" v-model="loginFields.lastName" id="lastName" :disabled="createInProgress"
                       name="lastName" v-validate="'required|maxLastNameLength'" data-vv-delay="500"/>
                <small class="form-text text-danger" v-show="errors.has('lastName')">{{ errors.first('lastName')}}</small>
              </div>
              <div class="form-group">
                <label for="email" class="text-secondary font-weight-bold">Email</label>
                <input class="form-control" type="text" v-model="loginFields.email" id="email" :disabled="createInProgress"
                       name="email" v-validate="'required|email|uniqueEmail'" data-vv-delay="500"/>
                <small class="form-text text-danger" v-show="errors.has('email')">{{ errors.first('email')}}</small>
              </div>
              <div class="form-group">
                <label for="password" class="text-secondary font-weight-bold">Password</label>
                <input class="form-control" type="password" v-model="loginFields.password" id="password" :disabled="createInProgress"
                       name="password" v-validate="'required|minPasswordLength|maxPasswordLength'" data-vv-delay="500" ref="password"/>
                <small class="form-text text-danger" v-show="errors.has('password')">{{ errors.first('password')}}</small>
              </div>
              <div class="form-group">
                <label for="password_confirmation" class="text-secondary font-weight-bold">Confirm Password</label>
                <input class="form-control" type="password" id="password_confirmation" :disabled="createInProgress"
                       name="password_confirmation" v-validate="'required|confirmed:password'" data-vv-delay="500" data-vv-as="Password Confirmation"/>
                <small class="form-text text-danger" v-show="errors.has('password_confirmation')">{{ errors.first('password_confirmation')}}</small>
              </div>
              <div class="field is-grouped">
                <div class="control">
                  <button type="submit" class="btn btn-outline-primary" :disabled="errors.any() || missingRequiredValues() || createInProgress">
                    Create Account <i v-if="!createInProgress" class="fas fa-arrow-circle-right"/>
                    <b-spinner v-if="createInProgress" label="Loading..." style="width: 1rem; height: 1rem;" variant="primary"/>
                  </button>
                  <div v-if="createInProgress && isRootAccount" class="mt-2 text-info">
                    Bootstrapping! May take a second...
                  </div>
                </div>
              </div>
              <div v-if="!isRootAccount" class="skills-pad-bottom-1-rem">
                <hr/>
                <p class="text-center"><small>Already have an account?
                  <strong><b-link @click="loginPage">Sign in</b-link></strong></small>
                </p>
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
        firstName: 'First Name',
        lastName: 'Last Name',
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
      isRootAccount: {
        type: Boolean,
        default: false,
      },
    },
    data() {
      return {
        loginFields: {
          firstName: '',
          lastName: '',
          email: '',
          password: '',
        },
        createInProgress: false,
      };
    },
    methods: {
      login() {
        this.$validator.validate().then((valid) => {
          if (valid) {
            this.createInProgress = true;
            this.$store.dispatch('signup', { isRootAccount: this.isRootAccount, ...this.loginFields }).then(() => {
              this.$router.push({ name: 'HomePage' });
            });
          }
        });
      },
      missingRequiredValues() {
        return !this.loginFields.firstName || !this.loginFields.lastName || !this.loginFields.email || !this.loginFields.password;
      },
      loginPage() {
        this.$router.push({ name: 'Login' });
      },
    },
  };
</script>

<style scoped>

</style>

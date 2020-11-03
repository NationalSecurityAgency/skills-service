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
          <logo1 />
          <h3 class="mt-4 text-primary">
            New <span v-if="isRootAccount">Root </span>Account
          </h3>
        </div>
        <ValidationObserver ref="observer" v-slot="{invalid, handleSubmit}" slim>
          <form @submit.prevent="handleSubmit(login)">
            <div class="card">
              <div class="card-body p-4">
                <div class="form-group">
                  <label for="firstName" class="text-primary">* First Name</label>
                  <ValidationProvider name="First Name" rules="required|maxFirstNameLength" v-slot="{errors}" :debounce=500>
                    <div class="input-group">
                      <div class="input-group-prepend">
                        <span class="input-group-text"><i class="fas fa-user"></i></span>
                      </div>
                      <input class="form-control" type="text" v-model="loginFields.firstName" id="firstName" :disabled="createInProgress"
                           name="firstName" aria-required="true"
                          :aria-invalid="errors && errors.length > 0"
                          aria-errormessage="firstnameError"/>
                    </div>
                    <small class="form-text text-danger" v-show="errors[0]" id="firstnameError">{{ errors[0] }}</small>
                  </ValidationProvider>
                </div>
                <div class="form-group">
                  <label for="lastName" class="text-primary">* Last Name</label>
                  <ValidationProvider name="Last Name" rules="required|maxLastNameLength" :debounce=500 v-slot="{errors}">
                    <div class="input-group">
                      <div class="input-group-prepend">
                        <span class="input-group-text"><i class="fas fa-user-tie"></i></span>
                      </div>
                      <input class="form-control" type="text" v-model="loginFields.lastName" id="lastName" :disabled="createInProgress"
                           name="lastName" aria-required="true" :aria-invalid="errors && errors.length > 0" aria-errormessage="lastnameError"/>
                    </div>
                    <small class="form-text text-danger" v-show="errors[0]" id="lastnameError">{{ errors[0] }}</small>
                  </ValidationProvider>
                </div>
                <div class="form-group">
                  <label for="email" class="text-primary">* Email</label>
                  <ValidationProvider name="Email" rules="required|email|uniqueEmail" :debounce=500 v-slot="{errors}">
                    <div class="input-group">
                      <div class="input-group-prepend">
                        <span class="input-group-text"><i class="far fa-envelope"></i></span>
                      </div>
                      <input class="form-control" type="text" v-model="loginFields.email" id="email" :disabled="createInProgress"
                           name="email" aria-required="true" :aria-invalid="errors && errors.length > 0" aria-errormessage="emailErrors"/>
                    </div>
                    <small class="form-text text-danger" v-show="errors[0]" id="emailErrors">{{ errors[0] }}</small>
                  </ValidationProvider>
                </div>
                <div class="form-group">
                  <label for="password" class="text-primary">* Password</label>
                  <ValidationProvider vid="password" name="Password" rules="required|minPasswordLength|maxPasswordLength" :debounce=500 v-slot="{errors}">
                    <div class="input-group">
                      <div class="input-group-prepend">
                        <span class="input-group-text"><i class="fas fa-key"></i></span>
                      </div>
                      <input class="form-control" type="password" v-model="loginFields.password" id="password" :disabled="createInProgress"
                           name="password" ref="password" aria-required="true" :aria-invalid="errors && errors.length > 0" aria-errormessage="passwordError"/>
                    </div>
                    <small class="form-text text-danger" v-show="errors[0]" id="passwordError">{{ errors[0] }}</small>
                  </ValidationProvider>
                </div>
                <div class="form-group">
                  <label for="password_confirmation" class="text-primary">* Confirm Password</label>
                  <ValidationProvider vid="password_confirmation" name="Password" rules="required|confirmed:password" :debounce=500 v-slot="{errors}">
                    <div class="input-group">
                      <div class="input-group-prepend">
                        <span class="input-group-text"><i class="fas fa-key"></i></span>
                      </div>
                      <input class="form-control" type="password" id="password_confirmation" v-model="passwordConfirmation" :disabled="createInProgress"
                           name="password_confirmation" aria-required="true" :aria-invalid="errors && errors.length" aria-errormessage="passwordConfirmationError"/>
                    </div>
                    <small class="form-text text-danger" v-show="errors[0]" id="passwordConfirmationError">{{ errors[0] }}</small>
                  </ValidationProvider>
                </div>
                <div class="field is-grouped">
                  <div class="control">
                    <div class="row">
                      <div class="col text-right">
                        <button type="submit" class="btn btn-outline-primary" :disabled="invalid || missingRequiredValues() || createInProgress">
                          Create Account <i v-if="!createInProgress" class="fas fa-arrow-circle-right"/>
                          <b-spinner v-if="createInProgress" label="Loading..." style="width: 1rem; height: 1rem;" variant="primary"/>
                        </button>
                        <div v-if="createInProgress && isRootAccount" class="mt-2 text-info">
                          Bootstrapping! May take a second...
                        </div>
                      </div>
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
        </ValidationObserver>
      </div>
    </div>
  </div>
</template>

<script>
  import { extend } from 'vee-validate';
  import { required, email, confirmed } from 'vee-validate/dist/rules';
  import AccessService from './AccessService';
  import Logo1 from '../brand/Logo1';

  extend('required', {
    ...required,
    message: '{_field_} is required',
  });
  extend('email', email);
  extend('confirmed', confirmed);
  extend('uniqueEmail', {
    message: (fieldName) => `${fieldName} is already used for another account.`,
    validate(value) {
      return AccessService.userWithEmailExists(value);
    },
  });

  export default {
    name: 'RequestAccount',
    components: { Logo1 },
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
        passwordConfirmation: '',
        createInProgress: false,
      };
    },
    methods: {
      login() {
        this.createInProgress = true;
        this.$store.dispatch('signup', { isRootAccount: this.isRootAccount, ...this.loginFields }).then(() => {
          this.$router.push({ name: 'HomePage' });
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

<template>
  <div class="row justify-content-center">
    <div class="col-md-8 col-lg-7 col-xl-4 mt-3" style="min-width: 20rem;">
      <div class="text-center mt-5">
        <i class="fa fa-users fa-4x"></i>
        <h2 class="mt-4">Create Skills Dashboard Account</h2>
      </div>
      <form @submit.prevent="login()">
        <div class="card">
            <div class="card-body p-4">
              <div class="form-group">
                <label for="firstName">First Name</label>
                <input class="form-control" type="text" v-model="loginFields.firstName" id="firstName"
                       name="firstName" v-validate="'required'" data-vv-delay="500"/>
                <small class="form-text text-danger" v-show="errors.has('firstName')">{{ errors.first('firstName')}}</small>
              </div>
              <div class="form-group">
                <label for="lastName">Last Name</label>
                <input class="form-control" type="text" v-model="loginFields.lastName" id="lastName"
                       name="lastName" v-validate="'required'" data-vv-delay="500"/>
                <small class="form-text text-danger" v-show="errors.has('lastName')">{{ errors.first('lastName')}}</small>
              </div>
              <div class="form-group">
                <label for="email">Email</label>
                <input class="form-control" type="text" v-model="loginFields.email" id="email"
                       name="email" v-validate="'required|email|uniqueEmail'" data-vv-delay="500"/>
                <small class="form-text text-danger" v-show="errors.has('email')">{{ errors.first('email')}}</small>
              </div>
              <div class="form-group">
                <label for="password">Password</label>
                <input class="form-control" type="password" v-model="loginFields.password" id="password"
                       name="password" v-validate="'required|min:8|max:15'" data-vv-delay="500" ref="password"/>
                <small class="form-text text-danger" v-show="errors.has('password')">{{ errors.first('password')}}</small>
              </div>
              <div class="form-group">
                <label for="password_confirmation">Confirm Password</label>
                <input class="form-control" type="password" id="password_confirmation"
                       name="password_confirmation" v-validate="'required|confirmed:password'" data-vv-delay="500" data-vv-as="Password Confirmation"/>
                <small class="form-text text-danger" v-show="errors.has('password_confirmation')">{{ errors.first('password_confirmation')}}</small>
              </div>
              <div class="field is-grouped">
                <div class="control">
                  <button type="submit" class="btn btn-outline-primary" :disabled="errors.any() || missingRequiredValues()">
                    Create Account <i class="fas fa-arrow-circle-right"/>
                  </button>
                </div>
              </div>
              <div class="skills-pad-bottom-1-rem">
                <hr/>
                <p class="text-center"><small>Already have a User Skills account?
                  <strong><b-link @click="loginPage">Sign in</b-link></strong></small>
                </p>
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
    data() {
      return {
        loginFields: {
          firstName: '',
          lastName: '',
          email: '',
          password: '',
        },
      };
    },
    methods: {
      login() {
        this.$validator.validate().then((valid) => {
          if (valid) {
            this.$store.dispatch('signup', this.loginFields).then(() => {
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

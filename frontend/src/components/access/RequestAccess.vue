<template>
  <div class="columns section">
    <div class="column is-half is-offset-one-quarter">
      <div class="has-text-centered skills-pad-bottom-1-rem">
        <i class="fa fa-users fa-3x"></i>
        <h2 class="title is-5 skills-pad-top-2-rem">Create Skills Dashboard Account</h2>
      </div>
      <form @submit.prevent="login()">
        <div class="columns">
          <div class="column is-three-fifths is-offset-one-fifth">

            <div class="box">
              <div class="field">
                <label class="label">First Name</label>
                <input class="input" type="text" v-model="loginFields.firstName" name="firstName"
                       v-validate="'required'" data-vv-delay="500"/>
                <p class="help is-danger" v-show="errors.has('firstName')">{{ errors.first('firstName')}}</p>
              </div>
              <div class="field">
                <label class="label">Last Name</label>
                <input class="input" type="text" v-model="loginFields.lastName" name="lastName"
                       v-validate="'required'" data-vv-delay="500"/>
                <p class="help is-danger" v-show="errors.has('lastName')">{{ errors.first('lastName')}}</p>
              </div>
              <div class="field">
                <label class="label">Email</label>
                <input class="input" type="text" v-model="loginFields.email" name="email"
                       v-validate="'required|email|uniqueEmail'" data-vv-delay="500"/>
                <p class="help is-danger" v-show="errors.has('email')">{{ errors.first('email')}}</p>
              </div>
              <div class="field">
                <label class="label">Password</label>
                <input class="input" type="password" v-model="loginFields.password" name="password"
                       v-validate="'required|min:8|max:15'" data-vv-delay="500" ref="password"/>
                <p class="help is-danger" v-show="errors.has('password')">{{ errors.first('password')}}</p>
              </div>
              <div class="field">
                <label class="label">Confirm Password</label>
                <input class="input" type="password" name="password_confirmation"
                       v-validate="'required|confirmed:password'" data-vv-delay="500" data-vv-as="Password Confirmation"/>
                <p class="help is-danger" v-show="errors.has('password_confirmation')">{{ errors.first('password_confirmation')}}</p>
              </div>
              <div class="field is-grouped">
                <div class="control">
                  <button class="button is-primary is-outlined" :disabled="errors.any() || missingRequiredValues()">
                    <span class="icon is-small">
                      <i class="fas fa-arrow-circle-right"/>
                    </span>
                    <span>Create Account</span>
                  </button>
                </div>
              </div>
              <div class="skills-pad-bottom-1-rem">
                <hr/>
                <p class="info has-text-centered">Already have a User Skills account?
                  <a style="font-weight: bold" @click="loginPage">Sign in</a>
                </p>
              </div>
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
              this.$router.push({ name: 'RequestAccount' });
            });
          }
        });
      },
      missingRequiredValues() {
        return !this.loginFields.firstName || !this.loginFields.lastName || !this.loginFields.email || !this.loginFields.password;
      },
      loginPage() {
        this.$router.push({ name: 'HomePage' });
      },
    },
  };
</script>

<style scoped>

</style>

<template>
    <div class="container is-fluid">
        <div class="columns section">
            <div class="column is-half is-offset-one-quarter">
                <div class="card">
                    <div class="card-header has-background-grey-lighter">
                        <span class="icon is-large">
                          <i class="card-header-icon fas fa-award"/>
                        </span>
                        <p class="card-header-title">User Skills Management Portal</p>
                    </div>
                    <div class="card-content">
                        <p>
                            Welcome to the User Skills Management Portal.  It looks like the application isn't fully setup. Before you can start using the Portal, you'll need a root user configured to update settings, grant permissions, etc.
                        </p>
                        <p style="margin-top:2em">
                            Please create the root account to get started. </p>
                    </div>
                    <form @submit.prevent="validateAndLogin">
                        <div class="columns">

                            <div class="column is-three-fifths is-offset-one-fifth">
                                <div class="field">
                                    <label class="label">First Name</label>
                                    <input class="input" type="text" v-model="loginFields.firstName" name="firstName"
                                           v-validate="'required'" data-vv-delay="500"/>
                                    <p class="help is-danger" v-show="errors.has('firstName')">{{
                                        errors.first('firstName')}}</p>
                                </div>
                                <div class="field">
                                    <label class="label">Last Name</label>
                                    <input class="input" type="text" v-model="loginFields.lastName" name="lastName"
                                           v-validate="'required'" data-vv-delay="500"/>
                                    <p class="help is-danger" v-show="errors.has('lastName')">{{
                                        errors.first('lastName')}}</p>
                                </div>
                                <div class="field">
                                    <label class="label">Email</label>
                                    <input class="input" type="text" v-model="loginFields.email" name="email"
                                           v-validate="'required|email'" data-vv-delay="500"/>
                                    <p class="help is-danger" v-show="errors.has('email')">{{
                                        errors.first('email')}}</p>
                                </div>
                                <div class="field">
                                    <label class="label">Password</label>
                                    <input class="input" type="password" v-model="loginFields.password" name="password" ref="password"
                                           placeholder="At least 8 characters"
                                           v-validate="'required|min:8|max:15'" data-vv-delay="500"/>
                                    <p class="help is-danger" v-show="errors.has('password')">{{
                                        errors.first('password')}}</p>
                                </div>
                                <div class="field">
                                    <label class="label">Re-enter Password</label>
                                    <input class="input" type="password" v-model="loginFields.password_confirmation" name="password_confirmation"
                                           v-validate="'required|confirmed:password'" data-vv-delay="500" data-vv-as="password"/>
                                    <p class="help is-danger" v-show="errors.has('password_confirmation')">{{
                                        errors.first('password_confirmation')}}</p>
                                </div>
                                <div class="field">
                                    <div class="control">
                                        <button class="button is-primary is-outlined"
                                                :disabled="errors.any() || missingRequiredValues()">
                                            <span class="icon is-small">
                                              <i class="fas fa-arrow-circle-right"/>
                                            </span>
                                            <span>Create Account</span>
                                        </button>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </form>
                </div>
            </div>
        </div>
    </div>
</template>

<script>
  import { Validator } from 'vee-validate';

  const dictionary = {
    en: {
      attributes: {
        password: 'Password',
        email: 'Email',
        firstName: 'First Name',
        lastName: 'Last Name',
      },
    },
  };
  Validator.localize(dictionary);

  export default {
    name: 'RootRegistration',
    components: {},
    data() {
      return {
        loginFields: {
          firstName: '',
          lastName: '',
          email: '',
          password: '',
          password_confirmation: '',
        },
        serverErrors: [],
      };
    },
    methods: {
      validateAndLogin() {
        this.$validator.validate().then((valid) => {
          if (valid) {
            this.$emit('login', this.loginFields);
          }
        });
      },
      missingRequiredValues() {
        return !this.loginFields.firstName || !this.loginFields.lastName || !this.loginFields.email || !this.loginFields.password || !this.loginFields.password_confirmation;
      },
    },
  };
</script>

<style scoped>

</style>

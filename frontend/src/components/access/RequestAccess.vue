<template>
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
            Welcome to the User Skills Management Portal.  User Skills is the corporate solution
            for adding gamification based training to your application(s).  User Skills offers a
            standard and consistent approach to gamified user training that strongly encourages
            user engagement to become domain and application experts.
          </p>
          <p style="margin-top:2em">
            Please login to view and manage your Projects, or you can Create an Account to get started. </p>
        </div>

        <div>
          <form @submit.prevent="login()">
            <div class="columns">

              <div class="column is-three-fifths is-offset-one-fifth">
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
                         v-validate="'required|email'" data-vv-delay="500"/>
                  <p class="help is-danger" v-show="errors.has('email')">{{ errors.first('email')}}</p>
                </div>
                <div class="field">
                  <label class="label">Password</label>
                  <input class="input" type="password" v-model="loginFields.password" name="password"
                         v-validate="'required|min:8|max:15'" data-vv-delay="500"/>
                  <p class="help is-danger" v-show="errors.has('password')">{{ errors.first('password')}}</p>
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
    name: 'RequestAccount',
    data() {
      return {
        loginFields: {
          firstName: '',
          lastName: '',
          email: '',
          password: '',
        },
        serverErrors: [],
      };
    },
    methods: {
      login() {
        this.$validator.validate().then((valid) => {
          if (valid) {
            this.$store.dispatch('signup', this.loginFields).then(() => {
              this.$router.push({ name: 'HomePage' });
            })
              .catch((error) => {
                this.serverErrors.push(error);
                throw error;
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

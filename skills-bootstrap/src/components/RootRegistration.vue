<template>
  <div class="container">
    <div class="row justify-content-center">
      <div class="col-md-8 col-lg-6 mt-5">
        <div class="card">
          <div class="card-header">
            <strong>
              <i class="fas fa-award mr-3"/>User Skills Management Dashboard
            </strong>
          </div>
          <div class="card-body">
            <div>
              <p>Welcome to the User Skills Management Dashboard. It looks like the application isn't fully setup. Before you can start using the Dashboard, you'll need a root user configured to update settings, grant permissions, etc.</p>
              <p style="margin-top:2em">Please create the root account to get started.</p>
            </div>
            <form @submit.prevent="validateAndLogin">
              <div class="columns">
                <div class="column is-three-fifths is-offset-one-fifth">
                  <div class="form-group">
                    <label class="label">First Name</label>
                    <input
                      class="form-control"
                      type="text"
                      v-model="loginFields.firstName"
                      name="firstName"
                      v-validate="'required'"
                      data-vv-delay="500"
                    >
                    <p class="help is-danger" v-show="errors.has('firstName')">{{errors.first('firstName')}}</p>
                  </div>
                  <div class="form-group">
                    <label class="label">Last Name</label>
                    <input
                      class="form-control"
                      type="text"
                      v-model="loginFields.lastName"
                      name="lastName"
                      v-validate="'required'"
                      data-vv-delay="500"
                    >
                    <p class="help is-danger" v-show="errors.has('lastName')">{{errors.first('lastName')}}</p>
                  </div>
                  <div class="form-group">
                    <label class="label">Email</label>
                    <input
                      class="form-control"
                      type="text"
                      v-model="loginFields.email"
                      name="email"
                      v-validate="'required|email|uniqueEmail'"
                      data-vv-delay="500"
                    >
                    <p class="help is-danger" v-show="errors.has('email')">{{errors.first('email')}}</p>
                  </div>
                  <div class="form-group">
                    <label class="label">Password</label>
                    <input
                      class="form-control"
                      type="password"
                      v-model="loginFields.password"
                      name="password"
                      ref="password"
                      placeholder="At least 8 characters"
                      v-validate="'required|min:8|max:15'"
                      data-vv-delay="500"
                    >
                    <p class="help is-danger" v-show="errors.has('password')">{{errors.first('password')}}</p>
                  </div>
                  <div class="form-group">
                    <label class="label">Re-enter Password</label>
                    <input
                      class="form-control"
                      type="password"
                      v-model="loginFields.password_confirmation"
                      name="password_confirmation"
                      v-validate="'required|confirmed:password'"
                      data-vv-delay="500"
                      data-vv-as="password"
                    >
                    <p class="help is-danger" v-show="errors.has('password_confirmation')">{{errors.first('password_confirmation')}}</p>
                  </div>
                  <div>
                    <button class="btn btn-outline-primary" :disabled="errors.any() || missingRequiredValues()">
                      <i class="fas fa-arrow-circle-right"/> Create Account
                    </button>
                  </div>
                </div>
              </div>
            </form>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { Validator } from "vee-validate";
import BootstrapService from "./BootstrapService";

const dictionary = {
  en: {
    attributes: {
      password: "Password",
      email: "Email",
      firstName: "First Name",
      lastName: "Last Name"
    }
  }
};
Validator.localize(dictionary);
Validator.extend(
  "uniqueEmail",
  {
    getMessage: "The email address is already used for another account.",
    validate(value) {
      return BootstrapService.userWithEmailExists(value).catch(e => {
        throw e;
      });
    }
  },
  {
    immediate: false
  }
);

export default {
  name: "RootRegistration",
  components: {},
  data() {
    return {
      loginFields: {
        firstName: "",
        lastName: "",
        email: "",
        password: "",
        password_confirmation: ""
      }
    };
  },
  methods: {
    validateAndLogin() {
      this.$validator.validate().then(valid => {
        if (valid) {
          this.$emit("registerUser", this.loginFields);
        }
      });
    },
    missingRequiredValues() {
      return (
        !this.loginFields.firstName ||
        !this.loginFields.lastName ||
        !this.loginFields.email ||
        !this.loginFields.password ||
        !this.loginFields.password_confirmation
      );
    }
  }
};
</script>

<style scoped>
</style>

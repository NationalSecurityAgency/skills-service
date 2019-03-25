<template>
  <div class="column is-half is-offset-one-quarter">
    <loading-container v-bind:is-loading="isLoading">
      <div class="card">
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
            <div class="field is-grouped">
              <div class="control">
                <button class="button is-primary is-outlined" :disabled="errors.any() || !hasChangedValues()">
                        <span class="icon is-small">
                          <i class="fas fa-arrow-circle-right"/>
                        </span>
                  <span>Save</span>
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </loading-container>
  </div>
</template>

<script>
  import { Validator } from 'vee-validate';
  import LoadingContainer from '../utils/LoadingContainer';

  const dictionary = {
    en: {
      attributes: {
        firstName: 'First Name',
        lastName: 'Last Name',
      },
    },
  };
  Validator.localize(dictionary);

  export default {
    name: 'GeneralSettings',
    components: { LoadingContainer },
    data() {
      return {
        isLoading: true,
        loginFields: {
          firstName: '',
          lastName: '',
        },
        originalValues: {
          firstName: '',
          lastName: '',
        },
      };
    },
    mounted() {
      this.loadData();
    },
    methods: {
      loadData() {
        const { userInfo } = this.$store.getters;
        if (userInfo !== null) {
          this.loginFields.firstName = userInfo.first;
          this.loginFields.lastName = userInfo.last;
          this.originalValues.firstName = userInfo.first;
          this.originalValues.lastName = userInfo.last;
        }
        this.isLoading = false;
      },
      hasChangedValues() {
        return this.originalValues.firstName !== this.loginFields.firstName || this.originalValues.lastName !== this.loginFields.lastName;
      },
    },
  };
</script>

<style scoped>
</style>

<template>
  <div>
    <sub-page-header title="General"/>

    <loading-container v-bind:is-loading="isLoading">
      <div class="card">
        <div class="card-body">
          <label>First Name</label>
          <div class="input-group">
            <input id="first-name" aria-describedby="basic-addon1" class="form-control"
                   type="text" v-model="loginFields.firstName" name="firstName" v-validate="'required'"
                   data-vv-delay="500"/>
          </div>
          <p class="text-danger" v-show="errors.has('firstName')">{{ errors.first('firstName')}}</p>

          <label class="mt-2">Last Name</label>
          <div class="input-group">
            <input class="form-control" type="text" v-model="loginFields.lastName" name="lastName"
                   v-validate="'required'" data-vv-delay="500"/>
          </div>
          <p class="text-danger" v-show="errors.has('lastName')">{{ errors.first('lastName')}}</p>

          <div class="mt-2">
            <button class="btn btn-outline-primary" :disabled="errors.any() || !hasChangedValues()">
              Save <i class="fas fa-arrow-circle-right"/>
            </button>
          </div>
        </div>
      </div>
    </loading-container>
  </div>
</template>

<script>
  import { Validator } from 'vee-validate';
  import LoadingContainer from '../utils/LoadingContainer';
  import SubPageHeader from '../utils/pages/SubPageHeader';

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
    components: { SubPageHeader, LoadingContainer },
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

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

          <label class="mt-2">Nickname</label>
          <div class="input-group">
            <input class="form-control" type="text" v-model="loginFields.nickname" name="nickname"
                   v-validate="'required'" data-vv-delay="500"/>
          </div>
          <p class="text-danger" v-show="errors.has('nickname')">{{ errors.first('nickname')}}</p>

          <div class="mt-2">
            <button class="btn btn-outline-primary" @click="updateUserInfo" :disabled="errors.any() || !hasChangedValues()">
              Save
              <i :class="[isSaving ? 'fa fa-circle-notch fa-spin fa-3x-fa-fw' : 'fas fa-arrow-circle-right']"></i>
            </button>
          </div>
        </div>
      </div>
    </loading-container>
  </div>
</template>

<script>
  import { Validator } from 'vee-validate';
  import SettingsService from './SettingsService';
  import LoadingContainer from '../utils/LoadingContainer';
  import SubPageHeader from '../utils/pages/SubPageHeader';
  import ToastSupport from '../utils/ToastSupport';

  const dictionary = {
    en: {
      attributes: {
        nickname: 'Nickname',
        firstName: 'First Name',
        lastName: 'Last Name',
      },
    },
  };
  Validator.localize(dictionary);

  export default {
    name: 'GeneralSettings',
    mixins: [ToastSupport],
    components: { SubPageHeader, LoadingContainer },
    data() {
      return {
        isLoading: true,
        loginFields: {
          firstName: '',
          lastName: '',
          nickname: '',
        },
        originalValues: {
          firstName: '',
          lastName: '',
          nickname: '',
        },
        isSaving: false,
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
          this.loginFields.nickname = userInfo.nickname;
          this.originalValues.firstName = userInfo.first;
          this.originalValues.lastName = userInfo.last;
          this.originalValues.nickname = userInfo.nickname;
        }
        this.isLoading = false;
      },
      hasChangedValues() {
        let hasChangedValues = false;
        Object.keys(this.originalValues).forEach((index) => {
          if (this.originalValues[index] !== this.loginFields[index]) {
            hasChangedValues = true;
          }
        });
        return hasChangedValues;
        // return this.originalValues.firstName !== this.loginFields.firstName || this.originalValues.lastName !== this.loginFields.lastName;
      },
      updateUserInfo() {
        this.isSaving = true;
        const userInfo = Object.assign({}, this.$store.getters.userInfo, this.loginFields);
        SettingsService.saveUserInfo(userInfo).then(() => {
          this.$store.commit('storeUser', userInfo);
          this.successToast('Saved', 'Updated User Info Successful!');
        })
          .catch(() => {
            this.errorToast('Failure', 'Failed to Update User Info Settings!');
          })
          .finally(() => {
            this.isSaving = false;
          });
      },
    },
  };
</script>

<style scoped>
</style>

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
  <div>
    <sub-page-header title="Profile"/>

    <loading-container v-bind:is-loading="isLoading">
      <div class="card">
        <div class="card-body">
          <div v-if="!pkiAuthenticated">
            <label>First Name</label>
            <div class="input-group">
              <input id="first-name" aria-describedby="basic-addon1" class="form-control"
                     type="text" v-model="loginFields.first" name="first" v-validate="'required|maxFirstNameLength'"
                     data-vv-delay="500"/>
            </div>
            <p class="text-danger" v-show="errors.has('first')">{{ errors.first('first')}}</p>

            <label class="mt-2">Last Name</label>
            <div class="input-group">
              <input class="form-control" type="text" v-model="loginFields.last" name="last"
                     v-validate="'required|maxLastNameLength'" data-vv-delay="500"/>
            </div>
            <p class="text-danger" v-show="errors.has('last')">{{ errors.first('last')}}</p>
          </div>
          <label class="mt-2">Nickname</label>
          <div class="input-group">
            <input class="form-control" type="text" v-model="loginFields.nickname" name="nickname"
                   v-validate="'maxNicknameLength'" data-vv-delay="500"/>
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
        first: 'First Name',
        last: 'Last Name',
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
          first: '',
          last: '',
          nickname: '',
        },
        originalValues: {
          first: '',
          last: '',
          nickname: '',
        },
        isSaving: false,
        pkiAuthenticated: false,
      };
    },
    mounted() {
      this.loadData();
      this.pkiAuthenticated = this.$store.getters.isPkiAuthenticated;
    },
    methods: {
      loadData() {
        const { userInfo } = this.$store.getters;
        if (userInfo !== null) {
          this.loginFields.first = userInfo.first;
          this.loginFields.last = userInfo.last;
          this.loginFields.nickname = userInfo.nickname;
          this.setOriginalValues();
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
      },
      updateUserInfo() {
        this.isSaving = true;
        const userInfo = Object.assign({}, this.$store.getters.userInfo, this.loginFields);
        SettingsService.saveUserInfo(userInfo).then(() => {
          this.$store.commit('storeUser', userInfo);
          this.successToast('Saved', 'Updated User Info Successful!');
          this.setOriginalValues();
        })
          .catch(() => {
            this.errorToast('Failure', 'Failed to Update User Info Settings!');
          })
          .finally(() => {
            this.isSaving = false;
          });
      },
      setOriginalValues() {
        this.originalValues.first = this.loginFields.first;
        this.originalValues.last = this.loginFields.last;
        this.originalValues.nickname = this.loginFields.nickname;
      },
    },
  };
</script>

<style scoped>
</style>

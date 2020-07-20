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
    <sub-page-header title="System Settings"/>

    <div class="card">
      <div class="card-header">System</div>
      <ValidationObserver ref="observer" v-slot="{invalid}" slim>
        <div class="card-body">
          <div class="form-group">
            <label class="label">Public URL</label>
            <ValidationProvider rules="required" name="publicUrl" v-slot="{ errors }">
            <input class="form-control" type="text" v-model="publicUrl" name="publicUrl" data-vv-delay="500"/>
            <p class="text-danger" v-show="errors[0]">{{errors[0]}}</p>
            </ValidationProvider>
          </div>
          <div class="form-group">
            <label class="label">Password Token Expiration</label>
            <ValidationProvider rules="required|iso8601" name="resetTokenExpiration" v-slot="{ errors }">
            <input class="form-control" type="text" v-model="resetTokenExpiration" name="resetTokenExpiration" data-vv-delay="500"/>
            <small class="text-info">supports ISO 8601 time duration format, e.g., PT2H, PT30M, PT1H30M</small>
            <p class="text-danger" v-show="errors[0]">{{errors[0]}}</p>
            </ValidationProvider>
          </div>

          <p v-if="invalid && overallErrMsg" class="text-center text-danger">***{{ overallErrMsg }}***</p>
          <div>
            <button class="btn btn-outline-primary" type="button" v-on:click="saveSystemSettings" :disabled="invalid">
              Save
              <i :class="[isSaving ? 'fa fa-circle-notch fa-spin fa-3x-fa-fw' : 'fas fa-arrow-circle-right']"></i>
            </button>
          </div>
        </div>
      </ValidationObserver>
    </div>

  </div>
</template>

<script>
  import { Validator, ValidationProvider, ValidationObserver } from 'vee-validate';
  import SubPageHeader from '../utils/pages/SubPageHeader';
  import SettingsService from './SettingsService';
  import ToastSupport from '../utils/ToastSupport';
  import axios from "axios";

  const dictionary = {
    en: {
      attributes: {
        publicUrl: 'Public URL',
      },
    },
  };
  Validator.localize(dictionary);

  export default {
    name: 'SystemSettings',
    mixins: [ToastSupport],
    components: { SubPageHeader, ValidationObserver, ValidationProvider },
    data() {
      return {
        publicUrl: '',
        resetTokenExpiration: 'PT2H',
        isSaving: false,
        overallErrMsg: '',
      };
    },
    mounted() {
      this.loadSystemSettings();
    },
    methods: {
      saveSystemSettings() {
        this.$refs.observer.validate().then((res) => {
          if(res) {
            this.isSaving = true;
            SettingsService.saveSystemSettings({
              publicUrl: this.publicUrl,
              resetTokenExpiration: this.resetTokenExpiration
            }).then(() => {
              this.successToast('Saved', 'System Settings Successful!');
            }).catch(() => {
                this.errorToast('Failure', 'Failed to Save System Settings!');
            }).finally(() => {
                this.isSaving = false;
              });
          } else {
            this.overallErrMsg = 'Whoops, something is wrong with the information you entered. Please try again.';
          }
        });
      },
      loadSystemSettings() {
        SettingsService.loadSystemSettings().then((resp) => {
          if (resp) {
            this.publicUrl = resp.publicUrl;
          }
        });
      }
    },
  };

  const timePeriodRegex = /^PT(?=(?:0\.)?\d+[HMS])((?:0\.)?\d+H)?((?:0\.)?\d+M)?((?:0\.)?\d+S)?$/;
  Validator.extend('iso8601', {
    getMessage() {
      return 'Invalid ISO 8601 Time Duration';
    },
    validate(value) {
      if(value) {
        return value.match(timePeriodRegex) !== null;
      } else {
        return false;
      }
    },
  });

</script>

<style scoped>

</style>

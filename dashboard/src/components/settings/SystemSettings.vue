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
      <div class="card-body">
        <div class="form-group">
          <label class="label">Public URL</label>
          <input class="form-control" type="text" v-model="publicUrl" name="publicUrl"
                 v-validate="'required'" data-vv-delay="500"/>
          <p class="text-danger" v-show="errors.has('publicUrl')">{{
            errors.first('publicUrl')}}</p>
        </div>
        <div>
          <button class="btn btn-outline-primary" type="button" v-on:click="saveGeneralSettings" :disabled="errors.any()">
            Save
            <i :class="[isSaving ? 'fa fa-circle-notch fa-spin fa-3x-fa-fw' : 'fas fa-arrow-circle-right']"></i>
          </button>
        </div>
      </div>
    </div>

  </div>
</template>

<script>
  import { Validator } from 'vee-validate';
  import SubPageHeader from '../utils/pages/SubPageHeader';
  import SettingsService from './SettingsService';
  import ToastSupport from '../utils/ToastSupport';

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
    mixin: ToastSupport,
    components: { SubPageHeader },
    data() {
      return {
        publicUrl: '',
        isSaveing: false,
      };
    },
    methods: {
      saveGeneralSettings() {
        this.isSaving = true;
        SettingsService.saveSystemSettings({ publicUrl: this.publicUrl }).then(() => {
          this.successToast('Saved', 'System Settings Successful!');
        })
          .catch(() => {
            this.errorToast('Failure', 'Failed to Save System Settings!');
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

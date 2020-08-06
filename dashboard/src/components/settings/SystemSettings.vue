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
            <label class="label">Public URL <InlineHelp msg="Because it is possible for the SkillTree dashboard
            to be deployed behind a load balancer or proxy, it is necessary to configure the public url so that email
            based communications from the system can provide valid links back to the SkillTree dashboard."/></label>
            <ValidationProvider rules="required" name="publicUrl" v-slot="{ errors }">
              <input class="form-control" type="text" v-model="publicUrl" name="publicUrl" data-vv-delay="500"
                     data-cy="publicUrl"/>
              <p class="text-danger" v-show="errors[0]">{{errors[0]}}</p>
            </ValidationProvider>
          </div>
          <div class="form-group">
            <label class="label">Password Reset Token Expiration <InlineHelp msg="How long password reset tokens remain valid before they expire"/></label>
            <ValidationProvider rules="required|iso8601" name="resetTokenExpiration" v-slot="{ errors }">
              <input class="form-control" type="text" v-model="resetTokenExpiration" name="resetTokenExpiration" data-vv-delay="500"
                     data-cy="resetTokenExpiration"/>
              <small class="text-info">supports ISO 8601 time duration format, e.g., 2H, 30M, 1H30M, 1M42S, etc</small>
              <p class="text-danger" v-show="errors[0]">{{errors[0]}}</p>
            </ValidationProvider>
          </div>
          <div class="form-group">
            <label class="label">From Email <InlineHelp msg="The From email address used in all email originating from the SkillTree application"/></label>
            <ValidationProvider :rules="{email:{require_tld:false,allow_ip_domain:true}}" name="fromEmail" v-slot="{ errors }">
              <input class="form-control" type="text" v-model="fromEmail" name="fromEmail" data-vv-delay="500"
                     data-cy="fromEmail"/>
              <p class="text-danger" v-show="errors[0]">{{errors[0]}}</p>
            </ValidationProvider>
          </div>

          <div class="form-group">
            <label class="label">Custom Header <InlineHelp msg="HTML (and in-line css) to display as a header for the dashboard application"/></label>
            <ValidationProvider rules="noscript|max:3000" name="customHeader" v-slot="{ errors }">
              <textarea class="form-control" name="customHeader" data-cy="customHeader" rows="3" v-model="customHeader"/>
              <p class="text-danger" v-show="errors[0]" data-cy="customHeaderError">{{errors[0]}}</p>
            </ValidationProvider>
          </div>

          <div class="form-group">
            <label class="label">Custom Footer <InlineHelp msg="HTML (and in-line css) to display as a footer for the dashboard application"/></label>
            <ValidationProvider rules="noscript|max:3000" name="customFooter" v-slot="{ errors }">
              <textarea class="form-control" name="customFooter" data-cy="customFooter" v-model="customFooter" rows="3"/>
              <p class="text-danger" v-show="errors[0]" data-cy="customFooterError">{{errors[0]}}</p>
            </ValidationProvider>
          </div>

          <p v-if="invalid && overallErrMsg" class="text-center text-danger">***{{ overallErrMsg }}***</p>
          <div>
            <button class="btn btn-outline-primary" type="button" v-on:click="saveSystemSettings" :disabled="invalid"
                    data-cy="saveSystemSettings">
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
  import InlineHelp from '../utils/InlineHelp';

  const dictionary = {
    en: {
      attributes: {
        publicUrl: 'Public URL',
        resetTokenExpiration: 'Password Reset Token Expiration',
        fromEmail: 'From Email',
        customHeader: 'Custom Header',
        customFooter: 'Custom Footer',
      },
    },
  };
  Validator.localize(dictionary);

  export default {
    name: 'SystemSettings',
    mixins: [ToastSupport],
    components: {
      SubPageHeader,
      ValidationObserver,
      ValidationProvider,
      InlineHelp,
    },
    data() {
      return {
        publicUrl: '',
        resetTokenExpiration: '2H',
        fromEmail: 'no_reply@skilltree',
        isSaving: false,
        overallErrMsg: '',
        customHeader: '',
        customFooter: '',
      };
    },
    mounted() {
      this.loadSystemSettings();
    },
    methods: {
      saveSystemSettings() {
        this.$refs.observer.validate().then((res) => {
          if (res) {
            this.isSaving = true;

            const {
              publicUrl,
              fromEmail,
              customHeader,
              customFooter,
            } = this;
            let { resetTokenExpiration } = this;
            if (!resetTokenExpiration.toLowerCase().startsWith('pt')) {
              resetTokenExpiration = `PT${resetTokenExpiration}`;
            }

            SettingsService.saveSystemSettings({
              publicUrl,
              resetTokenExpiration,
              fromEmail,
              customHeader,
              customFooter,
            }).then(() => {
              this.successToast('Saved', 'System Settings Successful!');
              this.$store.dispatch('loadConfigState');
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
            if (resp.resetTokenExpiration) {
              this.resetTokenExpiration = resp.resetTokenExpiration.replace('PT', '');
            }
            if (this.fromEmail) {
              this.fromEmail = resp.fromEmail;
            }
            if (resp.customHeader) {
              this.customHeader = resp.customHeader;
            }
            if (resp.customFooter) {
              this.customFooter = resp.customFooter;
            }
          }
        });
      },
    },
  };

  const timePeriodRegex = /^(PT)?(?=(?:0\.)?\d+[HMS])((?:0\.)?\d+H)?((?:0\.)?\d+M)?((?:0\.)?\d+S)?$/;
  Validator.extend('iso8601', {
    getMessage() {
      return 'Invalid ISO 8601 Time Duration';
    },
    validate(value) {
      if (value) {
        return value.match(timePeriodRegex) !== null;
      }
      return false;
    },
  });

  const scriptRegex = /<[^>]*script/;
  Validator.extend('noscript', {
    getMessage() {
      return '<script> tags are not allowed';
    },
    validate(value) {
      if (value) {
        return value.match(scriptRegex) === null;
      }
      return false;
    },
  });

</script>

<style scoped>

</style>

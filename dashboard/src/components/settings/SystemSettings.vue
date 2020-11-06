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
      <ValidationObserver ref="observer" v-slot="{invalid, pristine}" slim>
        <div class="card-body">
          <div class="form-group">
            <label class="label" for="publicUrl">* Public URL <InlineHelp msg="Because it is possible for the SkillTree dashboard
            to be deployed behind a load balancer or proxy, it is necessary to configure the public url so that email
            based communications from the system can provide valid links back to the SkillTree dashboard."/></label>
            <ValidationProvider rules="required" name="Public URL" v-slot="{ errors }" :debounce=500>
              <input class="form-control" type="text" v-model="publicUrl" name="publicUrl"
                     data-cy="publicUrl" aria-required="true"
                    id="publicUrl"
                    :aria-invalid="errors && errors.length > 0"
                    aria-errormessage="publicUrlError" aria-describedby="publicUrlError"/>
              <p class="text-danger" v-show="errors[0]" id="publicUrlError">{{errors[0]}}</p>
            </ValidationProvider>
          </div>
          <div class="form-group">
            <label class="label" for="resetTokenExpiration">* Password Reset Token Expiration <InlineHelp msg="How long password reset tokens remain valid before they expire"/></label>
            <ValidationProvider rules="required|iso8601" name="Password Reset Token Expiration" v-slot="{ errors }" :debounce=500>
              <input class="form-control" type="text" v-model="resetTokenExpiration" name="resetTokenExpiration"
                     data-cy="resetTokenExpiration" aria-required="true"
                      id="resetTokenExpiration"
                      :aria-invalid="errors && errors.length > 0"
                      aria-errormessage="resetTokenExpirationError" aria-describedby="resetTokenExpirationError"/>
              <small class="text-info" id="resetTokenExpirationFormat">supports ISO 8601 time duration format, e.g., 2H, 30M, 1H30M, 1M42S, etc</small>
              <p class="text-danger" v-show="errors[0]" data-cy="resetTokenExpirationError" id="resetTokenExpirationError">{{errors[0]}}</p>
            </ValidationProvider>
          </div>
          <div class="form-group">
            <label class="label" for="fromEmail">From Email <InlineHelp msg="The From email address used in all email originating from the SkillTree application"/></label>
            <ValidationProvider :rules="{email:{require_tld:false,allow_ip_domain:true}}" name="From Email" v-slot="{ errors }" :debounce=500>
              <input class="form-control" type="text" v-model="fromEmail" name="fromEmail"
                     data-cy="fromEmail" id="fromEmail"
                    :aria-invalid="errors && errors.length  > 0"
                    aria-errormessage="fromEmailError" aria-describedby="fromEmailError"/>
              <p class="text-danger" v-show="errors[0]" data-cy="fromEmailError" id="fromEmailError">{{errors[0]}}</p>
            </ValidationProvider>
          </div>

          <div class="form-group">
            <label class="label" for="customHeader">Custom Header <InlineHelp msg="HTML (and in-line css) to display as a header for the dashboard application"/></label>
            <ValidationProvider rules="noscript|max:3000" name="Custom Header" v-slot="{ errors }">
              <textarea class="form-control" name="customHeader" data-cy="customHeader" rows="3" v-model="customHeader"
                  id="customHeader"
                  :aria-invalid="errors && errors.length > 0"
                  aria-errormessage="customHeaderError" aria-describedby="customHeaderError"/>
              <p class="text-danger" v-show="errors[0]" data-cy="customHeaderError" id="customHeaderError">{{errors[0]}}</p>
            </ValidationProvider>
          </div>

          <div class="form-group">
            <label class="label" for="customFooter">Custom Footer <InlineHelp msg="HTML (and in-line css) to display as a footer for the dashboard application"/></label>
            <ValidationProvider rules="noscript|max:3000" name="Custom Footer" v-slot="{ errors }">
              <textarea class="form-control" name="customFooter" data-cy="customFooter" v-model="customFooter" rows="3"
                        id="customFooter"
                        :aria-invalid="errors && errors.length > 0"
                        aria-errormessage="customFooterError" aria-describedby="customFooterError"/>
              <p class="text-danger" v-show="errors[0]" data-cy="customFooterError" id="customFooterError">{{errors[0]}}</p>
            </ValidationProvider>
          </div>

          <p v-if="invalid && overallErrMsg" class="text-center text-danger" role="alert">***{{ overallErrMsg }}***</p>
          <div>
            <button class="btn btn-outline-success" type="button" v-on:click="saveSystemSettings" :disabled="invalid || (pristine===true)"
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
  import { extend } from 'vee-validate';
  import { email, max } from 'vee-validate/dist/rules';
  import SubPageHeader from '../utils/pages/SubPageHeader';
  import SettingsService from './SettingsService';
  import ToastSupport from '../utils/ToastSupport';
  import InlineHelp from '../utils/InlineHelp';

  extend('email', email);
  extend('max', max);

  export default {
    name: 'SystemSettings',
    mixins: [ToastSupport],
    components: {
      SubPageHeader,
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
  extend('iso8601', {
    message: 'Invalid ISO 8601 Time Duration',
    validate(value) {
      if (value) {
        return value.match(timePeriodRegex) !== null;
      }
      return false;
    },
  });

  const scriptRegex = /<[^>]*script/;
  extend('noscript', {
    message: '<script> tags are not allowed',
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

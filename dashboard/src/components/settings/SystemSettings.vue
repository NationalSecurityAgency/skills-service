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
            <label class="label" for="resetTokenExpiration">* Token Expiration <InlineHelp target-id="resetTokenExpirationHelp" msg="How long password reset and email confirmation tokens remain valid before they expire"/></label>
            <ValidationProvider rules="required|iso8601" name="Token Expiration" v-slot="{ errors }" :debounce=500>
              <input class="form-control" type="text" v-model="resetTokenExpiration" name="resetTokenExpiration"
                     data-cy="resetTokenExpiration" aria-required="true"
                      id="resetTokenExpiration"
                      :aria-invalid="errors && errors.length > 0"
                      aria-errormessage="resetTokenExpirationError" aria-describedby="resetTokenExpirationError"/>
              <small class="text-info" id="resetTokenExpirationFormat">supports ISO 8601 time duration format, e.g., 2H, 30M, 1H30M, 1M42S, etc</small>
              <p role="alert" class="text-danger" v-show="errors[0]" data-cy="resetTokenExpirationError" id="resetTokenExpirationError">{{errors[0]}}</p>
            </ValidationProvider>
          </div>

          <div class="form-group">
            <label class="label" for="customHeader">Custom Header <InlineHelp target-id="customHeaderHelp" msg="HTML (and in-line css) to display as a header for the dashboard application"/></label>
            <ValidationProvider rules="noscript|max:3000" name="Custom Header" v-slot="{ errors }">
              <textarea class="form-control" name="customHeader" data-cy="customHeader" rows="3" v-model="customHeader"
                  id="customHeader"
                  :aria-invalid="errors && errors.length > 0"
                  aria-errormessage="customHeaderError" aria-describedby="customHeaderError"/>
              <p role="alert" class="text-danger" v-show="errors[0]" data-cy="customHeaderError" id="customHeaderError">{{errors[0]}}</p>
            </ValidationProvider>
          </div>

          <div class="form-group">
            <label class="label" for="customFooter">Custom Footer <InlineHelp target-id="customFooterHelp" msg="HTML (and in-line css) to display as a footer for the dashboard application"/></label>
            <ValidationProvider rules="noscript|max:3000" name="Custom Footer" v-slot="{ errors }">
              <textarea class="form-control" name="customFooter" data-cy="customFooter" v-model="customFooter" rows="3"
                        id="customFooter"
                        :aria-invalid="errors && errors.length > 0"
                        aria-errormessage="customFooterError" aria-describedby="customFooterError"/>
              <p role="alert" class="text-danger" v-show="errors[0]" data-cy="customFooterError" id="customFooterError">{{errors[0]}}</p>
            </ValidationProvider>
          </div>

          <div class="form-group">
            <label>User Agreement</label>
            <ValidationProvider rules="noscript" v-slot="{errors}"
                                name="User Agreement">
              <markdown-editor v-model="userAgreement"
                               @input="updateUserAgreement"
                               :resizable="true"
                               aria-errormessage="userAgreementError"
                               aria-describedby="userAgreementError"
                               :aria-invalid="errors && errors.length > 0">
              </markdown-editor>
              <small role="alert" id="userAgreementError" class="form-text text-danger mb-3" data-cy="userAgreement">{{ errors[0] }}</small>
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
  import MarkdownEditor from '../utils/MarkdownEditor';

  extend('email', email);
  extend('max', max);

  export default {
    name: 'SystemSettings',
    mixins: [ToastSupport],
    components: {
      SubPageHeader,
      InlineHelp,
      MarkdownEditor,
    },
    data() {
      return {
        resetTokenExpiration: '2H',
        isSaving: false,
        overallErrMsg: '',
        customHeader: '',
        customFooter: '',
        userAgreement: '',
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
              customHeader,
              customFooter,
              userAgreement,
            } = this;
            let { resetTokenExpiration } = this;
            if (!resetTokenExpiration.toLowerCase().startsWith('pt')) {
              resetTokenExpiration = `PT${resetTokenExpiration}`;
            }

            SettingsService.saveSystemSettings({
              resetTokenExpiration,
              customHeader,
              customFooter,
              userAgreement,
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
            if (resp.resetTokenExpiration) {
              this.resetTokenExpiration = resp.resetTokenExpiration.replace('PT', '');
            }

            if (resp.customHeader) {
              this.customHeader = resp.customHeader;
            }
            if (resp.customFooter) {
              this.customFooter = resp.customFooter;
            }
            if (resp.userAgreement) {
              this.userAgreement = resp.userAgreement;
            }
          }
          this.$nextTick(() => {
            this.$refs.observer.validate();
          });
        });
      },
      updateUserAgreement(event) {
        this.userAgreement = event;
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

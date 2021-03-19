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
  <ValidationObserver ref="templateSettingsObserver" v-slot="{invalid, pristine}" slim>
    <div>
      <div class="form-group">
        <b-tabs class="h-100">
          <b-tab active>
            <template slot="title">
              <div data-cy="htmlHeaderTitle">
                <span class="label" v-show="htmlHeaderRequired" :class="hHeaderTitleClass">* </span>
                <label class="label" for="htmlEmailHeader">
                  HTML Header <inline-help msg="HTML (and in-line css) to display as a header for outgoing emails"/>
                </label>
              </div>
            </template>
            <div class="mt-2 content-height">
              <ValidationProvider :rules="{'noscript':true,'max':3000, 'required':htmlHeaderRequired}" vid="htmlHeader" name="HTML Header" v-slot="{ errors }">
                <textarea class="form-control" name="htmlEmailHeader" data-cy="htmlEmailHeader" rows="3" v-model="htmlHeader"
                          id="htmlEmailHeader"
                          :aria-invalid="errors && errors.length > 0"
                          aria-errormessage="htmlEmailHeaderError" aria-describedby="htmlEmailHeaderError"/>
                  <p class="text-danger" v-show="errors[0]" data-cy="htmlEmailHeaderError" id="htmlEmailHeaderError">{{errors[0]}}</p>
                  <p class="text-danger"
                     v-show="!errors[0] && htmlHeaderRequired && !htmlHeader"
                     data-cy="htmlEmailHeaderRequired">HTML Header is required if Plaintext is configured</p>
              </ValidationProvider>
            </div>
          </b-tab>
          <b-tab>
            <template slot="title">
              <div data-cy="ptHeaderTitle">
                <span class="label" v-show="plaintextHeaderRequired" :class="pHeaderTitleClass">* </span>
                <label class="label" for="plaintextEmailHeader">
                  Plaintext Header <inline-help msg="Plaintext to display as a header for outgoing emails"/>
                </label>
              </div>
            </template>
            <div class="mt-2 content-height">
              <ValidationProvider :rules="{'noscript':true,'max':3000, 'required':plaintextHeaderRequired}" vid="plaintextHeader" name="Plaintext Header" v-slot="{ errors }">
                <textarea class="form-control" name="plaintextEmailHeader" data-cy="plaintextEmailHeader" rows="3" v-model="plainTextHeader"
                          id="plaintextEmailHeader"
                          :aria-invalid="errors && errors.length > 0"
                          aria-errormessage="plaintextEmailHeaderError" aria-describedby="plaintextEmailHeaderError"/>
                  <p class="text-danger" v-show="errors[0]" data-cy="plaintextEmailHeaderError" id="plaintextEmailHeaderError">{{errors[0]}}</p>
                  <p class="text-danger"
                     v-show="!errors[0] && plaintextHeaderRequired && !plainTextHeader"
                     data-cy="plaintextEmailHeaderRequired">Plaintext Header is required if HTML is configured</p>
              </ValidationProvider>
            </div>
          </b-tab>
        </b-tabs>
      </div>

      <div class="form-group">
        <b-tabs class="h-100">
          <b-tab active>
            <template slot="title">
              <div data-cy="htmlFooterTitle">
                <span class="label" v-if="htmlFooterRequired" :class="hFooterTitleClass">* </span>
                <label class="label" for="htmlEmailFooter">
                  HTML Footer <inline-help msg="HTML (and in-line css) to display as a footer for outgoing emails"/>
                </label>
              </div>
            </template>
            <div class="mt-2 content-height">
              <ValidationProvider :rules="{'noscript':true,'max':3000, 'required':htmlFooterRequired}" vid="htmlFooter" name="HTML Footer" v-slot="{ errors }">
              <textarea class="form-control" name="htmlEmailFooter" data-cy="htmlEmailFooter" v-model="htmlFooter" rows="3"
                        id="htmlEmailFooter"
                        :aria-invalid="errors && errors.length > 0"
                        aria-errormessage="htmlEmailFooterError" aria-describedby="htmlEmailFooterError"/>
                <p class="text-danger" v-show="errors[0]" data-cy="htmlEmailFooterError" id="htmlEmailFooterError">{{errors[0]}}</p>
                <p class="text-danger"
                   v-show="!errors[0] && htmlFooterRequired && !htmlFooter"
                   data-cy="htmlEmailFooterRequired">HTML Footer is required if Plaintext is configured</p>
              </ValidationProvider>
            </div>
          </b-tab>
          <b-tab>
            <template slot="title">
              <div data-cy="ptFooterTitle">
                <span class="label" v-if="plaintextFooterRequired" :class="pFooterTitleClass">* </span>
                <label class="label" for="plaintextEmailFooter">
                  Plaintext Footer <inline-help msg="Plaintext to display as a footer for outgoing emails"/>
                </label>
              </div>
            </template>
            <div class="mt-2 content-height">
              <ValidationProvider :rules="{'noscript':true,'max':3000, 'required':plaintextFooterRequired}" vid="plaintextFooter" name="Plaintext Footer" v-slot="{ errors }">
              <textarea class="form-control" name="plaintextEmailFooter" data-cy="plaintextEmailFooter" v-model="plainTextFooter" rows="3"
                        id="plaintextEmailFooter"
                        :aria-invalid="errors && errors.length > 0"
                        aria-errormessage="plaintextEmailFooterError" aria-describedby="plaintextEmailFooterError"/>
                <p class="text-danger" v-show="errors[0]" data-cy="plaintextEmailFooterError" id="plaintextEmailFooterError">{{errors[0]}}</p>
                <p class="text-danger"
                   v-show="!errors[0] && plaintextFooterRequired && !plainTextFooter"
                   data-cy="plaintextEmailFooterRequired">Plaintext Footer is required if HTML is configured</p>
              </ValidationProvider>
            </div>
          </b-tab>
        </b-tabs>
      </div>

      <div>
        <button class="btn btn-outline-success"
                type="button"
                v-on:click="saveTemplateSettings"
                :disabled="invalid || pristine || isSaving"
                data-cy="emailTemplateSettingsSave">
          Save
          <i :class="[isSaving ? 'fa fa-circle-notch fa-spin fa-3x-fa-fw' : 'fas fa-arrow-circle-right']"></i>
        </button>
      </div>
    </div>
  </ValidationObserver>
</template>

<script>
  import { extend } from 'vee-validate';
  import { max, required } from 'vee-validate/dist/rules';
  import SettingsService from './SettingsService';
  import ToastSupport from '../utils/ToastSupport';
  import InlineHelp from '../utils/InlineHelp';

  extend('max', max);
  extend('required', required);

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

  const settingGroup = 'GLOBAL.EMAIL';
  export default {
    name: 'EmailTemplateSettings',
    mixins: [ToastSupport],
    components: { InlineHelp },
    data() {
      return {
        htmlHeader: '',
        htmlFooter: '',
        plainTextHeader: '',
        plainTextFooter: '',
        isSaving: false,
      };
    },
    mounted() {
      this.loadEmailSettings();
    },
    computed: {
      htmlHeaderRequired() {
        return !!this.plainTextHeader && !this.htmlHeader;
      },
      plaintextHeaderRequired() {
        return !!this.htmlHeader && !this.plainTextHeader;
      },
      htmlFooterRequired() {
        return !!this.plainTextFooter && !this.htmlFooter;
      },
      plaintextFooterRequired() {
        return !!this.htmlFooter && !this.plainTextFooter;
      },
      hHeaderTitleClass() {
        return {
          'text-danger': this.htmlHeaderRequired && !this.htmlHeader,
        };
      },
      pHeaderTitleClass() {
        return {
          'text-danger': this.plaintextHeaderRequired && !this.plainTextHeader,
        };
      },
      hFooterTitleClass() {
        return {
          'text-danger': this.htmlFooterRequired && !this.htmlFooter,
        };
      },
      pFooterTitleClass() {
        return {
          'text-danger': this.plaintextFooterRequired && !this.plainTextFooter,
        };
      },
    },
    methods: {
      saveTemplateSettings() {
        this.$refs.templateSettingsObserver.validate().then((res) => {
          if (res) {
            this.isSaving = true;
            const settings = this.convertToSettings();
            SettingsService.saveGlobalSettings(settings).then((result) => {
              if (result) {
                if (result.success) {
                  this.successToast('Saved', 'Email Template Settings Saved!');
                }
              }
            })
              .catch(() => {
                this.errorToast('Failure', 'Failed to Save the Email Template Settings!');
              })
              .finally(() => {
                this.isSaving = false;
              });
          }
        });
      },
      convertToSettings() {
        return [{
                  settingGroup,
                  setting: 'email.htmlHeader',
                  value: this.htmlHeader,
                },
                {
                  settingGroup,
                  setting: 'email.htmlFooter',
                  value: this.htmlFooter,
                },
                {
                  settingGroup,
                  setting: 'email.plaintextHeader',
                  value: this.plainTextHeader,
                },
                {
                  settingGroup,
                  setting: 'email.plaintextFooter',
                  value: this.plainTextFooter,
                }];
      },
      convertFromSettings(settings) {
        this.htmlHeader = settings.find((setting) => setting.setting === 'email.htmlHeader')?.value;
        this.htmlFooter = settings.find((setting) => setting.setting === 'email.htmlFooter')?.value;
        this.plainTextHeader = settings.find((setting) => setting.setting === 'email.plaintextHeader')?.value;
        this.plainTextFooter = settings.find((setting) => setting.setting === 'email.plaintextFooter')?.value;
      },
      loadEmailSettings() {
        SettingsService.getGlobalSettings(settingGroup).then((response) => {
          this.convertFromSettings(response);
        });
      },
    },
  };
</script>

<style scoped>

</style>

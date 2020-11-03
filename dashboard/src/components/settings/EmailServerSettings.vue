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
  <ValidationObserver ref="observer" v-slot="{invalid, pristine}" slim>
    <div>
      <div class="form-group">
        <label class="label" for="emailHost">* Host</label>
        <ValidationProvider name="Host" :debounce=500 v-slot="{errors}" rules="required">
          <input class="form-control" type="text" v-model="emailInfo.host" name="host"
                 data-cy="hostInput" aria-required="true"
                  :aria-invalid="errors && errors.length > 0"
                  aria-errormessage="hostError"
                  id="emailHost"/>
          <p class="text-danger" v-show="errors[0]" data-cy="hostError" id="hostError">{{errors[0]}}</p>
        </ValidationProvider>
      </div>
      <div class="form-group">
        <label class="label" for="emailPort">* Port</label>
        <ValidationProvider name="Port" :debounce=500 v-slot="{errors}" rules="required|min_value:1|max_value:65535">
          <input class="form-control" type="text" v-model="emailInfo.port" name="port"
                 data-cy="portInput" aria-required="true"
                  :aria-invalid="errors && errors.length > 0"
                  aria-errormessage="portError"
                  id="emailPort"/>
          <p class="text-danger" v-show="errors[0]" data-cy="portError" id="portError">{{errors[0] }}</p>
        </ValidationProvider>
      </div>
      <div class="form-group">
        <label class="label" for="emailProtocol">* Protocol</label>
        <ValidationProvider name="Protocol" :debounce=500 v-slot="{errors}" rules="required">
          <input class="form-control" type="text" v-model="emailInfo.protocol" name="protocol"
                 data-cy="protocolInput" aria-required="true"
                  :aria-invalid="errors && errors.length > 0"
                  aria-errormessage="protocolError"
                  id="emailProtocol"/>
          <p class="text-danger" v-show="errors[0]" data-cy="protocolError" id="protocolError">{{
            errors[0] }}</p>
        </ValidationProvider>
      </div>
      <div class="form-group">
        <b-form-checkbox v-model="emailInfo.tlsEnabled" switch data-cy="tlsSwitch">
          {{ emailInfo.tlsEnabled ? 'TLS Enabled' : 'TLS Disabled' }}
        </b-form-checkbox>
      </div>
      <div class="form-group">
        <b-form-checkbox v-model="emailInfo.authEnabled" switch data-cy="authSwitch">
          {{ emailInfo.authEnabled ? 'Authentication Enabled' : 'Authentication Disabled' }}
        </b-form-checkbox>
      </div>
      <div id="auth-div" v-if="emailInfo.authEnabled">
        <div class="form-group">
          <label class="label" for="emailUsername">* Username</label>
          <ValidationProvider name="Username" :debounce=500 v-slot="{errors}" rules="required">
            <input class="form-control" type="text" v-model="emailInfo.username" name="username"
                   data-cy="emailUsername" aria-required="true"
                   :aria-invalid="errors && errors.length > 0"
                    aria-errormessage="emailUsernameError"
                    id="emailUsername"/>
            <p class="text-danger" v-show="errors[0]" data-cy="emailUsernameError" id="emailUsernameError">{{errors[0]}}</p>
          </ValidationProvider>
        </div>
        <div class="form-group">
          <label class="label" for="emailPassword">* Password</label>
          <ValidationProvider name="Password" :debounce=500 v-slot="{errors}" rules="required">
            <input class="form-control" type="text" v-model="emailInfo.password" name="password"
                   data-cy="emailPassword" aria-required="true"
                    :aria-invalid="errors && errors.length > 0"
                    aria-errormessage="emailPasswordError"
                    id="emailPassword"/>
            <p class="text-danger" v-show="errors[0]" data-cy="emailPasswordError" id="emailPasswordError">{{errors[0]}}</p>
          </ValidationProvider>
        </div>
      </div>

      <p v-if="connectionError" class="text-danger" data-cy="connectionError" role="alert">
        Connection to Email server failed due to: {{connectionError}}
      </p>

      <div>
        <button class="btn btn-outline-primary mr-1" type="button"
                v-on:click="testConnection" :disabled="invalid || missingRequiredValues() || isTesting || isSaving"
                data-cy="emailSettingsTest"
                aria-roledescription="test email server settings button">
          Test
          <i :class="testButtonClass"></i>
        </button>
        <button class="btn btn-outline-primary" type="button" v-on:click="saveEmailSettings" :disabled="invalid || pristine || missingRequiredValues() || isSaving || isTesting" data-cy="emailSettingsSave">
          Save
          <i :class="[isSaving ? 'fa fa-circle-notch fa-spin fa-3x-fa-fw' : 'fas fa-arrow-circle-right']"></i>
        </button>
      </div>
    </div>
  </ValidationObserver>
</template>

<script>
  import { extend } from 'vee-validate';
  // eslint-disable-next-line camelcase
  import { min_value, max_value } from 'vee-validate/dist/rules';
  import SettingsService from './SettingsService';
  import ToastSupport from '../utils/ToastSupport';

  extend('min_value', {
    // eslint-disable-next-line camelcase
    ...min_value,
    message: (fieldname, placeholders) => `${fieldname} must be ${placeholders.min} or greater`,
  });
  extend('max_value', {
    // eslint-disable-next-line camelcase
    ...max_value,
    message: (fieldname, placeholders) => `${fieldname} must be ${placeholders.max} or less`,
  });

  export default {
    name: 'EmailServerSettings',
    mixins: [ToastSupport],
    data() {
      return {
        emailInfo: {
          host: 'localhost',
          port: '25',
          protocol: 'smtp',
          username: '',
          password: '',
          authEnabled: false,
          tlsEnabled: false,
        },
        isTesting: false,
        isSaving: false,
        connectionError: '',
        testFailed: false,
        testSuccess: false,
      };
    },
    mounted() {
      this.loadEmailSettings();
    },
    computed: {
      testButtonClass() {
        if (this.isTesting) {
          return ['fa fa-circle-notch fa-spin fa-3x-fa-fw'];
        }

        if (this.testSuccess) {
          return ['fa fa-check-circle'];
        }

        if (this.testFailed) {
          return ['fa fa-times-circle'];
        }

        return ['fa fa-question-circle'];
      },
    },
    methods: {
      testConnection() {
        this.isTesting = true;
        SettingsService.testConnection(this.emailInfo).then((response) => {
          if (response) {
            this.successToast('Connection Status', 'Email Connection Successful!');
            this.testSuccess = true;
            this.testFailed = false;
          } else {
            this.errorToast('Connection Status', 'Email Connection Failed');
            this.testSuccess = false;
            this.testFailed = true;
          }
        })
          .catch(() => {
            this.errorToast('Failure', 'Failed to Test the Email Connection');
          })
          .finally(() => {
            this.isTesting = false;
          });
      },
      saveEmailSettings() {
        this.isSaving = true;
        if (this.emailInfo.authEnabled === false || this.emailInfo.authEnabled === 'false') {
          this.emailInfo.username = '';
          this.emailInfo.password = '';
        }
        SettingsService.saveEmailSettings(this.emailInfo).then((result) => {
          if (result) {
            if (result.success) {
              this.successToast('Saved', 'Email Connection Successful!');
            } else {
              this.connectionError = result.explanation;
            }
          }
        })
          .catch(() => {
            this.errorToast('Failure', 'Failed to Save the Connection Settings!');
          })
          .finally(() => {
            this.isSaving = false;
          });
      },
      loadEmailSettings() {
        SettingsService.loadEmailSettings().then((response) => {
          this.emailInfo = Object.assign(this.emailInfo, response);
        });
      },
      missingRequiredValues() {
        return !this.isAuthValid() || !this.emailInfo.host || !this.emailInfo.port || !this.emailInfo.protocol;
      },
      isAuthValid() {
        return !this.emailInfo.authEnabled || (this.emailInfo.username && this.emailInfo.password);
      },
    },
  };
</script>

<style scoped>

</style>

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
    <div class="form-group">
            <label class="label">Host</label>
      <input class="form-control" type="text" v-model="emailInfo.host" name="host"
             v-validate="'required'" data-vv-delay="500" data-cy="hostInput"/>
      <p class="text-danger" v-show="errors.has('host')">{{
        errors.first('host')}}</p>
    </div>
    <div class="form-group">
      <label class="label">Port</label>
      <input class="form-control" type="text" v-model="emailInfo.port" name="port"
             v-validate="'required|min_value:1|max_value:65535'" data-vv-delay="500" data-cy="portInput"/>
      <p class="text-danger" v-show="errors.has('port')">{{
        errors.first('port')}}</p>
    </div>
    <div class="form-group">
      <label class="label">Protocol</label>
      <input class="form-control" type="text" v-model="emailInfo.protocol" name="protocol"
             v-validate="'required'" data-vv-delay="500" data-cy="protocolInput"/>
      <p class="text-danger" v-show="errors.has('protocol')">{{
        errors.first('protocol')}}</p>
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
        <label class="label">Username</label>
        <input class="form-control" type="text" v-model="emailInfo.username" name="username"
               v-validate="'required'" data-vv-delay="500" data-cy="emailUsername"/>
        <p class="text-danger" v-show="errors.has('username')">{{
          errors.first('username')}}</p>
      </div>
      <div class="form-group">
        <label class="label">Password</label>
        <input class="form-control" type="text" v-model="emailInfo.password" name="password"
               v-validate="'required'" data-vv-delay="500" data-cy="emailPassword"/>
        <p class="text-danger" v-show="errors.has('password')">{{
          errors.first('password')}}</p>
      </div>
    </div>

    <p v-if="connectionError" class="text-danger" data-cy="connectionError">
      Connection to Email server failed due to: {{connectionError}}
    </p>

    <div>
      <button class="btn btn-outline-primary mr-1" type="button" v-on:click="testConnection" :disabled="errors.any() || missingRequiredValues() || isTesting || isSaving" data-cy="emailSettingsTest">
        Test
        <i :class="testButtonClass"></i>
      </button>
      <button class="btn btn-outline-primary" type="button" v-on:click="saveEmailSettings" :disabled="errors.any() || missingRequiredValues() || isSaving || isTesting" data-cy="emailSettingsSave">
        Save
        <i :class="[isSaving ? 'fa fa-circle-notch fa-spin fa-3x-fa-fw' : 'fas fa-arrow-circle-right']"></i>
      </button>
    </div>
  </div>
</template>

<script>
  import { Validator } from 'vee-validate';
  import SettingsService from './SettingsService';
  import ToastSupport from '../utils/ToastSupport';

  const dictionary = {
    en: {
      attributes: {
        host: 'Hostname',
        port: 'Port',
        protocol: 'Protocol',
        username: 'Username',
        password: 'Password',
      },
    },
  };
  Validator.localize(dictionary);

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

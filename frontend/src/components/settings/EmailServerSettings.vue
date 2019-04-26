<template>
  <div>
    <div class="box">
      <div class="columns">
        <div class="column">
          <div class="field">
            <label class="label">Host</label>
            <input class="input" type="text" v-model="emailInfo.host" name="host"
                   v-validate="'required'" data-vv-delay="500"/>
            <p class="help is-danger" v-show="errors.has('host')">{{
              errors.first('host')}}</p>
          </div>
          <div class="field">
            <label class="label">Port</label>
            <input class="input" type="text" v-model="emailInfo.port" name="port"
                   v-validate="'required|min_value:1|max_value:65535'" data-vv-delay="500"/>
            <p class="help is-danger" v-show="errors.has('port')">{{
              errors.first('port')}}</p>
          </div>
          <div class="field">
            <label class="label">Protocol</label>
            <input class="input" type="text" v-model="emailInfo.protocol" name="protocol"
                   v-validate="'required'" data-vv-delay="500"/>
            <p class="help is-danger" v-show="errors.has('protocol')">{{
              errors.first('protocol')}}</p>
          </div>
          <div class="field">
            <b-switch v-model="emailInfo.tlsEnabled">
              {{ emailInfo.tlsEnabled ? 'TLS Enabled' : 'TLS Disabled' }}
            </b-switch>
          </div>
          <div class="field">
            <b-switch v-model="emailInfo.authEnabled">
              {{ emailInfo.authEnabled ? 'Authentication Enabled' : 'Authentication Disabled' }}
            </b-switch>
          </div>
          <div id="auth-div" v-if="emailInfo.authEnabled">
            <div class="field">
              <label class="label">Username</label>
              <input class="input" type="text" v-model="emailInfo.username" name="username"
                     v-validate="'required'" data-vv-delay="500"/>
              <p class="help is-danger" v-show="errors.has('username')">{{
                errors.first('username')}}</p>
            </div>
            <div class="field">
              <label class="label">Password</label>
              <input class="input" type="text" v-model="emailInfo.password" name="password"
                     v-validate="'required'" data-vv-delay="500"/>
              <p class="help is-danger" v-show="errors.has('password')">{{
                errors.first('password')}}</p>
            </div>
          </div>
          <div class="columns" style="margin-top: 0px">
            <div class="control column is-half">
              <button id="test-button" class="button is-primary is-outlined"  style="float:right" v-on:click="testConnection" :disabled="errors.any() || missingRequiredValues()">
                <span id="test-button-text">Test</span>
                <span class="icon is-small">
                <i :class="[isTesting ? 'fa fa-circle-notch fa-spin fa-3x-fa-fw' : 'far fa-check-circle']"></i>
              </span>
              </button>
            </div>
          <!--</div>-->
          <!--<div class="column control-column">-->
            <div class="control column is-half">
              <button id="save-button" class="button is-primary is-outlined"  style="float:left" v-on:click="saveEmailSettings" :disabled="errors.any() || missingRequiredValues()">
                <span id="save-button-text">Save</span>
                <span class="icon is-small">
                <i :class="[isSaving ? 'fa fa-circle-notch fa-spin fa-3x-fa-fw' : 'fas fa-arrow-circle-right']"></i>
              </span>
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
  import { Validator } from 'vee-validate';
  import SettingsService from './SettingsService';
  import ToastHelper from '../utils/ToastHelper';

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
      };
    },
    methods: {
      testConnection() {
        this.isTesting = true;
        SettingsService.testConnection(this.emailInfo).then((response) => {
          if (response) {
            this.$toast.open(ToastHelper.defaultConf('Email Connection Successful!'));
          } else {
            this.$toast.open(ToastHelper.defaultConf('Email Connection Failed', true));
          }
        })
          .catch(() => {
            this.$toast.open(ToastHelper.defaultConf('Failed to Test the Email Connection', true));
          })
          .finally(() => {
            this.isTesting = false;
          });
      },
      saveEmailSettings() {
        this.isSaving = true;
        SettingsService.saveEmailSettings(this.emailInfo).then(() => {
          this.$toast.open(ToastHelper.defaultConf('Email Settings Saved!'));
        })
          .catch(() => {
            this.$toast.open(ToastHelper.defaultConf('Failed to Save the Connection Settings!', true));
          })
          .finally(() => {
            this.isSaving = false;
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

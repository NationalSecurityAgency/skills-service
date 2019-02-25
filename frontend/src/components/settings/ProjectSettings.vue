<template>
  <div>
    <div class="columns skills-underline-container">
      <div class="column">
        <span class="title is-3">Settings</span>
      </div>
    </div>
    <div class="skills-bordered-component">
      <b-loading :is-full-page="false" :active.sync="isLoading" :can-cancel="false"></b-loading>
      <div class="columns">
        <div class="column is-full">
           <span>
             <b-tooltip label="Change to true to calculate levels based on explicit point values instead of percentages."
                        position="is-top" size="is-small" animanted="true" type="is-light" multilined>
            <span><i class="fas fa-question-circle"></i></span>
            </b-tooltip>
            Use Points For Levels:
            <b-switch v-model="levelPointsSetting.value" v-on:input="settingChanged">
              {{ levelPointsSetting.value }}
            </b-switch>
          </span>
        </div>
      </div>

      <div class="columns skills-pad-top-1-rem">
        <div class="column">
          <a class="button is-outlined is-success" v-on:click="save"
             :disabled="errors.any()">
            <span>Save</span>
            <span class="icon is-small">
              <i class="fas fa-arrow-circle-right"/>
              </span>
          </a>
          <b-tooltip v-if="dirty" label="Settings have been changed, don't forget to save"
                     position="is-right" animanted="true" type="is-light">
            <span><i class="mi mi-warning dirty-warning"></i></span>
          </b-tooltip>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
  import SettingService from './SettingsService';
  import ToastHelper from '../utils/ToastHelper';

  const initialSettingValue = { value: 'false', setting: 'level.points.enabled' };
  export default {
    name: 'ProjectSettings',
    props: ['projectId'],
    data() {
      return {
        isLoading: true,
        levelPointsSetting: Object.assign({ projectId: this.projectId }, initialSettingValue),
        lastLoadedValue: null,
        serverErrors: [],
        dirty: false,
      };
    },
    mounted() {
      this.loadSettings();
    },
    methods: {
      settingChanged(value) {
        const valueStr = `${value}`;
        if (valueStr !== this.lastLoadedValue.value) {
          this.dirty = true;
        } else {
          this.dirty = false;
        }
      },
      loadSettings() {
        SettingService.getSetting(this.projectId, this.levelPointsSetting.setting)
          .then((response) => {
            this.isLoading = false;
            if (response) {
              this.levelPointsSetting = response;
            } else {
              this.levelPointsSetting = Object.assign({ projectId: this.projectId }, initialSettingValue);
            }
            this.lastLoadedValue = Object.assign({}, this.levelPointsSetting);
          })
          .catch((e) => {
            this.serverErrors.push(e);
        });
      },
      save() {
        const self = this;
        this.isLoading = true;
        SettingService.saveSetting(this.projectId, this.levelPointsSetting)
          .then((res) => {
            this.isLoading = false;
            this.dirty = false;
            this.levelPointsSetting = res;
            this.$toast.open(ToastHelper.defaultConf('Successfully saved settings!', false));
          })
          .catch((e) => {
            this.dirty = false;
            self.loadSettings();
            this.isLoading = false;
            this.serverErrors.push(e);
            if (e && e.response) {
              if (e.response.data && e.response.data.errorMsg) {
                this.$toast.open(ToastHelper.defaultConf(`Error saving settings: ${e.response.data.errorMsg}`, true));
              } else {
                this.$toast.open(ToastHelper.defaultConf(`Error saving settings: ${e.response.status} - ${e.response.statusText}`, true));
              }
            }
        });
      },
    },
  };

</script>

<style scoped>
  .dirty-warning {
    color: #E6B34F;
  }

</style>

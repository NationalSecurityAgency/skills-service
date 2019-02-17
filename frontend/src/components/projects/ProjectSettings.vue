<template>
  <div>
    <div class="columns skills-underline-container">
      <div class="column">
        <span class="title is-3">Settings</span>
      </div>
      <div class="column has-text-right">
        <a class="button is-outlined is-info" v-on:click="save"
           :disabled="errors.any()">
          <span>Save</span>
          <span class="icon is-small">
            <i class="fas fa-arrow-circle-right"/>
            </span>
        </a>
      </div>
    </div>
    <b-loading :is-full-page="false" :active.sync="isLoading" :can-cancel="false"></b-loading>
    <div class="columns">
      <div class="column is-full">
         <span>
           <b-tooltip label="Change to true to calculate levels based on explicit point values instead of percentages."
                      position="is-top" size="is-small" animanted="true" type="is-light" multilined>
          <span><i class="fas fa-question-circle"></i></span>
          </b-tooltip>
          Use Points For Levels:
          <b-switch v-model="levelPointsSetting.value">
            {{ levelPointsSetting.value }}
          </b-switch>
        </span>
      </div>
    </div>
  </div>
</template>

<script>
  import SettingService from '../settings/SettingsService';

  export default {
    name: 'ProjectSettings',
    props: ['projectId'],
    data() {
      return {
        isLoading: true,
        levelPointsSetting: { value: false, setting: 'level.points.enabled', projectId: this.projectId },
        serverErrors: [],
      };
    },
    mounted() {
      this.loadSettings();
    },
    methods: {
      loadSettings() {
        SettingService.getSetting(this.projectId, this.levelPointsSetting.setting)
          .then((response) => {
            this.isLoading = false;
            if (response) {
              this.levelPointsSetting = response;
            }
          })
          .catch((e) => {
            this.serverErrors.push(e);
        });
      },
      save() {
        this.isLoading = true;
        SettingService.saveSetting(this.projectId, this.levelPointsSetting)
          .then((res) => {
            this.isLoading = false;
            this.levelPointsSetting = res;
          })
          .catch((e) => {
            this.isLoading = false;
            this.serverErrors.push(e);
            throw e;
        });
      },
    },
  };

</script>

<style scoped>

</style>

<template>
  <div>
    <sub-page-header title="Project Settings"/>
    <simple-card>
      <loading-container :is-loading="isLoading">
        <div class="row">
          <div class="col col-md-3 text-secondary">
            Use Points For Levels:
            <inline-help
              msg="Change to true to calculate levels based on explicit point values instead of percentages."/>
          </div>
          <div class="col">
            <b-form-checkbox v-model="levelPointsSetting.value" v-on:input="settingChanged" name="check-button" switch>
              {{ levelPointsSetting.value }}
            </b-form-checkbox>
          </div>
        </div>

        <hr/>

        <div class="row">
          <div class="col">
            <b-button variant="outline-info" @click="save" :disabled="!dirty || errors.any()">
              Save <i class="fas fa-arrow-circle-right"/>
            </b-button>

            <span v-if="dirty" class="text-warning ml-2">
          <i class="fa fa-exclamation-circle"
             v-b-tooltip.hover="'Settings have been changed, don not forget to save'"/> Unsaved Changes
        </span>
          </div>
        </div>
      </loading-container>
    </simple-card>

  </div>
</template>

<script>
  import SettingService from './SettingsService';
  import SubPageHeader from '../utils/pages/SubPageHeader';
  import SimpleCard from '../utils/cards/SimpleCard';
  import InlineHelp from '../utils/InlineHelp';
  import LoadingContainer from '../utils/LoadingContainer';
  import ToastSupport from '../utils/ToastSupport';

  const initialSettingValue = { value: 'false', setting: 'level.points.enabled' };
  export default {
    name: 'ProjectSettings',
    mixins: [ToastSupport],
    components: {
      LoadingContainer,
      InlineHelp,
      SimpleCard,
      SubPageHeader,
    },
    props: ['projectId'],
    data() {
      return {
        isLoading: true,
        levelPointsSetting: Object.assign({ projectId: this.projectId }, initialSettingValue),
        lastLoadedValue: null,
        dirty: false,
      };
    },
    mounted() {
      this.loadSettings();
    },
    methods: {
      settingChanged(value) {
        const valueStr = `${value}`;
        if (valueStr !== `${this.lastLoadedValue.value}`) {
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
          });
      },
      save() {
        this.isLoading = true;
        SettingService.saveSetting(this.projectId, this.levelPointsSetting)
          .then((res) => {
            this.dirty = false;
            this.lastLoadedValue = Object.assign({}, this.levelPointsSetting);
            this.levelPointsSetting = res;
            this.successToast('Settings Updated', 'Successfully saved settings!');
          })
          .catch((e) => {
            if (e.response.data && e.response.data.errorCode && e.response.data.errorCode === 'InsufficientPointsToConvertLevels') {
              this.errorToast('Setting Not Saved!', e.response.data.message);
            } else {
              const errorMessage = (e.response && e.response.data && e.response.data.message) ? e.response.data.message : undefined;
              this.$router.push({ name: 'ErrorPage', query: { errorMessage } });
            }
          })
          .finally(() => {
            this.isLoading = false;
          });
      },
    },
  };

</script>

<style scoped>
</style>

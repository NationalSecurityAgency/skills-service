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
    <sub-page-header title="Preferences"/>

    <loading-container v-bind:is-loading="isLoading">
        <div class="card">
          <div class="card-body">
            <div class="">
              <b-form-group label-for="home-page-pref">
                <template v-slot:label>
                  <i class="fas fa-home" aria-hidden="true"></i> Default Home Page:
                  <inline-help
                    msg="Select which page you would to be displayed when first visiting the SkillTree dashboard."/>
                </template>
                <b-form-radio-group
                  id="home-page-pref"
                  class="pl-2"
                  data-cy="landingPageSelector"
                  v-on:input="homePagePrefChanged"
                  v-model="settings.homePage.value"
                  :options="[{ text: 'Progress and Rankings', value: 'progress'}, {text: 'Project Admin', value: 'admin'}]"
                  stacked
                ></b-form-radio-group>
              </b-form-group>
            </div>
            <div >
              <i class="fas fa-users-slash" aria-hidden="true"></i> <span id="rankAndLeaderboardOptOutLabel">Rank and Leaderboard Opt-Out:</span>
              <inline-help
                msg="Change to true and you will not be shown on the Leaderboard or assigned a rank"/>
              <b-form-checkbox v-model="settings.rankAndLeaderboardOptOut.value"
                               name="check-button"
                               v-on:input="rankAndLeaderboardOptOutPrefChanged"
                               aria-labelledby="rankAndLeaderboardOptOutLabel"
                               data-cy="rankAndLeaderboardOptOutSwitch"
                               class="ml-3"
                               inline switch>
                {{ settings.rankAndLeaderboardOptOut.value ? 'Yes' : 'No' }}
              </b-form-checkbox>
            </div>

            <hr/>
            <p v-if="errMsg" class="text-center text-danger mt-3" role="alert">***{{ errMsg }}***</p>

            <div class="row">
              <div class="col">
                <b-button variant="outline-success" @click="save" :disabled="!isDirty" data-cy="userPrefsSettingsSave">
                  Save <i class="fas fa-arrow-circle-right"/>
                </b-button>

                <span v-if="isDirty" class="text-warning ml-2" data-cy="unsavedChangesAlert">
              <i class="fa fa-exclamation-circle"
                 v-b-tooltip.hover="'Settings have been changed, do not forget to save'"/> Unsaved Changes
            </span>
                <span v-if="!isDirty && showSavedMsg" class="text-success ml-2" data-cy="settingsSavedAlert">
              <i class="fa fa-check" />
              Settings Updated!
            </span>
              </div>
            </div>

          </div>
        </div>
    </loading-container>
  </div>
</template>

<script>
  import SettingsService from './SettingsService';
  import LoadingContainer from '../utils/LoadingContainer';
  import SubPageHeader from '../utils/pages/SubPageHeader';
  import ToastSupport from '../utils/ToastSupport';
  import InlineHelp from '../utils/InlineHelp';

  export default {
    name: 'Preferences',
    mixins: [ToastSupport],
    components: { SubPageHeader, LoadingContainer, InlineHelp },
    data() {
      return {
        isLoading: true,
        settings: {
          homePage: {
            settingGroup: 'user.prefs',
            value: 'progress',
            setting: 'home_page',
            lastLoadedValue: '',
            dirty: false,
          },
          rankAndLeaderboardOptOut: {
            settingGroup: 'user.prefs',
            value: false,
            setting: 'rank_and_leaderboard_optOut',
            lastLoadedValue: '',
            dirty: false,
          },
        },
        errMsg: null,
        showSavedMsg: false,
      };
    },
    mounted() {
      this.loadSettings();
    },
    methods: {
      homePagePrefChanged(value) {
        this.settings.homePage.dirty = `${value}` !== `${this.settings.homePage.lastLoadedValue}`;
      },
      rankAndLeaderboardOptOutPrefChanged(value) {
        this.settings.rankAndLeaderboardOptOut.dirty = `${value}` !== `${this.settings.rankAndLeaderboardOptOut.lastLoadedValue}`;
      },
      loadSettings() {
        SettingsService.getUserSettings()
          .then((response) => {
            if (response) {
              const entries = Object.entries(this.settings);
              entries.forEach((entry) => {
                const [key, value] = entry;
                const found = response.find((item) => item.setting === value.setting);
                if (found) {
                  this.settings[key] = { dirty: false, lastLoadedValue: found.value, ...found };
                }
              });
            }
          })
          .finally(() => {
            this.isLoading = false;
          });
      },
      save() {
        const dirtyChanges = Object.values(this.settings).filter((item) => item.dirty);
        if (dirtyChanges) {
          this.isLoading = true;
          SettingsService.checkUserSettingsValidity(dirtyChanges)
            .then((res) => {
              if (res.valid) {
                this.saveUserSettings(dirtyChanges);
              } else {
                this.errMsg = res.explanation;
                this.isLoading = false;
              }
            });
        }
      },
      saveUserSettings(dirtyChanges) {
        SettingsService.saveUserSettings(dirtyChanges)
          .then(() => {
            this.showSavedMsg = true;
            setTimeout(() => { this.showSavedMsg = false; }, 4000);
            const entries = Object.entries(this.settings);
            entries.forEach((entry) => {
              const [key, value] = entry;
              this.settings[key] = Object.assign(value, { dirty: false, lastLoadedValue: value.value });

              if (key === 'homePage') {
                const userInfo = { ...this.$store.getters.userInfo, landingPage: value.value };
                this.$store.commit('storeUser', userInfo);
              }
            });
          })
          .finally(() => {
            this.isLoading = false;
          });
      },
    },
    computed: {
      isDirty() {
        const foundDirty = Object.values(this.settings).find((item) => item.dirty);
        return foundDirty;
      },
    },
  };
</script>

<style scoped>
</style>

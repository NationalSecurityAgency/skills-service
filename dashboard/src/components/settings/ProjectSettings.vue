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
    <sub-page-header title="Project Settings"/>
    <simple-card>
      <loading-container :is-loading="isLoading">
        <div class="row">
          <div class="col col-md-3 text-secondary" id="productionModeEnabledLabel">
            Production Mode:
            <inline-help
              msg="Change to true for this project to be visible in the 'Progress and Ranking' page for all SkillTree users."/>
          </div>
          <div class="col">
            <b-form-checkbox v-model="settings.productionModeEnabled.value"
                             name="check-button"
                             v-on:input="productionModeEnabledChanged"
                             aria-labelledby="productionModeEnabledLabel"
                             data-cy="productionModeEnabledSwitch"
                             switch>
              {{ settings.productionModeEnabled.value }}
            </b-form-checkbox>
          </div>
        </div>

        <div class="row mt-3">
          <div class="col col-md-3 text-secondary" id="pointsForLevelsLabel">
            Use Points For Levels:
            <inline-help
              msg="Change to true to calculate levels based on explicit point values instead of percentages."/>
          </div>
          <div class="col">
            <b-form-checkbox v-model="settings.levelPointsEnabled.value"
                             name="check-button"
                             v-on:input="levelPointsEnabledChanged"
                             aria-labelledby="pointsForLevelsLabel"
                             data-cy="usePointsForLevelsSwitch"
                             switch>
              {{ settings.levelPointsEnabled.value }}
            </b-form-checkbox>
          </div>
        </div>

        <div class="row mt-3">
          <div class="col col-md-3 text-secondary" id="rootHelpUrlLabel">
            Root Help Url:
            <inline-help
              msg="Optional root for Skills' 'Help Url' parameter. When configured 'Help Url' can use relative path to this root."/>
          </div>
          <div class="col">
            <b-form-input v-model="settings.helpUrlHost.value" placeholder="http://www.HelpArticlesHost.com"
                          v-on:input="helpUrlHostChanged" aria-labelledby="rootHelpUrlLabel"></b-form-input>
          </div>
        </div>

        <div class="row mt-3">
          <div class="col col-md-3 text-secondary" id="selfReportLabel">
            Self Report Default:
          </div>
          <div class="col">
            <b-form-checkbox v-model="selfReport.enabled"
                             name="check-button"
                             v-on:input="selfReportingControl"
                             aria-labelledby="pointsForLevelsLabel"
                             data-cy="selfReportSwitch"
                             switch>
              Disable/Enable <inline-help
              msg="Will serve as a default when creating new skills."/>
            </b-form-checkbox>

            <b-card class="mt-1">
              <b-form-group  label="Approval Type:" v-slot="{ ariaDescribedby }">
                <b-form-radio-group
                  id="self-reporting-type"
                  v-model="selfReport.selected"
                  :options="selfReport.options"
                  v-on:input="selfReportingTypeChanged"
                  :aria-describedby="ariaDescribedby"
                  name="Self Reporting Options"
                  data-cy="selfReportTypeSelector"
                  stacked
                ></b-form-radio-group>
              </b-form-group>
            </b-card>
          </div>
        </div>

        <div class="row mt-3">
          <div class="col col-md-3 text-secondary" id="rankAndLeaderboardOptOutLabel">
            Rank Opt-Out for ALL Admins:
            <inline-help
              msg="Change to true and all of the project's admins will not be shown on the Leaderboard or assigned a rank"/>
          </div>
          <div class="col">
            <b-form-checkbox v-model="settings.rankAndLeaderboardOptOut.value"
                             name="check-button"
                             v-on:input="rankAndLeaderboardOptOutChanged"
                             aria-labelledby="rankAndLeaderboardOptOutLabel"
                             data-cy="rankAndLeaderboardOptOutSwitch"
                             switch>
              {{ settings.rankAndLeaderboardOptOut.value }}
            </b-form-checkbox>
          </div>
        </div>

        <hr/>

        <p v-if="errMsg" class="text-center text-danger mt-3" role="alert">***{{ errMsg }}***</p>

        <div class="row">
          <div class="col">
            <b-button variant="outline-success" @click="save" :disabled="!isDirty" data-cy="saveSettingsBtn">
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
      </loading-container>
    </simple-card>

  </div>
</template>

<script>
  import { SkillsReporter } from '@skilltree/skills-client-vue';
  import SettingService from './SettingsService';
  import SubPageHeader from '../utils/pages/SubPageHeader';
  import SimpleCard from '../utils/cards/SimpleCard';
  import InlineHelp from '../utils/InlineHelp';
  import LoadingContainer from '../utils/LoadingContainer';
  import ToastSupport from '../utils/ToastSupport';

  export default {
    name: 'ProjectSettings',
    mixins: [ToastSupport],
    components: {
      LoadingContainer,
      InlineHelp,
      SimpleCard,
      SubPageHeader,
    },
    data() {
      return {
        isLoading: true,
        selfReport: {
          enabled: false,
          selected: 'Approval',
          options: [
            { text: 'Approval Queue (reviewed by project admins first)', value: 'Approval', disabled: true },
            { text: 'Honor System (applied right away)', value: 'HonorSystem', disabled: true },
          ],
        },
        settings: {
          productionModeEnabled: {
            value: 'false',
            setting: 'production.mode.enabled',
            lastLoadedValue: 'false',
            dirty: false,
            projectId: this.$route.params.projectId,
          },
          levelPointsEnabled: {
            value: 'false',
            setting: 'level.points.enabled',
            lastLoadedValue: 'false',
            dirty: false,
            projectId: this.$route.params.projectId,
          },
          selfReportType: {
            value: '',
            setting: 'selfReport.type',
            lastLoadedValue: '',
            dirty: false,
            projectId: this.$route.params.projectId,
          },
          helpUrlHost: {
            value: '',
            setting: 'help.url.root',
            lastLoadedValue: '',
            dirty: false,
            projectId: this.$route.params.projectId,
          },
          rankAndLeaderboardOptOut: {
            value: false,
            setting: 'project-admins_rank_and_leaderboard_optOut',
            lastLoadedValue: false,
            dirty: false,
            projectId: this.$route.params.projectId,
          },
        },
        errMsg: null,
        showSavedMsg: false,
      };
    },
    mounted() {
      this.loadSettings();
    },
    computed: {
      isDirty() {
        const foundDirty = Object.values(this.settings).find((item) => item.dirty);
        return foundDirty;
      },
    },
    methods: {
      updateApprovalType(disabled) {
        this.selfReport.options = this.selfReport.options.map((item) => {
          const copy = { ...item };
          copy.disabled = disabled;
          return copy;
        });
      },
      selfReportingControl(checked) {
        this.updateApprovalType(!checked);
        if (checked) {
          this.settings.selfReportType.value = this.selfReport.selected;
        } else {
          this.settings.selfReportType.value = '';
        }
        this.settings.selfReportType.dirty = `${this.settings.selfReportType.value}` !== `${this.settings.selfReportType.lastLoadedValue}`;
      },
      productionModeEnabledChanged(value) {
        this.settings.productionModeEnabled.dirty = `${value}` !== `${this.settings.productionModeEnabled.lastLoadedValue}`;
      },
      rankAndLeaderboardOptOutChanged(value) {
        this.settings.rankAndLeaderboardOptOut.dirty = `${value}` !== `${this.settings.rankAndLeaderboardOptOut.lastLoadedValue}`;
      },
      selfReportingTypeChanged(value) {
        this.settings.selfReportType.value = value;
        this.settings.selfReportType.dirty = `${this.settings.selfReportType.value}` !== `${this.settings.selfReportType.lastLoadedValue}`;
      },
      levelPointsEnabledChanged(value) {
        this.settings.levelPointsEnabled.dirty = `${value}` !== `${this.settings.levelPointsEnabled.lastLoadedValue}`;
      },
      helpUrlHostChanged(value) {
        this.settings.helpUrlHost.dirty = `${value}` !== `${this.settings.helpUrlHost.lastLoadedValue}`;
      },
      loadSettings() {
        SettingService.getSettingsForProject(this.$route.params.projectId)
          .then((response) => {
            if (response) {
              const entries = Object.entries(this.settings);
              entries.forEach((entry) => {
                const [key, value] = entry;
                const found = response.find((item) => item.setting === value.setting);
                if (found) {
                  this.settings[key] = { dirty: false, lastLoadedValue: found.value, ...found };

                  // special handling of self reporting as it's not just a simple key-value prop control
                  if (found.setting === this.settings.selfReportType.setting) {
                    this.selfReport.enabled = true;
                    this.selfReport.selected = this.settings.selfReportType.value;
                    this.updateApprovalType(false);
                  }
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
          SettingService.checkSettingsValidity(this.$route.params.projectId, dirtyChanges)
            .then((res) => {
              if (res.valid) {
                this.saveSettings(dirtyChanges);
              } else {
                this.errMsg = res.explanation;
                this.isLoading = false;
              }
            });
        }
      },
      saveSettings(dirtyChanges) {
        SettingService.saveSettings(this.$route.params.projectId, dirtyChanges)
          .then(() => {
            this.showSavedMsg = true;
            setTimeout(() => { this.showSavedMsg = false; }, 4000);
            const entries = Object.entries(this.settings);
            entries.forEach((entry) => {
              const [key, value] = entry;
              this.settings[key] = Object.assign(value, { dirty: false, lastLoadedValue: value.value });
              if (value.setting === this.settings.helpUrlHost.setting && value.value && value.value.length > 0) {
                SkillsReporter.reportSkill('ConfigureProjectRootHelpUrl');
              }
            });
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

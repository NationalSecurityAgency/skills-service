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
  <ValidationObserver ref="observer" v-slot="{ invalid, handleSubmit }" slim>
  <div>
    <sub-page-header title="Project Settings"/>
    <simple-card>
      <loading-container :is-loading="isLoading">
        <div class="row" data-cy="projectVisibility">
          <div class="col col-md-3 text-secondary" id="projectVisibilityLabel">
            Project Visibility: <inline-help target-id="projectVisibilityHelp" :msg="projectVisibilityHelpMsg" />
          </div>
          <div class="col">
            <b-form-select v-model="settings.projectVisibility.value"
                           :options="projectVisibilityOptions"
                           @input="projectVisibilityChanged"
                           aria-labelledby="projectVisibilityLabel"
                           data-cy="projectVisibilitySelector" required/>
          </div>
        </div>

          <div class="row mt-3">
            <div class="col col-md-3 text-secondary" id="pointsForLevelsLabel">
              Use Points For Levels:
              <inline-help
                target-id="pointsForLevelsHelp"
                msg="Change to true to calculate levels based on explicit point values instead of percentages."/>
            </div>
            <div class="col">
              <b-form-checkbox v-model="settings.levelPointsEnabled.value"
                               name="check-button"
                               v-on:input="levelPointsEnabledChanged"
                               aria-labelledby="pointsForLevelsLabel"
                               data-cy="usePointsForLevelsSwitch"
                               switch>
                {{ usePointsForLevelsLabel }}
              </b-form-checkbox>
            </div>
          </div>

          <ValidationProvider rules="root_help_url|customUrlValidator" v-slot="{errors}"
                              name="Root Help Url">
            <div class="row mt-3">
              <div class="col col-md-3 text-secondary" id="rootHelpUrlLabel">
                Root Help Url:
                <inline-help
                  target-id="rootHelpUrlHelp"
                  msg="Optional root for Skills' 'Help Url' parameter. When configured 'Help Url' can use relative path to this root."/>
              </div>
              <div class="col">
                <b-form-input v-model="settings.helpUrlHost.value"
                              placeholder="http://www.HelpArticlesHost.com"
                              data-cy="rootHelpUrlInput"
                              v-on:input="helpUrlHostChanged"
                              aria-describedby="rootHelpUrlError"
                              aria-errormessage="rootHelpUrlError"
                              aria-labelledby="rootHelpUrlLabel">
                </b-form-input>
              </div>
            </div>
            <div v-if="errors && errors.length > 0" class="row">
              <div class="col">
                <small role="alert" class="form-text text-danger" id="rootHelpUrlError"
                       data-cy="rootHelpUrlError">{{ errors[0] }}</small>
              </div>
            </div>
          </ValidationProvider>

          <div class="row mt-3">
            <div class="col col-md-3 text-secondary" id="selfReportLabel">
              Self Report Default:
              <inline-help
                target-id="selfReportSwitchHelp"
                msg="Will serve as a default when creating new skills."/>
            </div>
            <div class="col">
              <b-form-checkbox v-model="selfReport.enabled"
                               name="check-button"
                               v-on:input="selfReportingControl"
                               aria-labelledby="selfReportLabel"
                               data-cy="selfReportSwitch"
                               switch>
                {{ selfReportingEnabledLabel }}
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
                  >
                    <template #first>
                      <div class="row m-0">
                        <b-form-radio class="mr-2" value="Approval" :disabled="!selfReport.enabled">Approval Queue (reviewed by project admins first)</b-form-radio>
                        <span class="text-muted mr-3 ml-2">|</span>
                        <label for="self-report-checkbox" class="m-0">
                          <b-form-checkbox data-cy="justificationRequiredCheckbox" id="justification-required-checkbox"
                                           class="d-inline" v-model="settings.selfReportJustificationRequired.value"
                                           :disabled="!approvalSelected || !selfReport.enabled" @input="justificationRequiredChanged"/>
                          <span class="font-italic" :class="{ 'text-secondary': !approvalSelected || !selfReport.enabled}">Justification Required </span><inline-help
                          msg="Check to require users to submit a justification when self-reporting this skill"
                          target-id="justificationRequired"/>
                        </label>
                      </div>
                    </template>
                  </b-form-radio-group>
                </b-form-group>
              </b-card>
            </div>
          </div>

          <div class="row mt-3">
            <div class="col col-md-3 text-secondary" id="rankAndLeaderboardOptOutLabel">
              Rank Opt-Out for ALL Admins:
              <inline-help
                target-id="rankAndLeaderboardOptOutHelp"
                msg="Change to true and all of the project's admins will not be shown on the Leaderboard or assigned a rank"/>
            </div>
            <div class="col">
              <b-form-checkbox v-model="settings.rankAndLeaderboardOptOut.value"
                               name="check-button"
                               v-on:input="rankAndLeaderboardOptOutChanged"
                               aria-labelledby="rankAndLeaderboardOptOutLabel"
                               data-cy="rankAndLeaderboardOptOutSwitch"
                               switch>
                {{ rankOptOutLabel }}
              </b-form-checkbox>
            </div>
          </div>

          <div class="row mt-3">
            <div class="col col-md-3 text-secondary" id="customLabelsLabel">
              Custom Labels:
              <inline-help
                target-id="customLabelsSwitchHelp"
                msg="Enabling allows for setting custom labels in the Skills Display component"/>
            </div>
            <div class="col">
              <b-form-checkbox v-model="showCustomLabelsConfigToggle"
                               name="check-button"
                               aria-labelledby="customLabelsLabel"
                               data-cy="customLabelsSwitch"
                               switch>
                {{ showCustomLabelsConfigLabel }}
              </b-form-checkbox>

              <b-collapse id="customLabelsCollapse" :visible="showCustomLabelsConfigToggle">
                <b-card class="mt-1">
                  <ValidationProvider rules="maxCustomLabelLength" v-slot="{errors}"
                                      name="Project Display Text">
                    <div class="row mb-1">
                      <div class="col col-md-3 text-secondary" id="projectDisplayTextLabel">
                        Project Display Text:
                        <inline-help
                          target-id="projectDisplayTextHelp"
                          msg='The word "Project" may be overloaded to some organizations.  You can change the value displayed to users in Skills Display here.'/>
                      </div>
                      <div class="col">
                        <b-form-input v-model="settings.projectDisplayName.value"
                                      data-cy="projectDisplayTextInput"
                                      v-on:input="projectDisplayNameChanged"
                                      aria-labelledby="projectDisplayTextLabel">
                        </b-form-input>
                      </div>
                    </div>
                    <div v-if="errors && errors.length > 0" class="row">
                      <div class="col">
                        <small role="alert" class="form-text text-danger" id="projectDisplayTextError"
                               data-cy="projectDisplayTextError">{{ errors[0] }}</small>
                      </div>
                    </div>
                  </ValidationProvider>
                  <ValidationProvider rules="maxCustomLabelLength" v-slot="{errors}"
                                      name="Subject Display Text">
                    <div class="row mb-1">
                      <div class="col col-md-3 text-secondary" id="subjectDisplayTextLabel">
                        Subject Display Text:
                        <inline-help
                          target-id="subjectDisplayTextHelp"
                          msg='The word "Subject" may be overloaded to some organizations.  You can change the value displayed to users in Skills Display here.'/>
                      </div>
                      <div class="col">
                        <b-form-input v-model="settings.subjectDisplayName.value"
                                      data-cy="subjectDisplayTextInput"
                                      v-on:input="subjectDisplayNameChanged"
                                      aria-labelledby="subjectDisplayTextLabel">
                        </b-form-input>
                      </div>
                    </div>
                    <div v-if="errors && errors.length > 0" class="row">
                      <div class="col">
                        <small role="alert" class="form-text text-danger" id="subjectDisplayTextError"
                               data-cy="subjectDisplayTextError">{{ errors[0] }}</small>
                      </div>
                    </div>
                  </ValidationProvider>
                  <ValidationProvider rules="maxCustomLabelLength" v-slot="{errors}"
                                      name="Group Display Text">
                    <div class="row mb-1">
                      <div class="col col-md-3 text-secondary" id="groupDisplayTextLabel">
                        Group Display Text:
                        <inline-help
                          target-id="groupDisplayTextHelp"
                          msg='The word "Group" may be overloaded to some organizations.  You can change the value displayed to users in Skills Display here.'/>
                      </div>
                      <div class="col">
                        <b-form-input v-model="settings.groupDisplayName.value"
                                      data-cy="groupDisplayTextInput"
                                      v-on:input="groupDisplayNameChanged"
                                      aria-labelledby="groupDisplayTextLabel">
                        </b-form-input>
                      </div>
                    </div>
                    <div v-if="errors && errors.length > 0" class="row">
                      <div class="col">
                        <small role="alert" class="form-text text-danger" id="groupDisplayTextError"
                               data-cy="groupDisplayTextError">{{ errors[0] }}</small>
                      </div>
                    </div>
                  </ValidationProvider>

                  <ValidationProvider rules="maxCustomLabelLength" v-slot="{errors}"
                                      name="Skill Display Text">
                    <div class="row mb-1">
                      <div class="col col-md-3 text-secondary" id="skillDisplayTextLabel">
                        Skill Display Text:
                        <inline-help
                          target-id="skillDisplayTextHelp"
                          msg='The word "Skill" may be overloaded to some organizations.  You can change the value displayed to users in Skills Display here.'/>
                      </div>
                      <div class="col">
                        <b-form-input v-model="settings.skillDisplayName.value"
                                      data-cy="skillDisplayTextInput"
                                      v-on:input="skillDisplayNameChanged"
                                      aria-labelledby="skillDisplayTextLabel">
                        </b-form-input>
                      </div>
                    </div>
                    <div v-if="errors && errors.length > 0" class="row">
                      <div class="col">
                        <small role="alert" class="form-text text-danger" id="skillDisplayTextError"
                               data-cy="skillDisplayTextError">{{ errors[0] }}</small>
                      </div>
                    </div>
                  </ValidationProvider>
                  <ValidationProvider rules="maxCustomLabelLength" v-slot="{errors}"
                                      name="Level Display Text">
                    <div class="row">
                      <div class="col col-md-3 text-secondary" id="levelDisplayTextLabel">
                        Level Display Text:
                        <inline-help
                          target-id="levelDisplayTextHelp"
                          msg='The word "Level" may be overloaded to some organizations.  You can change the value displayed to users in Skills Display here.'/>
                      </div>
                      <div class="col">
                        <b-form-input v-model="settings.levelDisplayName.value"
                                      data-cy="levelDisplayTextInput"
                                      v-on:input="levelDisplayNameChanged"
                                      aria-labelledby="levelDisplayTextLabel">
                        </b-form-input>
                      </div>
                    </div>
                    <div v-if="errors && errors.length > 0" class="row">
                      <div class="col">
                        <small role="alert" class="form-text text-danger" id="levelDisplayTextError"
                               data-cy="levelDisplayTextError">{{ errors[0] }}</small>
                      </div>
                    </div>
                  </ValidationProvider>
                </b-card>
              </b-collapse>
            </div>
          </div>

          <div class="row mt-3">
            <div class="col col-md-3 text-secondary" id="groupDescriptions">
              Always Show Group Descriptions:
              <inline-help
                target-id="groupDescriptionsHelp"
                msg="Toggle this setting to always show the group's descriptions in this project"/>
            </div>
            <div class="col">
              <b-form-checkbox v-model="settings.groupDescriptions.value"
                               name="check-button"
                               v-on:input="groupDescriptionsChanged"
                               aria-labelledby="groupDescriptionsLabel"
                               data-cy="groupDescriptionsSwitch"
                               switch>
                {{ groupDescriptionsLabel }}
              </b-form-checkbox>
            </div>
          </div>

          <hr/>

          <p v-if="errMsg" class="text-center text-danger mt-3" role="alert">***{{ errMsg }}***</p>

          <div class="row">
            <div class="col">
              <b-button variant="outline-success" @click="handleSubmit(save)" :disabled="invalid || !isDirty" data-cy="saveSettingsBtn">
                Save <i class="fas fa-arrow-circle-right"/>
              </b-button>

              <span v-if="isDirty" class="text-warning ml-2" data-cy="unsavedChangesAlert">
                <i class="fa fa-exclamation-circle"
                   aria-label="Settings have been changed, do not forget to save"
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
  </ValidationObserver>
</template>

<script>
  import { extend, ValidationProvider } from 'vee-validate';
  import { SkillsReporter } from '@skilltree/skills-client-vue';
  import MsgBoxMixin from '@/components/utils/modal/MsgBoxMixin';
  import SettingService from './SettingsService';
  import SubPageHeader from '../utils/pages/SubPageHeader';
  import SimpleCard from '../utils/cards/SimpleCard';
  import InlineHelp from '../utils/InlineHelp';
  import LoadingContainer from '../utils/LoadingContainer';
  import ToastSupport from '../utils/ToastSupport';

  const publicNotDiscoverable = 'pnd';
  const discoverableProgressAndRanking = 'dpr';
  const privateInviteOnly = 'pio';

  extend('root_help_url', {
    message: (field) => `${field} must start with "http(s)"`,
    validate(value) {
      if (!value) {
        return true;
      }
      return value.startsWith('http') || value.startsWith('https');
    },
  });

  export default {
    name: 'ProjectSettings',
    mixins: [ToastSupport, MsgBoxMixin],
    components: {
      LoadingContainer,
      InlineHelp,
      SimpleCard,
      SubPageHeader,
      ValidationProvider,
    },
    data() {
      return {
        isLoading: true,
        showCustomLabelsConfigToggle: false,
        selfReport: {
          enabled: false,
          justificationRequired: false,
          selected: 'Approval',
          options: [
            // { text: 'Approval Queue (reviewed by project admins first)', value: 'Approval', disabled: true },
            { text: 'Honor System (applied right away)', value: 'HonorSystem', disabled: true },
          ],
        },
        settings: {
          projectVisibility: {
            value: 'pnd',
            setting: 'synthetic.project_visibility',
            lastLoadedValue: 'pnd',
            dirty: false,
            projectId: this.$route.params.projectId,
          },
          productionModeEnabled: {
            value: 'false',
            setting: 'production.mode.enabled',
            lastLoadedValue: 'false',
            dirty: false,
            projectId: this.$route.params.projectId,
          },
          inviteOnlyProject: {
            value: 'false',
            setting: 'invite_only',
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
          selfReportJustificationRequired: {
            value: 'false',
            setting: 'selfReport.justificationRequired',
            lastLoadedValue: 'false',
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
          groupDescriptions: {
            value: false,
            setting: 'group-descriptions',
            lastLoadedValue: false,
            dirty: false,
            projectId: this.$route.params.projectId,
          },
          projectDisplayName: {
            value: 'Project',
            setting: 'project.displayName',
            lastLoadedValue: 'Project',
            dirty: false,
            projectId: this.$route.params.projectId,
          },
          subjectDisplayName: {
            value: 'Subject',
            setting: 'subject.displayName',
            lastLoadedValue: 'Subject',
            dirty: false,
            projectId: this.$route.params.projectId,
          },
          groupDisplayName: {
            value: 'Group',
            setting: 'group.displayName',
            lastLoadedValue: 'Group',
            dirty: false,
            projectId: this.$route.params.projectId,
          },
          skillDisplayName: {
            value: 'Skill',
            setting: 'skill.displayName',
            lastLoadedValue: 'Skill',
            dirty: false,
            projectId: this.$route.params.projectId,
          },
          levelDisplayName: {
            value: 'Level',
            setting: 'level.displayName',
            lastLoadedValue: 'Level',
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
        return !!foundDirty;
      },
      isProgressAndRankingEnabled() {
        return this.$store.getters.config.rankingAndProgressViewsEnabled === true || this.$store.getters.config.rankingAndProgressViewsEnabled === 'true';
      },
      approvalSelected() {
        return this.selfReport.selected === 'Approval';
      },
      shouldShowCustomLabelsConfig() {
        return this.showCustomLabelsConfigToggle
          || this.settings.projectDisplayName.value !== 'Project' || this.settings.projectDisplayName.dirty
          || this.settings.subjectDisplayName.value !== 'Subject' || this.settings.subjectDisplayName.dirty
          || this.settings.groupDisplayName.value !== 'Group' || this.settings.groupDisplayName.dirty
          || this.settings.skillDisplayName.value !== 'Skill' || this.settings.skillDisplayName.dirty
          || this.settings.levelDisplayName.value !== 'Level' || this.settings.levelDisplayName.dirty;
      },
      showCustomLabelsConfigLabel() {
        if (this.showCustomLabelsConfigToggle) {
          return 'Enabled';
        }
        return 'Disabled';
      },
      selfReportingEnabledLabel() {
        if (this.selfReport.enabled) {
          return 'Enabled';
        }
        return 'Disabled';
      },
      groupDescriptionsLabel() {
        if (this.settings.groupDescriptions.value === 'true' || this.settings.groupDescriptions.value === true) {
          return 'Enabled';
        }
        return 'Disabled';
      },
      rankOptOutLabel() {
        if (this.settings.rankAndLeaderboardOptOut.value) {
          return 'Enabled';
        }
        return 'Disabled';
      },
      usePointsForLevelsLabel() {
        if (this.settings.levelPointsEnabled.value) {
          return 'Enabled';
        }
        return 'Disabled';
      },
      projectVisibilityOptions() {
        const opts = [
          { value: publicNotDiscoverable, text: 'Public Not Discoverable' },
          { value: privateInviteOnly, text: 'Private Invite Only' },
        ];
        if (this.isProgressAndRankingEnabled) {
          opts.push({ value: discoverableProgressAndRanking, text: 'Discoverable on Progress And Ranking' });
        }
        return opts;
      },
      projectVisibilityHelpMsg() {
        if (this.isProgressAndRankingEnabled) {
          return 'Public Not Discoverable (default) projects can be accessed directly but are not discoverable on the Progress and Ranking page. '
            + 'Discoverable on Progress and Ranking projects can be accessed directly and are selectable on the Progress and Ranking page. '
            + 'Private Invite Only projects may ONLY be accessed by users who have been issued a specific project invite. ';
        }
        return 'Public Not Discoverable (default) projects can be accessed directly by any user. '
          + 'Private Invite Only projects may ONLY be accessed by users who have been issued a specific project invite. ';
      },
    },
    watch: {
      isDirty(newVal, oldVal) {
        if (newVal === false && oldVal === true && this.errMsg !== null) {
          this.errMsg = null;
        }
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
      groupDescriptionsChanged(value) {
        this.settings.groupDescriptions.dirty = `${value}` !== `${this.settings.groupDescriptions.lastLoadedValue}`;
      },
      inviteOnlyProjectChanged(value) {
        this.settings.inviteOnlyProject.dirty = `${value}` !== `${this.settings.inviteOnlyProject.lastLoadedValue}`;
      },
      selfReportingTypeChanged(value) {
        this.settings.selfReportType.value = value;
        this.settings.selfReportType.dirty = `${this.settings.selfReportType.value}` !== `${this.settings.selfReportType.lastLoadedValue}`;
      },
      justificationRequiredChanged(value) {
        this.settings.selfReportJustificationRequired.dirty = `${value}` !== `${this.settings.selfReportJustificationRequired.lastLoadedValue}`;
      },
      levelPointsEnabledChanged(value) {
        this.settings.levelPointsEnabled.dirty = `${value}` !== `${this.settings.levelPointsEnabled.lastLoadedValue}`;
      },
      helpUrlHostChanged(value) {
        this.settings.helpUrlHost.dirty = `${value}` !== `${this.settings.helpUrlHost.lastLoadedValue}`;
      },
      projectDisplayNameChanged(value) {
        this.settings.projectDisplayName.dirty = `${value}` !== `${this.settings.projectDisplayName.lastLoadedValue}`;
      },
      subjectDisplayNameChanged(value) {
        this.settings.subjectDisplayName.dirty = `${value}` !== `${this.settings.subjectDisplayName.lastLoadedValue}`;
      },
      groupDisplayNameChanged(value) {
        this.settings.groupDisplayName.dirty = `${value}` !== `${this.settings.groupDisplayName.lastLoadedValue}`;
      },
      skillDisplayNameChanged(value) {
        this.settings.skillDisplayName.dirty = `${value}` !== `${this.settings.skillDisplayName.lastLoadedValue}`;
      },
      levelDisplayNameChanged(value) {
        this.settings.levelDisplayName.dirty = `${value}` !== `${this.settings.levelDisplayName.lastLoadedValue}`;
      },
      projectVisibilityChanged(value) {
        const dirty = `${value}` !== `${this.settings.projectVisibility.lastLoadedValue}`;
        this.settings.projectVisibility.dirty = dirty;
        if (dirty) {
          if (value === publicNotDiscoverable) {
            this.settings.inviteOnlyProject.value = 'false';
            this.settings.productionModeEnabled.value = 'false';
          } else if (value === privateInviteOnly) {
            this.settings.inviteOnlyProject.value = 'true';
            this.settings.productionModeEnabled.value = 'false';
            this.msgOk('Changing this Project to Invite Only '
              + 'will restrict access to the training profile and '
              + 'skill reporting to only invited users.', 'Changing to Invite Only');
          } else if (value === discoverableProgressAndRanking) {
            this.settings.inviteOnlyProject.value = 'false';
            this.settings.productionModeEnabled.value = 'true';
          }
        } else {
          this.settings.inviteOnlyProject.value = this.settings.inviteOnlyProject.lastLoadedValue;
          this.settings.productionModeEnabled.value = this.settings.productionModeEnabled.lastLoadedValue;
        }
        this.inviteOnlyProjectChanged(this.settings.inviteOnlyProject.value);
        this.productionModeEnabledChanged(this.settings.productionModeEnabled.value);
      },
      loadSettings() {
        SettingService.getSettingsForProject(this.$route.params.projectId)
          .then((response) => {
            if (response) {
              this.setSyntheticSetting(response);
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
            this.showCustomLabelsConfigToggle = this.shouldShowCustomLabelsConfig;
          })
          .finally(() => {
            this.isLoading = false;
          });
      },
      setSyntheticSetting(settingsResponse) {
        const productionMode = settingsResponse.find((setting) => setting.setting === 'production.mode.enabled');
        const inviteOnly = settingsResponse.find((setting) => setting.setting === 'invite_only');

        let selectedValue = publicNotDiscoverable;
        if (inviteOnly?.value === 'true') {
          selectedValue = privateInviteOnly;
        } else if (productionMode?.value === 'true') {
          selectedValue = discoverableProgressAndRanking;
        }
        this.settings.projectVisibility.lastLoadedValue = selectedValue;
        this.settings.projectVisibility.value = selectedValue;
        this.settings.projectVisibility.dirty = false;
      },
      save() {
        this.$refs.observer.validate()
          .then((res1) => {
            if (!res1) {
              this.errMsg = 'Form did NOT pass validation, please fix and try to Save again';
            } else {
              const dirtyChanges = Object.values(this.settings).filter((item) => item.dirty && !item.setting.startsWith('synthetic.'));
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
            }
          });
      },
      saveSettings(dirtyChanges) {
        SettingService.saveSettings(this.$route.params.projectId, dirtyChanges)
          .then(() => {
            this.showSavedMsg = true;
            this.$announcer.polite('Project Settings have been successfully saved');
            setTimeout(() => {
              this.showSavedMsg = false;
            }, 4000);
            const entries = Object.entries(this.settings);
            entries.forEach((entry) => {
              const [key, value] = entry;
              this.settings[key] = Object.assign(value, {
                dirty: false,
                lastLoadedValue: value.value,
              });
              if (value.setting === this.settings.helpUrlHost.setting && value.value && value.value.length > 0) {
                SkillsReporter.reportSkill('ConfigureProjectRootHelpUrl');
              }
            });
            this.$store.dispatch('loadProjConfigState', this.$route.params.projectId);
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

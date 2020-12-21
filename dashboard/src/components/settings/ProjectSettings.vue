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

        <hr/>

        <p v-if="errMsg" class="text-center text-danger mt-3" role="alert">***{{ errMsg }}***</p>

        <div class="row">
          <div class="col">
            <b-button variant="outline-success" @click="save" :disabled="!isDirty">
              Save <i class="fas fa-arrow-circle-right"/>
            </b-button>

            <span v-if="isDirty" class="text-warning ml-2">
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
        settings: {
          levelPointsEnabled: {
            value: 'false',
            setting: 'level.points.enabled',
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
        },
        errMsg: null,
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
            this.successToast('Settings Updated', 'Successfully saved settings!');
            const entries = Object.entries(this.settings);
            entries.forEach((entry) => {
              const [key, value] = entry;
              this.settings[key] = Object.assign(value, { dirty: false, lastLoadedValue: value.value });
              if (value.setting === this.settings.helpUrlHost.setting) {
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

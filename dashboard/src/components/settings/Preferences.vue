/*
Copyright 2024 SkillTree

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
<script setup>
import { computed, onMounted, ref } from 'vue'
import SubPageHeader from '@/components/utils/pages/SubPageHeader.vue'
import SettingsService from '@/components/settings/SettingsService.js'
import { useAppConfig } from '@/common-components/stores/UseAppConfig.js'
import { useColors } from '@/skills-display/components/utilities/UseColors.js'
import {useThemesHelper} from "@/components/header/UseThemesHelper.js";
import {useUserInfo} from "@/components/utils/UseUserInfo.js";
import {useUserPreferences} from "@/stores/UseUserPreferences.js";
import KeyboardShortcutInput from "@/components/settings/KeyboardShortcutInput.vue";
import { useSkillsDisplayAttributesState } from '@/skills-display/stores/UseSkillsDisplayAttributesState.js'

const themeHelper = useThemesHelper()
const appConfig = useAppConfig();
const colors = useColors()
const userInfo = useUserInfo()
const userPreferences = useUserPreferences()
const displayAttributes = useSkillsDisplayAttributesState()
const progressViewOptions = [
  { value: 'project-default', label: 'Use project default' },
  { value: 'compact', label: 'Compact' },
  { value: 'expanded', label: 'Expanded' },
]

const isLoading = ref(true);
const settings = ref({
  progressView: {
    settingGroup: 'user.prefs',
    value: 'project-default',
    setting: 'sd_progress_view',
    lastLoadedValue: 'project-default',
    dirty: false,
    type: String,
  },
  homePage: {
    settingGroup: 'user.prefs',
    value: 'admin',
    setting: 'home_page',
    lastLoadedValue: '',
    dirty: false,
    type: String,
  },
  rankAndLeaderboardOptOut: {
    settingGroup: 'user.prefs',
    value: false,
    setting: 'rank_and_leaderboard_optOut',
    lastLoadedValue: false,
    dirty: false,
    type: Boolean,
  },
  enableDarkMode: {
    settingGroup: 'user.prefs',
    value: false,
    setting: 'enable_dark_mode',
    lastLoadedValue: false,
    dirty: false,
    type: Boolean,
  },
  sdSearchKeyboardShortcut: {
    settingGroup: 'user.prefs',
    value: 'Ctrl + k',
    setting: 'sd_search_training_keyboard_shortcut',
    lastLoadedValue: false,
    dirty: false,
    type: String,
  },
  sdPreviousSkillKeyboardShortcut: {
    settingGroup: 'user.prefs',
    value: 'Ctrl + Alt + p',
    setting: 'sd_previous_skill_keyboard_shortcut',
    lastLoadedValue: false,
    dirty: false,
    type: String,
  },
  sdNextSkillKeyboardShortcut: {
    settingGroup: 'user.prefs',
    value: 'Ctrl + Alt + n',
    setting: 'sd_next_skill_keyboard_shortcut',
    lastLoadedValue: false,
    dirty: false,
    type: String,
  }
});
const errMsg = ref(null);
const showSavedMsg = ref(false);

onMounted(() => {
  loadSettings();
})

let isDirty = computed(() => {
  return Object.values(settings.value).find((item) => item.dirty);
});

function loadSettings() {
  userPreferences.loadUserPreferences()
      .then((response) => {
        if (response) {
          const prefKeys = Object.keys(settings.value)
          let hasHomeKey = false;
          prefKeys.forEach((key) => {
            const currentSettingsObj = settings.value[key]
            const newSettingValue = response[currentSettingsObj.setting]
            if (newSettingValue !== undefined && newSettingValue !== null) {
              if (currentSettingsObj.type === Boolean) {
                settings.value[key].value = newSettingValue?.toLowerCase() === 'true';
              } else {
                settings.value[key].value = newSettingValue
              }
              settings.value[key].lastLoadedValue = settings.value[key].value
              if (key === 'homePage') {
                hasHomeKey = true;
              }
            }
          })
          if (!hasHomeKey) {
            settings.value.homePage.value = appConfig.defaultLandingPage;
          }
        }
      })
      .finally(() => {
        isLoading.value = false;
      });
}

function save() {
  const dirtyChanges = Object.values(settings.value).filter((item) => item.dirty);
  if (dirtyChanges) {
    isLoading.value = true;
    SettingsService.checkUserSettingsValidity(dirtyChanges)
        .then((res) => {
          if (res.valid) {
            saveUserSettings(dirtyChanges);
          } else {
            errMsg.value = res.explanation;
            isLoading.value = false;
          }
        });
  }
}

function saveUserSettings(dirtyChanges) {
  SettingsService.saveUserSettings(dirtyChanges)
      .then(() => {
        if (dirtyChanges.some((setting) => setting.setting === 'sd_progress_view')) {
          displayAttributes.invalidateConfig()
          // The save may finish after SPA navigation back to an active display.
          displayAttributes.loadConfigStateIfNeeded().catch(() => { /* Shared Axios handling displays configuration errors. */ })
        }
        showSavedMsg.value = true;
        setTimeout(() => { showSavedMsg.value = false; }, 4000);
        const entries = Object.entries(settings.value);
        entries.forEach((entry) => {
          const [key, value] = entry;
          settings.value[key] = Object.assign(value, { dirty: false, lastLoadedValue: value.value });
          if (key === 'homePage') {
            userInfo.userInfo.value.landingPage = value.value
          }
          if (key === 'enableDarkMode') {
            if(value.value) {
              themeHelper.setDarkMode();
            } else {
              themeHelper.setLightMode();
            }
          }
        });
      })
      .finally(() => {
        // reload user preferences after saving
        useUserPreferences().loadUserPreferences()
        isLoading.value = false;
      });
}

function isProgressAndRankingEnabled() {
  return (appConfig.rankingAndProgressViewsEnabled === true || appConfig.rankingAndProgressViewsEnabled === 'true') && userInfo.userInfo.value.adminDashboardAccess;
}

function homePagePrefChanged() {
  settings.value.homePage.dirty = `${settings.value.homePage.value}` !== `${settings.value.homePage.lastLoadedValue}`;
}

function progressViewChanged() {
  settings.value.progressView.dirty = settings.value.progressView.value !== settings.value.progressView.lastLoadedValue
}

function rankAndLeaderboardOptOutPrefChanged() {
  settings.value.rankAndLeaderboardOptOut.dirty = `${settings.value.rankAndLeaderboardOptOut.value}` !== `${settings.value.rankAndLeaderboardOptOut.lastLoadedValue}`;
}

function enableDarkModeChanged() {
  settings.value.enableDarkMode.dirty = `${settings.value.enableDarkMode.value}` !== `${settings.value.enableDarkMode.lastLoadedValue}`;
}
function sdNextSkillKeyboardShortcutChanged() {
  settings.value.sdNextSkillKeyboardShortcut.dirty = `${settings.value.sdNextSkillKeyboardShortcut.value}` !== `${settings.value.sdNextSkillKeyboardShortcut.lastLoadedValue}`;
}
function sdPreviousSkillKeyboardShortcutChanged() {
  settings.value.sdPreviousSkillKeyboardShortcut.dirty = `${settings.value.sdPreviousSkillKeyboardShortcut.value}` !== `${settings.value.sdPreviousSkillKeyboardShortcut.lastLoadedValue}`;
}
function sdSearchKeyboardShortcutChanged() {
  settings.value.sdSearchKeyboardShortcut.dirty = `${settings.value.sdSearchKeyboardShortcut.value}` !== `${settings.value.sdSearchKeyboardShortcut.lastLoadedValue}`;
}

</script>

<template>
  <sub-page-header title="Preferences"/>

  <Card v-if="!isLoading">
    <template #content>
      <div class="flex flex-col">
        <div data-cy="enableDarkMode" class="flex flex-col gap-3 border-b border-surface pb-6 sm:flex-row sm:items-center sm:gap-4">
          <label for="enableDarkModeSwitch" class="flex items-center gap-2 font-semibold">
            <i class="fas fa-moon fa-fw" :class="colors.getTextClass(2)" aria-hidden="true"></i> <span id="enableDarkModeLabel">Dark Mode:</span>
          </label>
          <div class="flex shrink-0 items-center gap-2">
            <ToggleSwitch v-model="settings.enableDarkMode.value" data-cy="enableDarkModeSwitch"
                          aria-labelledby="enableDarkModeLabel"
                          inputId="enableDarkModeSwitch"
                          @change="enableDarkModeChanged" />
            <span class="min-w-8">{{ settings.enableDarkMode.value ? 'On' : 'Off'}}</span>
          </div>
        </div>
        <div class="border-b border-surface py-6">
          <fieldset id="progress-view-preference" class="min-w-0" data-cy="progressViewPreferenceSetting"
                    aria-describedby="progressViewPreferenceHelp">
            <legend class="font-semibold"><i class="fa-solid fa-chart-line fa-fw mr-2" :class="colors.getTextClass(2)" aria-hidden="true"></i>Default Progress View:</legend>
            <div class="pl-7">
              <p id="progressViewPreferenceHelp" class="mt-2 text-sm text-muted-color">Choose how project and subject trainee pages display progress.</p>
              <div class="mt-4 flex flex-col gap-3 sm:flex-row sm:flex-wrap sm:gap-x-6">
                <div v-for="option in progressViewOptions" :key="option.value" class="flex items-center gap-2">
                  <RadioButton v-model="settings.progressView.value" :value="option.value" name="progressView"
                               :inputId="`progressView-${option.value}`" :data-cy="`progressViewPreference-${option.value}`"
                               @change="progressViewChanged" />
                  <label :for="`progressView-${option.value}`">{{ option.label }}</label>
                </div>
              </div>
            </div>
          </fieldset>
        </div>
        <div data-cy="defaultHomePageSetting" v-if="isProgressAndRankingEnabled()" class="border-b border-surface py-6">
          <fieldset class="min-w-0">
            <legend class="font-semibold"><i class="fas fa-home fa-fw mr-2" :class="colors.getTextClass(1)" aria-hidden="true"></i>Default Home Page:</legend>
            <div class="mt-4 flex flex-col gap-3 pl-7 sm:flex-row sm:flex-wrap sm:gap-x-6">
              <div class="flex items-center gap-2">
                <RadioButton v-model="settings.homePage.value" data-cy="admin" value="admin" inputId="admin" @change="homePagePrefChanged" />
                <label for="admin">Project Admin</label>
              </div>
              <div class="flex items-center gap-2">
                <RadioButton v-model="settings.homePage.value" data-cy="progress" value="progress" inputId="progress" @change="homePagePrefChanged"/>
                <label for="progress">Progress and Rankings</label>
              </div>
            </div>
          </fieldset>
        </div>
        <div data-cy="rankOptOut" class="flex flex-col gap-3 border-b border-surface py-6 sm:flex-row sm:items-center sm:gap-4">
          <label for="rankAndLeaderboardOptOutSwitch" class="flex items-center gap-2 font-semibold">
            <i class="fas fa-users-slash fa-fw" :class="colors.getTextClass(2)" aria-hidden="true"></i> <span id="rankAndLeaderboardOptOutLabel">Rank and Leaderboard Opt-Out:</span>
          </label>
          <div class="flex shrink-0 items-center gap-2">
            <ToggleSwitch v-model="settings.rankAndLeaderboardOptOut.value" data-cy="rankAndLeaderboardOptOutSwitch"
                         aria-labelledby="rankAndLeaderboardOptOutLabel"
                         inputId="rankAndLeaderboardOptOutSwitch"
                         @change="rankAndLeaderboardOptOutPrefChanged" />
            <span class="min-w-8">{{ settings.rankAndLeaderboardOptOut.value ? 'Yes' : 'No'}}</span>
          </div>
        </div>
        <div class="border-b border-surface py-6">
          <fieldset class="min-w-0">
            <legend class="font-semibold"><i class="fas fa-keyboard fa-fw mr-2" :class="colors.getTextClass(2)" aria-hidden="true"></i>Keyboard shortcuts when taking a training</legend>
            <div class="mt-4 grid grid-cols-1 gap-4 pl-7 lg:grid-cols-3">
              <keyboard-shortcut-input
                  label="Search"
                  v-model="settings.sdSearchKeyboardShortcut.value"
                  @change="sdSearchKeyboardShortcutChanged"/>
              <keyboard-shortcut-input
                  label="Previous Skill"
                  v-model="settings.sdPreviousSkillKeyboardShortcut.value"
                  @change="sdPreviousSkillKeyboardShortcutChanged"/>
              <keyboard-shortcut-input
                  label="Next Skill"
                  v-model="settings.sdNextSkillKeyboardShortcut.value"
                  @change="sdNextSkillKeyboardShortcutChanged"/>
            </div>
          </fieldset>
        </div>
        <div class="flex flex-wrap items-center gap-2 pt-6">
          <SkillsButton label="Save" icon="fas fa-arrow-circle-right" @click.stop="save" size="small" :disabled="!isDirty" data-cy="userPrefsSettingsSave" />
          <InlineMessage v-if="isDirty" data-cy="unsavedChangesAlert" severity="warn">Unsaved Changes</InlineMessage>
          <InlineMessage v-if="!isDirty && showSavedMsg" data-cy="settingsSavedAlert" severity="success">Settings Updated!</InlineMessage>
        </div>
      </div>
    </template>
  </Card>
</template>

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

const themeHelper = useThemesHelper()
const appConfig = useAppConfig();
const colors = useColors()
const userInfo = useUserInfo()
const userPreferences = useUserPreferences()

const isLoading = ref(true);
const settings = ref({
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
        showSavedMsg.value = true;
        setTimeout(() => { showSavedMsg.value = false; }, 4000);
        const entries = Object.entries(settings.value);
        entries.forEach((entry) => {
          const [key, value] = entry;
          settings[key] = Object.assign(value, { dirty: false, lastLoadedValue: value.value });
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
      <div data-cy="defaultHomePageSetting" v-if="isProgressAndRankingEnabled()" class="pb-6">
        <label :for="settings.homePage.value === 'admin' ? 'progress' : 'admin'">
          <i class="fas fa-home mr-1" :class="colors.getTextClass(1)" aria-hidden="true"></i> Default Home Page:
        </label>
        <div class="pt-2 pl-2">
          <RadioButton v-model="settings.homePage.value" data-cy="admin" value="admin" inputId="admin" @change="homePagePrefChanged" />
          <label for="admin" class="ml-2">Project Admin</label>
        </div>
        <div class="pt-2 pl-2">
          <RadioButton v-model="settings.homePage.value" data-cy="progress" value="progress" inputId="progress" @change="homePagePrefChanged"/>
          <label for="progress" class="ml-2">Progress and Rankings</label>
        </div>
      </div>
      <div data-cy="rankOptOut" class="pb-2 flex content-center items-center">
        <label for="rankAndLeaderboardOptOutSwitch">
          <i class="fas fa-users-slash mr-2" :class="colors.getTextClass(2)" aria-hidden="true"></i> <span id="rankAndLeaderboardOptOutLabel">Rank and Leaderboard Opt-Out:</span>
        </label>
        <ToggleSwitch v-model="settings.rankAndLeaderboardOptOut.value" data-cy="rankAndLeaderboardOptOutSwitch" class="ml-2"
                     aria-labelledby="rankAndLeaderboardOptOutLabel"
                     inputId="rankAndLeaderboardOptOutSwitch"
                     @change="rankAndLeaderboardOptOutPrefChanged" />
        <span class="ml-2">{{ settings.rankAndLeaderboardOptOut.value ? 'Yes' : 'No'}}</span>
      </div>
      <div data-cy="enableDarkMode" class="pt-2 pb-2 flex content-center items-center">
        <label for="enableDarkModeSwitch">
          <i class="fas fa-moon mr-2" :class="colors.getTextClass(2)" aria-hidden="true"></i> <span id="enableDarkModeLabel">Dark Mode:</span>
        </label>
        <ToggleSwitch v-model="settings.enableDarkMode.value" data-cy="enableDarkModeSwitch" class="ml-2"
                     aria-labelledby="enableDarkModeLabel"
                     inputId="enableDarkModeSwitch"
                     @change="enableDarkModeChanged" />
        <span class="ml-2">{{ settings.enableDarkMode.value ? 'On' : 'Off'}}</span>
      </div>
      <Fieldset legend="Keyboard shortcuts when taking a training" class="mt-3">
        <div class="flex flex-col gap-4 pt-2">
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
      </Fieldset>
      <div class="mt-3 flex gap-2">
        <SkillsButton label="Save" icon="fas fa-arrow-circle-right" @click.stop="save" size="small" :disabled="!isDirty" data-cy="userPrefsSettingsSave" />
        <InlineMessage v-if="isDirty" data-cy="unsavedChangesAlert" severity="warn">Unsaved Changes</InlineMessage>
        <InlineMessage v-if="!isDirty && showSavedMsg" data-cy="settingsSavedAlert" severity="success">Settings Updated!</InlineMessage>
      </div>
    </template>
  </Card>
</template>

<style scoped></style>

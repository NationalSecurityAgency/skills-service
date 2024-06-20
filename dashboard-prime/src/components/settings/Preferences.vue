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
import {usePrimeVue} from "primevue/config";
import {useThemesHelper} from "@/components/header/UseThemesHelper.js";

const PrimeVue = usePrimeVue()
const themeHelper = useThemesHelper()
const appConfig = useAppConfig();
const colors = useColors()

const isLoading = ref(true);
const settings = ref({
  homePage: {
    settingGroup: 'user.prefs',
    value: 'admin',
    setting: 'home_page',
    lastLoadedValue: '',
    dirty: false,
  },
  rankAndLeaderboardOptOut: {
    settingGroup: 'user.prefs',
    value: false,
    setting: 'rank_and_leaderboard_optOut',
    lastLoadedValue: false,
    dirty: false,
  },
  enableDarkMode: {
    settingGroup: 'user.prefs',
    value: false,
    setting: 'enable_dark_mode',
    lastLoadedValue: false,
    dirty: false,
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
  SettingsService.getUserSettings()
      .then((response) => {
        if (response) {
          const entries = Object.entries(settings.value);
          let hasHomeKey = false;
          entries.forEach((entry) => {
            const [key, value] = entry;
            const found = response.find((item) => item.setting === value.setting);
            if (found) {
              settings.value[key] = { dirty: false, lastLoadedValue: found.value, ...found };
              if (key === 'homePage') {
                hasHomeKey = true;
              }
              if (key === 'rankAndLeaderboardOptOut' || key === 'enableDarkMode') {
                settings.value[key].value = settings.value[key].value.toLowerCase() === 'true';
              }
            }
          });
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
            // const userInfo = { ...$store.getters.userInfo, landingPage: value.value };
            // $store.commit('storeUser', userInfo);
          }
          if (key === 'enableDarkMode') {
            const selectedThemes = value.value ? [themeHelper.themeOptions[0].value, themeHelper.themeOptions[1].value] : [themeHelper.themeOptions[1].value, themeHelper.themeOptions[0].value];
            PrimeVue.changeTheme(selectedThemes[0], selectedThemes[1], 'theme-link')
          }
        });
      })
      .finally(() => {
        isLoading.value = false;
      });
}

function isProgressAndRankingEnabled() {
  return appConfig.rankingAndProgressViewsEnabled === true || appConfig.rankingAndProgressViewsEnabled === 'true';
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
</script>

<template>
  <sub-page-header title="Preferences"/>

  <Card v-if="!isLoading">
    <template #content>
      <div data-cy="defaultHomePageSetting" v-if="isProgressAndRankingEnabled()">
        <i class="fas fa-home mr-1" :class="colors.getTextClass(1)" aria-hidden="true"></i> Default Home Page:
        <div class="pt-2 pl-2">
          <RadioButton v-model="settings.homePage.value" data-cy="admin" value="admin" inputId="admin" @change="homePagePrefChanged" />
          <label for="admin" class="ml-2">Project Admin</label>
        </div>
        <div class="pt-2 pl-2">
          <RadioButton v-model="settings.homePage.value" data-cy="progress" value="progress" inputId="progress" @change="homePagePrefChanged"/>
          <label for="progress" class="ml-2">Progress and Rankings</label>
        </div>
      </div>
      <div data-cy="rankOptOut" class="pt-4 pb-2 flex align-content-center align-items-center">
        <i class="fas fa-users-slash mr-2" :class="colors.getTextClass(2)" aria-hidden="true"></i> <span id="rankAndLeaderboardOptOutLabel">Rank and Leaderboard Opt-Out:</span>
        <InputSwitch v-model="settings.rankAndLeaderboardOptOut.value" data-cy="rankAndLeaderboardOptOutSwitch" class="ml-2"
                     aria-labelledby="rankAndLeaderboardOptOutLabel"
                     @change="rankAndLeaderboardOptOutPrefChanged" />
        <span class="ml-2">{{ settings.rankAndLeaderboardOptOut.value ? 'Yes' : 'No'}}</span>
      </div>
      <div data-cy="enableDarkMode" class="pt-2 pb-2 flex align-content-center align-items-center">
        <i class="fas fa-moon mr-2" :class="colors.getTextClass(2)" aria-hidden="true"></i> <span id="enableDarkModeLabel">Dark Mode:</span>
        <InputSwitch v-model="settings.enableDarkMode.value" data-cy="enableDarkModeSwitch" class="ml-2"
                     aria-labelledby="enableDarkModeLabel"
                     @change="enableDarkModeChanged" />
        <span class="ml-2">{{ settings.enableDarkMode.value ? 'On' : 'Off'}}</span>
      </div>
      <SkillsButton label="Save" icon="fas fa-arrow-circle-right" @click.stop="save" size="small" :disabled="!isDirty" data-cy="userPrefsSettingsSave"/>
      <span v-if="isDirty" class="text-warning ml-2" data-cy="unsavedChangesAlert">
              <i class="fa fa-exclamation-circle" /> Unsaved Changes
      </span>
      <span v-if="!isDirty && showSavedMsg" class="text-success ml-2" data-cy="settingsSavedAlert">
        <i class="fa fa-check" />
        Settings Updated!
      </span>
    </template>
  </Card>
</template>

<style scoped></style>

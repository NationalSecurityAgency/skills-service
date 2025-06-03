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
import SettingsButton from '@/components/header/SettingsButton.vue'
import HelpButton from '@/components/header/HelpButton.vue'
import SkillsBreadcrumb from '@/components/header/SkillsBreadcrumb.vue'
import { usePagePath } from '@/components/utils/UsePageLocation.js'
import InceptionButton from '@/components/inception /InceptionButton.vue'
import SkipToContent from '@/components/header/SkipToContent.vue'
import { useAppInfoState } from '@/stores/UseAppInfoState.js'
import { useThemesHelper } from '@/components/header/UseThemesHelper.js'
import SkillTreeHeaderSvgIcon from '@/components/brand/SkillTreeHeaderSvgIcon.vue'
import UpgradeInProgressWarning from '@/components/header/UpgradeInProgressWarning.vue'
import NotificationButton from "@/components/header/NotificationButton.vue";

const pathPath = usePagePath()
const appInfoState = useAppInfoState()
const themeHelper = useThemesHelper()


</script>

<template>
  <div class="header">
    <skip-to-content></skip-to-content>
    <div class="text-primary bg-primary-contrast">
      <upgrade-in-progress-warning />
      <div class="flex flex-wrap pt-4 px-4 pb-2 justify-center mb-4 border-b border-surface-200 dark:border-surface-600">
        <div class="flex-1 justify-center">
          <div class="flex">
            <router-link data-cy="skillTreeLogo" class="" to="/">
              <SkillTreeHeaderSvgIcon />
            </router-link>
            <div v-if="pathPath.isAdminPage.value"
                 ref="adminStamp"
                 class="skills-stamp"
                 :class="{
                   'skills-stamp-color-light-theme': !themeHelper.isDarkTheme,
                   'skills-stamp-color-dark-theme': themeHelper.isDarkTheme,
                 }"
            >ADMIN</div>
          </div>
        </div>
        <div class="flex-none">
          <div v-if="!appInfoState.showUa" class="flex flex-row">
            <inception-button v-if="pathPath.isAdminPage.value"
                              class="mr-2"
                              data-cy="inception-button" />
            <notification-button data-cy="notification-button" />
            <settings-button data-cy="settings-button" class="ml-2" />
            <help-button data-cy="help-button" class="ml-2" />
          </div>
        </div>
      </div>
    </div>
    <skills-breadcrumb class="px-4"></skills-breadcrumb>
  </div>
</template>

<style scoped>

.skills-stamp {
  margin-left: 0.5rem;

  border: 2px solid transparent;
  border-radius: 4px;
  display: inline-block;
  padding: 8px 2px 0px 2px;
  line-height: 18px;
  font-size: 18px;
  font-family: 'Black Ops One', cursive;
  text-transform: uppercase;
  text-align: center;
  opacity: 0.8;
  width: 110px;
  height: 40px;
  transform: rotate(-15deg);
}

.skills-stamp-color-light-theme {
  box-shadow:
    0 0 0 3px #8b6d6d,
    0 0 0 2px #8b6d6d inset;
  color: #722b2b;
}

.skills-stamp-color-dark-theme {
  box-shadow:
    0 0 0 3px #E76F50,
    0 0 0 2px #F3A161 inset;
  color: #d9f8f4;
}

@media (max-width: 675px) {
  .skills-stamp {
    max-width: 9rem;
    line-height: 12px;
    font-size: 14px;
    width: 85px;
  }
}

@media (max-width: 563px) {
  .skills-stamp {
    max-width: 9rem;
    line-height: 12px;
    font-size: 14px;
    width: 85px;
  }
}

.skip-main {
  position: absolute !important;
  overflow: hidden !important;
  z-index: -999 !important;
}

.skip-main:focus,
.skip-main:active {
  left: 5px !important;
  top: 5px !important;
  font-size: 1.2em !important;
  z-index: 999 !important;
}
</style>

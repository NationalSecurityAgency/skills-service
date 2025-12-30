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
import { ref, onMounted } from 'vue';
import PageHeader from '../utils/pages/PageHeader.vue';
import Navigation from '../utils/Navigation.vue';
import SettingsService from './SettingsService.js';
import { useAppConfig } from '@/common-components/stores/UseAppConfig.js'

const appConfig = useAppConfig()
const navItems = ref([
  { name: 'Profile', iconClass: 'fa-address-card skills-color-profile', page: 'GeneralSettings' },
  { name: 'Preferences', iconClass: 'fa-user-cog skills-color-preferences', page: 'Preferences' },
]);

const isLoading = ref(true);

const loadSettings = () => {
  SettingsService.hasRoot()
      .then((response) => {
        const isRoot = response;
        if (isRoot) {
          navItems.value.push(
            { name: 'Security', iconClass: 'fa-lock skills-color-security', page: 'SecuritySettings' },
            { name: 'Email', iconClass: 'fa-at skills-color-email', page: 'EmailSettings' },
            { name: 'System', iconClass: 'fa-wrench skills-color-system', page: 'SystemSettings' },
          );
          if (appConfig.enableOpenAIIntegration) {
            navItems.value.push(
              { name: 'AI Prompts', iconClass: 'fa-solid fa-wand-magic-sparkles', page: 'AiPromptSettings' },
            );
          }
        }
      })
      .finally(() => {
        isLoading.value = false;
      });
};

onMounted(() => {
  loadSettings();
})
</script>

<template>
  <PageHeader :loading="isLoading"
              :options="{title: 'Settings', icon: 'fas fa-cog skills-color-settings', subTitle: 'Dashboard settings'}" />
  <Navigation :nav-items="navItems"/>
</template>

<style scoped></style>

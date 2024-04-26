<script setup>
import { ref, onMounted } from 'vue';
import PageHeader from '../utils/pages/PageHeader.vue';
import Navigation from '../utils/Navigation.vue';
import SettingsService from './SettingsService.js';


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

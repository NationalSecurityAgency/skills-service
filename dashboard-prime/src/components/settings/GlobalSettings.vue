<script setup>
import { ref, onMounted } from 'vue';
import PageHeader from '../utils/pages/PageHeader.vue';
import Navigation from '../utils/Navigation.vue';
import SettingsService from './SettingsService.js';


const navItems = ref([
  { label: 'Profile', icon: 'fa-address-card skills-color-profile', route: '/settings' },
  { label: 'Preferences', icon: 'pi pi-cog skills-color-preferences', route: '/settings/preferences' },
]);

let isRoot = false;
let isLoading = true;

const loadSettings = () => {
  SettingsService.hasRoot()
      .then((response) => {
        isRoot = response;
        if (isRoot) {
          navItems.value.push(
              { label: 'Security', icon: 'fa-lock skills-color-security', route: '/settings/security' },
              { label: 'Email', icon: 'fa-at skills-color-email', route: '/settings/email' },
              { label: 'System', icon: 'fa-wrench skills-color-system', route: '/settings/system' },
          );
        }
      })
      .finally(() => {
        isLoading = false;
      });
};

onMounted(() => {
  loadSettings();
})
</script>

<template>
  <PageHeader :loading="false" :options="{title: 'Settings', icon: 'pi pi-cog skills-color-settings', subTitle: 'Dashboard settings'}" />
  <Navigation :nav-items="navItems"/>
</template>

<style scoped></style>

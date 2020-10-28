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
    <page-header :loading="isLoading"
                 :options="{title: 'Settings', icon: 'fas fa-cog text-cyan', subTitle: 'Dashboard settings'}"/>

    <navigation :nav-items="navItems"/>
  </div>
</template>

<script>
  import Navigation from '../utils/Navigation';
  import SettingsService from './SettingsService';
  import PageHeader from '../utils/pages/PageHeader';

  export default {
    name: 'GlobalSettings',
    components: {
      PageHeader,
      Navigation,
    },
    data() {
      return {
        isLoading: true,
        isRoot: false,
        navItems: [{ name: 'Profile', iconClass: 'fa-address-card text-success', page: 'GeneralSettings' }],
      };
    },
    mounted() {
      this.loadSettings();
    },
    methods: {
      loadSettings() {
        SettingsService.hasRoot()
          .then((response) => {
            this.isRoot = response;
            if (this.isRoot) {
              this.navItems.push(
                { name: 'Security', iconClass: 'fa-lock text-warning', page: 'SecuritySettings' },
                { name: 'Email', iconClass: 'fa-at text-blue', page: 'EmailSettings' },
                { name: 'System', iconClass: 'fa-wrench text-secondary', page: 'SystemSettings' },
              );
            }
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

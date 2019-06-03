<template>
  <div>
    <page-header :loading="isLoading"
                 :options="{title: 'Settings', icon: 'fas fa-cog', subTitle: 'Dashboard settings'}"/>

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
        navItems: [{ name: 'General', iconClass: 'fa-address-card', page: 'GeneralSettings' }],
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
                { name: 'Security', iconClass: 'fa-lock', page: 'SecuritySettings' },
                { name: 'Email', iconClass: 'fa-at', page: 'EmailSettings' },
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

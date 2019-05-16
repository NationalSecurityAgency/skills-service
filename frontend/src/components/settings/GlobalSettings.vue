<template>
  <div>
    <page-header :loading="isLoading"
                 :options="{title: 'Settings', icon: 'fas fa-cog', subTitle: 'Dashboard settings'}"/>

    <section class="section">
      <navigation :nav-items="navItems">
        <template slot="General">
          <general-settings/>
        </template>
        <template slot="Security">
          <security-settings :is-root="isRoot"/>
        </template>
        <template slot="Email">
          <email-settings :is-root="isRoot"/>
        </template>
      </navigation>
    </section>
  </div>
</template>

<script>
  import Navigation from '../utils/Navigation';
  import GeneralSettings from './GeneralSettings';
  import SecuritySettings from './SecuritySettings';
  import EmailSettings from './EmailSettings';
  import SettingsService from './SettingsService';
  import PageHeader from '../utils/pages/PageHeader';

  export default {
    name: 'GlobalSettings',
    components: {
      PageHeader,
      EmailSettings,
      SecuritySettings,
      GeneralSettings,
      Navigation,
    },
    data() {
      return {
        isLoading: true,
        isRoot: false,
        navItems: [{ name: 'General', iconClass: 'fa-address-card' }],
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
                { name: 'Security', iconClass: 'fa-lock' },
                { name: 'Email', iconClass: 'fa-at' },
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

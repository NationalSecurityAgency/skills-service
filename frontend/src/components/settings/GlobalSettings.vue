<template>
  <div>
    <section class="section">
      <loading-container v-bind:is-loading="isLoading">

        <div class="columns has-text-left">
          <div class="column">
            <div class="subject-title">
              <h1 class="title"><i class="fas fa-list-alt has-text-link"/> SETTINGS</h1>
            </div>
          </div>
        </div>
      </loading-container>
    </section>
    <section class="section">
      <navigation :nav-items="[
        {name: 'General', iconClass: 'fa-address-card'},
        {name: 'Security', iconClass: 'fa-lock'},
        {name: 'Email', iconClass: 'fa-at'}
      ]">
        <template slot="General">
          <section>
            <general-settings/>
          </section>
        </template>
        <template slot="Security">
          <section>
            <security-settings/>
          </section>
        </template>
        <template slot="Email">

        </template>
      </navigation>
    </section>
  </div>
</template>

<script>
  import LoadingContainer from '../utils/LoadingContainer';
  import Navigation from '../utils/Navigation';
  import GeneralSettings from './GeneralSettings';
  import SecuritySettings from './SecuritySettings';

  export default {
    name: 'GlobalSettings',
    components: {
      SecuritySettings, GeneralSettings, Navigation, LoadingContainer,
    },
    data() {
      return {
        isLoading: true,
      };
    },
    mounted() {
      this.loadSettings();
    },
    methods: {
      loadSettings() {
        this.isLoading = false;
      },
    },
  };
</script>

<style scoped>
  .section {
    padding: 2rem 1.5rem;
  }
</style>

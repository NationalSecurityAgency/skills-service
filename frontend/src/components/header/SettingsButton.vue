<template>
  <b-dropdown right variant="outline-info">
    <template slot="button-content">
      <i class="fas fa-user-astronaut pr-1"/>
      <span class="d-inline-block text-truncate" style="max-width: 12rem; vertical-align: top">{{ displayName }}</span>
    </template>
    <b-dropdown-item href="#"  @click="gotoSettings">
      <span class="text-info"> <i class="fas fa-cog" style="width: 1.5rem;"/>Settings</span>
    </b-dropdown-item>
    <b-dropdown-item v-if="isJwtAuthenticatedUser" href="#" @click="signOut">
      <span class="text-info"> <i class="fas fa-sign-out-alt" style="width: 1.5rem;"/>Log Out</span>
    </b-dropdown-item>
  </b-dropdown>
</template>

<script>
  export default {
    name: 'SettingsButton',
    data() {
      return {
        userInfoLoaded: false,
      };
    },
    computed: {
      userInfo() {
        return this.$store.getters.userInfo;
      },
      isAuthenticatedUser() {
        return this.$store.getters.isAuthenticated;
      },
      isJwtAuthenticatedUser() {
        return this.isAuthenticatedUser && !this.$store.getters.isPkiAuthenticated;
      },
      displayName() {
        const { userInfo } = this.$store.getters;
        let displayName = userInfo.nickname;
        if (!displayName) {
          displayName = `${userInfo.first} ${userInfo.last}`;
        }
        return displayName;
      },
    },
    methods: {
      gotoSettings() {
        this.$router.push({ name: 'GeneralSettings' });
      },
      signOut() {
        this.$store.dispatch('logout');
      },
    },
  };
</script>

<style scoped>


</style>

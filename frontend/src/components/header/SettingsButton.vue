<template>
  <b-dropdown right variant="outline-info">
    <template slot="button-content">
      <i class="fas fa-user-astronaut pr-1"/> {{ userInfo.first }} {{ userInfo.last }}
    </template>
    <b-dropdown-item href="#"  @click="gotoSettings">
      <span class="text-info"> <i class="fas fa-cog" style="width: 1.5rem;"/>Settings</span>
    </b-dropdown-item>
    <b-dropdown-item v-if="isJwtAuthenticatedUser" href="#" @click="signOut">
      <span class="text-info"> <i class="fas fa-sign-out-alt" style="width: 1.5rem;"/>Log Out</span>
    </b-dropdown-item>
  </b-dropdown>

<!--    <button class="btn btn-outline-info" v-b-tooltip.hover.bottom="toolTipText" @click="gotoSettings">-->
<!--      <i class="fas fa-user-astronaut pr-1"/>-->
<!--      <span v-if="isAuthenticatedUser">{{ userInfo.first }} {{ userInfo.last }}</span>-->
<!--      <span v-else>Hello, Sign In</span>-->
<!--    </button>-->
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
    },
    methods: {
      gotoSettings() {
        this.$router.push({ name: 'Settings', params: { settingsCategory: 'general' } });
      },
      signOut() {
        this.$store.dispatch('logout');
      },
    },
  };
</script>

<style scoped>


</style>

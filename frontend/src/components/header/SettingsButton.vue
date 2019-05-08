<template>
    <button class="btn btn-outline-info" v-b-tooltip.hover.bottom="toolTipText" @click="gotoSettings">
      <i class="fas fa-user-astronaut pr-1"/>
      <span v-if="isAuthenticatedUser">{{ userInfo.first }} {{ userInfo.last }}</span>
      <span v-else>Hello, Sign In</span>
    </button>
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
      toolTipText() {
        let text = 'Hello, Please Sign In Below';
        if (this.userInfo) {
          text = `Logged in as '${this.userInfo.first} ${this.userInfo.last}'`;
        }
        return text;
      },
      userInfo() {
        return this.$store.getters.userInfo;
      },
      isAuthenticatedUser() {
        return this.$store.getters.isAuthenticated;
      },
    },
    methods: {
      gotoSettings() {
        this.$router.push({ name: 'Settings', params: { settingsCategory: 'general' } });
      },
    },
  };
</script>

<style scoped>


</style>

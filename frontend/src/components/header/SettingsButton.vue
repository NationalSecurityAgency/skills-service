<template>
  <b-tooltip :label="toolTipText"
             position="is-bottom" animanted="true" type="is-light">
    <a class="button is-outlined">
      <span class="icon is-small"><i class="fas fa-user-astronaut"/></span>
      <span v-if="isAuthenticatedUser" class="pad-left settings">{{ userInfo.first }} {{ userInfo.last }}</span>
      <span v-else class="pad-left settings">{{ 'Hello, Sign In' }}</span>
    </a>
  </b-tooltip>
</template>

<script>
  export default {
    name: 'SettingsButton',
    data() {
      return {
        userInfoLoaded: false,
        serverErrors: [],
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
  };
</script>

<style scoped>
  .settings {
    width: 6em;
    overflow: hidden;
    text-overflow: ellipsis;
  }
</style>

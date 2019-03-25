<template>
  <b-tooltip label="Settings"
             position="is-bottom" animanted="true" type="is-light">
    <button class="button is-outlined" @click="gotoSettings">
      <span class="icon is-small">
        <i class="fas fa-cog"/>
      </span>
    </button>
  </b-tooltip>
</template>

<script>
  export default {
    name: 'GlobalSettingsButton',
    methods: {
      gotoSettings() {
        this.$router.push({ name: 'Settings', params: { settingsCategory: 'general' } });
      },
    },
  };
</script>

<style scoped>

</style>

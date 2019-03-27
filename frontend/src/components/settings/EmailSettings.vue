<template>
  <div class="settings-div">
    <div class="columns skills-underline-container">
      <div class="column">
        <span class="title">Email Settings</span>
      </div>
    </div>
    <email-server-settings v-if="isRoot"/>
  </div>
</template>

<script>
  import EmailServerSettings from './EmailServerSettings';

  export default {
    name: 'EmailSettings',
    components: { EmailServerSettings },
    props: {
      isRoot: {
        type: Boolean,
        default: false,
      },
    },
  };
</script>

<style scoped>

</style>

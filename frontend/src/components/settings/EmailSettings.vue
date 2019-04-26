<template>
  <div>
    <sub-page-header title="Email Settings"/>
    <email-server-settings v-if="isRoot"/>
  </div>
</template>

<script>
  import EmailServerSettings from './EmailServerSettings';
  import SubPageHeader from '../utils/pages/SubPageHeader';

  export default {
    name: 'EmailSettings',
    components: { SubPageHeader, EmailServerSettings },
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

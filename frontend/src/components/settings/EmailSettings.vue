<template>
  <div>
    <sub-page-header title="Email Settings"/>
    <div class="card">
      <div class="card-header">Email Connection Settings</div>
      <div class="card-body">
        <email-server-settings />
      </div>
    </div>
  </div>
</template>

<script>
  import EmailServerSettings from './EmailServerSettings';
  import SubPageHeader from '../utils/pages/SubPageHeader';

  export default {
    name: 'EmailSettings',
    components: { SubPageHeader, EmailServerSettings },
  };
</script>

<style scoped>

</style>

<template>
  <div>
    <div class="columns skills-underline-container">
      <div class="column">
        <span class="title">Access Settings</span>
      </div>
    </div>
    <role-manager :project="project"/>
    <trusted-client-props :project="project"/>
  </div>
</template>

<script>
  import RoleManager from './RoleManager';
  import TrustedClientProps from './TrustedClientProps';

  export default {
    name: 'AccessSettings',
    components: { RoleManager, TrustedClientProps },
    props: ['project'],
  };
</script>

<style scoped>

</style>

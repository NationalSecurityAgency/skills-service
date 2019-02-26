<template>
  <div>
    <div class="columns skills-underline-container">
      <div class="column">
        <span class="title">Access Settings</span>
      </div>
    </div>
    <user-roles :project="project"/>
    <trusted-client-props :project="project"/>
  </div>
</template>

<script>
  import UserRoles from './UserRoles';
  import TrustedClientProps from './TrustedClientProps';

  export default {
    name: 'AccessSettings',
    components: { UserRoles, TrustedClientProps },
    props: ['project'],
  };
</script>

<style scoped>

</style>

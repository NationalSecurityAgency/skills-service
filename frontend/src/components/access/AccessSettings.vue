<template>
  <div>
    <div class="columns">
      <div class="column">
        <span class="title">Access Settings</span>
      </div>
    </div>
    <user-roles :project="project"/>
    <allowed-origins :project="project"/>
  </div>
</template>

<script>
  import UserRoles from './UserRoles';
  import AllowedOrigins from './AllowedOrigins';

  export default {
    name: 'AccessSettings',
    components: { UserRoles, AllowedOrigins },
    props: ['project'],
  };
</script>

<style scoped>

</style>

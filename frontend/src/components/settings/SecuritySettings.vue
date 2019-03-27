<template>
  <div class="settings-div">
    <div class="columns skills-underline-container">
      <div class="column">
        <span class="title">Security Settings</span>
      </div>
    </div>
    <div v-if="isRoot">
      <span class="subtitle">Root Users</span>
      <role-manager :role="role" :user-type="userType" :role-description="roleDescription" />
    </div>
  </div>
</template>

<script>
  import RoleManager from '../access/RoleManager';

  export default {
    name: 'SecuritySettings',
    components: { RoleManager },
    props: {
      isRoot: {
        type: Boolean,
        default: false,
      },
    },
    data() {
      return {
        role: 'ROLE_SUPER_DUPER_ADMIN',
        roleDescription: 'Root User',
        userType: 'ROOT',
      };
    },
  };
</script>

<style scoped>

</style>

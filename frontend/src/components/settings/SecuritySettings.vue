<template>
  <div>
    <sub-page-header title="Security Settings"/>

    <div v-if="isRoot" class="card">
      <div class="card-header">Root Users Management</div>
      <div class="card-body">
        <role-manager :role="role" :user-type="userType" :role-description="roleDescription" />
      </div>
    </div>
  </div>
</template>

<script>
  import RoleManager from '../access/RoleManager';
  import SubPageHeader from '../utils/pages/SubPageHeader';

  export default {
    name: 'SecuritySettings',
    components: { SubPageHeader, RoleManager },
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

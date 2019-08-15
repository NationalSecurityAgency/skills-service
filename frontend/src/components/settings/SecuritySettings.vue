<template>
  <div>
    <sub-page-header title="Security Settings"/>

    <div class="card">
      <div class="card-header">Root Users Management</div>
      <div class="card-body">
        <role-manager :role="root.role" :user-type="root.userType" :role-description="root.roleDescription" />
      </div>
    </div>

    <div class="card mt-2">
      <div class="card-header">Supervisor Users Management</div>
      <div class="card-body">
        <role-manager :role="supervisor.role" :user-type="supervisor.userType" :role-description="supervisor.roleDescription" />
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
    data() {
      return {
        root: {
          role: 'ROLE_SUPER_DUPER_USER',
          roleDescription: 'Root User',
          userType: 'ROOT',
        },
        supervisor: {
          role: 'ROLE_SUPERVISOR',
          roleDescription: 'Supervisor User',
          userType: 'SUPERVISOR',
        },
      };
    },
  };
</script>

<style scoped>

</style>

<template>
  <div>
    <sub-page-header title="Access Settings"/>
    <div class="card">
      <div class="card-header">
        Project Administrator
      </div>
      <div class="card-body">
        <role-manager :project="project"/>
      </div>
    </div>

    <trusted-client-props :project="project" class="my-4"/>
  </div>
</template>

<script>
  import RoleManager from './RoleManager';
  import TrustedClientProps from './TrustedClientProps';
  import SubPageHeader from '../utils/pages/SubPageHeader';

  export default {
    name: 'AccessSettings',
    components: { SubPageHeader, RoleManager, TrustedClientProps },
    props: ['project'],
  };
</script>

<style scoped>

</style>

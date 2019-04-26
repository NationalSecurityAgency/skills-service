<template>
  <div>
    <sub-page-header title="Access Settings"/>
    <div class="card mb-2">
      <div class="card-body">
        <role-manager :project="project"/>
      </div>
    </div>

    <trusted-client-props :project="project"/>
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

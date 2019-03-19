<template>
  <div>
    <div class="columns skills-underline-container">
      <div class="column">
        <span class="title">Access Settings</span>
      </div>
    </div>
    <project-admins :project="project"/>
    <trusted-client-props :project="project"/>
  </div>
</template>

<script>
  import ProjectAdmins from './ProjectAdmins';
  import TrustedClientProps from './TrustedClientProps';

  export default {
    name: 'AccessSettings',
    components: { ProjectAdmins, TrustedClientProps },
    props: ['project'],
  };
</script>

<style scoped>

</style>

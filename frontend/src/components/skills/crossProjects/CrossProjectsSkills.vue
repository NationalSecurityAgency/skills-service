<template>
  <div id="cross-projects-panel">
    <sub-page-header title="Cross-Project Skills"/>

    <share-skills-with-other-projects :project-id="this.$route.params.projectId"/>

    <shared-skills-from-other-projects :project-id="this.$route.params.projectId" class="my-4"/>
  </div>
</template>

<script>
  import ShareSkillsWithOtherProjects from './ShareSkillsWithOtherProjects';
  import SharedSkillsFromOtherProjects from './SharedSkillsFromOtherProjects';
  import SubPageHeader from '../../utils/pages/SubPageHeader';

  export default {
    name: 'CrossProjectsSkills',
    components: { SubPageHeader, SharedSkillsFromOtherProjects, ShareSkillsWithOtherProjects },
  };
</script>

<style scoped>
</style>

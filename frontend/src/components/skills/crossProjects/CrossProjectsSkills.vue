<template>
  <div id="cross-projects-panel">
    <sub-page-header title="Cross-Project Skills"/>

    <share-skills-with-other-projects :project-id="projectId" class="mb-3"/>

    <shared-skills-from-other-projects :project-id="projectId"/>
  </div>
</template>

<script>
  import ShareSkillsWithOtherProjects from './ShareSkillsWithOtherProjects';
  import SharedSkillsFromOtherProjects from './SharedSkillsFromOtherProjects';
  import SubPageHeader from '../../utils/pages/SubPageHeader';

  export default {
    name: 'CrossProjectsSkills',
    props: ['projectId'],
    components: { SubPageHeader, SharedSkillsFromOtherProjects, ShareSkillsWithOtherProjects },
  };
</script>

<style scoped>
</style>

<template>
  <div id="cross-projects-panel">
    <div class="columns">
      <div class="column is-full">
        <span class="title is-3">Cross Project Skills</span>
      </div>
    </div>

    <share-skills-with-other-projects :project-id="projectId"></share-skills-with-other-projects>

    <shared-skills-from-other-projects :project-id="projectId"></shared-skills-from-other-projects>
  </div>
</template>

<script>
  import ShareSkillsWithOtherProjects from './ShareSkillsWithOtherProjects';
  import SharedSkillsFromOtherProjects from './SharedSkillsFromOtherProjects';

  export default {
    name: 'CrossProjectsSkills',
    props: ['projectId'],
    components: { SharedSkillsFromOtherProjects, ShareSkillsWithOtherProjects },
  };
</script>

<style scoped>
</style>

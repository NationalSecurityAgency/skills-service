<template>
<div>
  <skills-display
    :options="options"
    :version="skillsVersion"/>
</div>
</template>

<script>
  import { SkillsDisplay } from '@skilltree/skills-client-vue';
  import SkillsDisplayOptionsMixin from './SkillsDisplayOptionsMixin';

  export default {
    name: 'MyProjectSkillsPage',
    mixins: [SkillsDisplayOptionsMixin],
    components: {
      SkillsDisplay,
    },
    data() {
      return {
        projectId: this.$route.params.projectId,
        skillsVersion: 0,
      };
    },
  };
</script>

<style scoped>

</style>

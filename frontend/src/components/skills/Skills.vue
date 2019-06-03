<template>
  <div>
    <loading-container v-bind:is-loading="isLoading">
      <skills-table :skills-prop="skills" :is-top-level="true" :project-id="this.$route.params.projectId" :subject-id="this.$route.params.subjectId" v-on:skills-change="skillsChanged"/>
    </loading-container>
  </div>
</template>

<script>
  import LoadingContainer from '../utils/LoadingContainer';
  import SkillsTable from './SkillsTable';
  import SkillsService from './SkillsService';

  export default {
    name: 'Skills',
    components: { SkillsTable, LoadingContainer },
    data() {
      return {
        isLoading: true,
        skills: [],
      };
    },
    mounted() {
      this.loadSkills();
    },
    methods: {
      loadSkills() {
        SkillsService.getSubjectSkills(this.$route.params.projectId, this.$route.params.subjectId)
          .then((skills) => {
            this.isLoading = false;
            const loadedSkills = skills;
            this.skills = loadedSkills.map((loadedSkill) => {
              const copy = Object.assign({}, loadedSkill);
              copy.created = window.moment(loadedSkill.created);
              return copy;
            });
          });
      },
      skillsChanged(skillId) {
        this.$emit('skills-change', skillId);
      },
    },
  };
</script>

<style scoped>
</style>

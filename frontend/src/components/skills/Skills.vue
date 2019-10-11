<template>
  <div>
    <loading-container v-bind:is-loading="isLoading">
      <skills-table :skills-prop="skills" :is-top-level="true" :project-id="this.$route.params.projectId" :subject-id="this.$route.params.subjectId" v-on:skills-change="skillsChanged"/>
    </loading-container>
  </div>
</template>

<script>
  import { createNamespacedHelpers } from 'vuex';
  import LoadingContainer from '../utils/LoadingContainer';
  import SkillsTable from './SkillsTable';
  import SkillsService from './SkillsService';

  const { mapActions } = createNamespacedHelpers('subjects');

  export default {
    name: 'Skills',
    components: { SkillsTable, LoadingContainer },
    data() {
      return {
        isLoading: true,
        skills: [],
        projectId: null,
        subjectId: null,
      };
    },
    mounted() {
      this.projectId = this.$route.params.projectId;
      this.subjectId = this.$route.params.subjectId;
      this.loadSkills();
    },
    methods: {
      ...mapActions([
        'loadSubjectDetailsState',
      ]),
      loadSkills() {
        SkillsService.getSubjectSkills(this.projectId, this.subjectId)
          .then((skills) => {
            const loadedSkills = skills;
            this.skills = loadedSkills.map((loadedSkill) => {
              const copy = Object.assign({}, loadedSkill);
              copy.created = window.moment(loadedSkill.created);
              return copy;
            });
          })
          .finally(() => {
            this.isLoading = false;
          });
      },
      skillsChanged(skillId) {
        this.loadSubjectDetailsState({ projectId: this.projectId, subjectId: this.subjectId });
        this.$emit('skills-change', skillId);
      },
    },
  };
</script>

<style scoped>
</style>

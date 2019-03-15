<template>
  <div>
    <loading-container v-bind:is-loading="isLoading">
      <skills-table :skills-prop="skills" :is-top-level="true" :project-id="projectId" :subject-id="subjectId" v-on:skills-change="skillsChanged">
        <div slot="skillsTableTitle">
          <h1 class="title is-3">Skills</h1>
        </div>
      </skills-table>
    </loading-container>
  </div>
</template>

<script>
  import LoadingContainer from '../utils/LoadingContainer';
  import SkillsTable from './SkillsTable';
  import SkillsService from './SkillsService';

  export default {
    name: 'Skills',
    props: ['projectId', 'subjectId'],
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
        SkillsService.getSubjectSkills(this.projectId, this.subjectId)
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


<template>
  <div>
    <sub-page-header title="Overview"/>
    <loading-container :is-loading="isLoading">
      <div class="card">
        <div class="card-body">
          <child-row-skills-display v-if="skill.skillId" :skill="skill"></child-row-skills-display>
        </div>
      </div>
    </loading-container>
  </div>
</template>

<script>
  import SubPageHeader from '../utils/pages/SubPageHeader';
  import ChildRowSkillsDisplay from './ChildRowSkillsDisplay';
  import SkillsService from './SkillsService';
  import LoadingContainer from '../utils/LoadingContainer';

  export default {
    name: 'SkillOverview',
    components: {
      LoadingContainer,
      ChildRowSkillsDisplay,
      SubPageHeader,
    },
    data() {
      return {
        isLoading: true,
        skill: {},
      };
    },
    mounted() {
      SkillsService.getSkillDetails(this.$route.params.projectId, this.$route.params.subjectId, this.$route.params.skillId)
        .then((response) => {
          this.skill = Object.assign(response, { subjectId: this.$route.params.subjectId });
        })
        .finally(() => {
          this.isLoading = false;
        });
    },
  };
</script>

<style scoped>

</style>

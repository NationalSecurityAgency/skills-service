<template>
  <div class="row col-sm-12">
    <div class="col-sm-4 text-right w-85 text-truncate font-weight-bold">
      {{ skill.skill }}
    </div>
    <div class="col-md-8">
      <user-skill-progress :skill="skill" :show-description="showDescription" v-on:progressbar-clicked="skillRowClicked"></user-skill-progress>
    </div>
  </div>
</template>

<script>
  import UserSkillProgress from '@/userSkills/modal/UserSkillProgress.vue';

  export default {
    name: 'UserSkillsSubjectSkillRow',
    components: {
      UserSkillProgress,
    },
    props: {
      skill: Object,
      showDescription: Boolean,
    },
    data() {
      return {
        showSkillDependencyGraph: false,
      };
    },
    computed: {
      locked() {
        return this.skill.dependencyInfo && !this.skill.dependencyInfo.achieved;
      },
    },
    methods: {
      skillRowClicked() {
        // only respond to events if the row is locked and we need to display dependency component
        if (this.locked) {
           // this.showSkillDependencyGraph = true;
          this.$router.push({
            name: 'skillDependencies',
            params: {
              skill: this.skill,
            },
          });
        }
      },
    },
  };
</script>

<style scoped>
</style>

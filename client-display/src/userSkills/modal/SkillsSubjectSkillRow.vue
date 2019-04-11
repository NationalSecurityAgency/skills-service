<template>
  <div class="row col-sm-12">
    <div class="col-sm-4 text-right">
      <small class="d-inline-block text-truncate font-weight-bold">{{ skill.skill }}</small>
    </div>
    <div class="col-md-8">
      <user-skill-progress
        :skill="skill"
        :show-description="showDescription"
        @progressbar-clicked="skillRowClicked" />
    </div>
  </div>
</template>

<script>
  import UserSkillProgress from '@/userSkills/modal/SkillProgress.vue';

  export default {
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

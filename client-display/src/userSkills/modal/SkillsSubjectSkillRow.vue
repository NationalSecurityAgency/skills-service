<template>
  <user-skill-progress :skill="skill" :show-description="showDescription" @progressbar-clicked="skillRowClicked" />
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
    computed: {
      locked() {
        return this.skill.dependencyInfo && !this.skill.dependencyInfo.achieved;
      },
    },
    methods: {
      skillRowClicked() {
        // only respond to events if the row is locked and we need to display dependency component
        if (this.locked) {
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

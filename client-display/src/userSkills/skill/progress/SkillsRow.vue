<template>
  <skill-progress :skill="skill" :show-description="showDescription" @progressbar-clicked="skillRowClicked" />
</template>

<script>
  import SkillProgress from '@/userSkills/skill/progress/SkillProgress.vue';

  export default {
    components: {
      SkillProgress,
    },
    props: {
      skill: Object,
      showDescription: Boolean,
    },
    methods: {
      skillRowClicked() {
        this.$router.push({
          name: 'skillDependencies',
          params: {
            skillId: this.skill.skillId,
          },
        });
      },
    },
  };
</script>

<style scoped>
</style>

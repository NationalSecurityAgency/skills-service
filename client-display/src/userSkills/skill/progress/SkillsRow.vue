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
        const params = { skillId: this.skill.skillId };
        if(this.skill.projectId) {
          console.log(`skill has a projectId of [${this.skill.projectId}], passing as route param`);
          params.projectId = this.skill.projectId;
        }
        this.$router.push({
          name: 'skillDetails',
          params: params,
        });
      },
    },
  };
</script>

<style scoped>
</style>

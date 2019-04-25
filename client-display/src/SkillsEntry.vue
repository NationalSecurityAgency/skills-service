<template>
  <div
    ref="containerElement"
    class="skills-container">
    <user-skills
      v-if="token"
      @height-change="onHeightChange"/>
  </div>
</template>

<script>
  import UserSkills from '@/userSkills/UserSkills.vue';

  export default {
    name: 'SkillsEntry',
    components: {
      UserSkills,
    },
    computed: {
      token() {
        return this.$store.state.authToken;
      },
    },
  };
</script>

<style scoped>

</style>

<template>
  <div
    ref="containerElement"
    class="skills-container">
    <user-skills
      v-if="token" />
  </div>
</template>

<script>
  import UserSkills from '@/userSkills/UserSkills.vue';

  export default {
    name: 'SkillsEntry',
    components: {
      UserSkills,
    },
    props: {
      version: {
        type: Number,
        default: 0,
      },
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

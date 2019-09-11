<template>
  <b-button v-if="isConfigurationInitialized" to="/skills" variant="outline-info">
    <skills-level project-id="Inception" />
<!--    Level {{ level }}-->
  </b-button>
</template>

<script>
  import { SkillsLevel, SkillsConfiguration } from '@skills/skills-client-vue';
  import LevelService from '../levels/LevelService';

  export default {
    name: 'InceptionButton',
    components: {
      SkillsLevel,
    },
    data() {
      return {
        isConfigurationInitialized: false,
      };
    },
    mounted() {
      SkillsConfiguration.afterConfigure()
        .then(() => {
          this.isConfigurationInitialized = true;
        });
    },
    computed: {
      userId() {
        return this.$store.getters.userInfo.userId;
      },
    },
  };
</script>

<style scoped>

</style>

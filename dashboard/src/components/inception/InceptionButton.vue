/*
Copyright 2020 SkillTree

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    https://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
<template>
  <b-button v-if="isConfigurationInitialized" to="/skills" variant="outline-primary" size="sm" aria-label="inception rank button">
    <i class="fas fa-trophy mr-1"></i>
    <skills-level project-id="Inception" />
  </b-button>
</template>

<script>
  import { SkillsLevel, SkillsConfiguration } from '@skilltree/skills-client-vue';

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

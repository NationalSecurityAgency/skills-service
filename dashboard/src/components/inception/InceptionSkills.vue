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
  <div class="inception-container pb-4">
    <skills-display
      :options="options"
      :version="skillsVersion"
      :theme="theme"/>
  </div>
</template>

<script>
  import { SkillsDisplay } from '@skilltree/skills-client-vue';
  import SkillsDisplayOptionsMixin from '../myProgress/SkillsDisplayOptionsMixin';

  export default {
    name: 'InceptionSkills',
    mixins: [SkillsDisplayOptionsMixin],
    components: {
      SkillsDisplay,
    },
    data() {
      return {
        projectId: 'Inception',
        skillsVersion: 0,
        theme: {
          backgroundColor: '#f6f8fa',
          landingPageTitle: 'Dashboard Skills',
          maxWidth: '100%',
          pageTitleTextColor: '#212529',
          pageTitleFontSize: '1.5rem',
          backButton: {
            padding: '5px 10px',
            fontSize: '12px',
            lineHeight: '1.5',
          },
        },
      };
    },
  };
</script>

<style scoped>
.inception-container {
  min-height: calc(100vh - 80px);
  background-color: #f6f8fa;
}
</style>

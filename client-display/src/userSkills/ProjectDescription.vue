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
  <div class="project-description mb-3" data-cy="projectDescription">
    <div class="card">
      <p class="skills-text-description project-description-text text-primary mt-3 pt-3" style="font-size: 0.9rem;">
        <markdown-text v-if="description" :text="description"/>
      </p>
    </div>
  </div>
</template>

<script>
  import MarkdownText from '@/common-components/utilities/MarkdownText';

  export default {
    name: 'ProjectDescription',
    components: { MarkdownText },
    props: {
      description: {
        type: String,
        required: true,
      },
    },
  };

</script>

<style scoped>

</style>

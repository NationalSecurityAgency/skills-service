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
  <loading-container :is-loading="loadingDescription" :data-cy="`projectDescriptionRow_${projectId}`">
    <div class="w-100">
      <markdown-text :text="description" />
    </div>
  </loading-container>
</template>

<script>
  import LoadingContainer from '@/components/utils/LoadingContainer';
  import ProjectService from '@/components/projects/ProjectService';
  import MarkdownText from '@/common-components/utilities/MarkdownText';

  export default {
    name: 'ProjectDescriptionRow',
    components: {
      LoadingContainer,
      MarkdownText,
    },
    props: {
      projectId: {
        type: String,
        required: true,
      },
    },
    data() {
      return {
        loadingDescription: true,
        description: '',
      };
    },
    mounted() {
      this.loadDescription();
    },
    methods: {
      loadDescription() {
        this.loadingDescription = true;
        ProjectService.loadDescription(this.projectId)
          .then((data) => {
            this.description = data.description;
          }).finally(() => {
            this.loadingDescription = false;
          });
      },
    },
  };
</script>

<style scoped>

</style>

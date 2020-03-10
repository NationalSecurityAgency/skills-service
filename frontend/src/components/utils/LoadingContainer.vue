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
  <div class="loading-container">
    <skills-spinner :is-loading="isLoading"/>
    <div v-if="!isLoading">
      <slot></slot>
    </div>
  </div>
</template>

<script>
  import SkillsSpinner from './SkillsSpinner';

  export default {
    name: 'LoadingContainer',
    components: {
      SkillsSpinner,
    },
    props: ['isLoading'],
  };
</script>

<style scoped>
  .loading-container {
    /* need to be relative for the loading bar to look right */
    position: relative;
    min-height: 5rem;
  }
</style>

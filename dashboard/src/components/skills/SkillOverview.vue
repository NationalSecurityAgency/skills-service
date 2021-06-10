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
  <div>
    <sub-page-header title="Overview"/>
    <loading-container :is-loading="loadingSkill">
      <div class="card">
        <div class="card-body">
          <child-row-skills-display v-if="skill && skill.skillId" :skill="skill"></child-row-skills-display>
        </div>
      </div>
    </loading-container>
  </div>
</template>

<script>
  import { createNamespacedHelpers } from 'vuex';
  import SubPageHeader from '../utils/pages/SubPageHeader';
  import ChildRowSkillsDisplay from './ChildRowSkillsDisplay';
  import LoadingContainer from '../utils/LoadingContainer';

  const { mapGetters } = createNamespacedHelpers('skills');

  // edits to id no longer work right, header still not reloaded
  export default {
    name: 'SkillOverview',
    components: {
      LoadingContainer,
      ChildRowSkillsDisplay,
      SubPageHeader,
    },
    computed: {
      ...mapGetters([
        'skill',
        'loadingSkill',
      ]),
    },
  };
</script>

<style scoped>

</style>

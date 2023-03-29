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
    <sub-page-header title="Skills"/>

    <skills-spinner :is-loading="isLoadingData" />
    <b-card body-class="p-0">
      <loading-container v-bind:is-loading="isLoadingData">
        <simple-skills-table v-if="quizSkills && quizSkills.length > 0"
                             :skills="quizSkills" v-on:skill-removed="deleteSkill"></simple-skills-table>

        <no-content2 v-else title="No Skills Yet..." icon="fas fa-award" class="mb-5"
                     message="No skills have been associated with this quiz yet"></no-content2>
      </loading-container>
    </b-card>
  </div>
</template>

<script>
  // import { createNamespacedHelpers } from 'vuex';
  import SubPageHeader from '@/components/utils/pages/SubPageHeader';
  import SkillsSpinner from '@/components/utils/SkillsSpinner';
  import NoContent2 from '@/components/utils/NoContent2';
  // import SkillsService from '@/components/skills/SkillsService';
  // import SkillsSelector2 from '@/components/skills/SkillsSelector2';
  import SimpleSkillsTable from '@/components/skills/SimpleSkillsTable';
  import LoadingContainer from '@/components/utils/LoadingContainer';
  // const { mapGetters } = createNamespacedHelpers('quiz');

  export default {
    name: 'QuizSettings',
    components: {
      SubPageHeader,
      NoContent2,
      SimpleSkillsTable,
      LoadingContainer,
      // SkillsSelector2,
      SkillsSpinner,
    },
    data() {
      return {
        isLoadingData: false,
        quizSkills: [],
      };
    },
    mounted() {

    },
    methods: {
    },
  };
</script>

<style scoped>

</style>

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
    <loading-container v-bind:is-loading="isLoading">
      <skills-table :skills-prop="skills" :is-top-level="true" :project-id="this.$route.params.projectId" :subject-id="this.$route.params.subjectId" v-on:skills-change="skillsChanged"/>
    </loading-container>
  </div>
</template>

<script>
  import { createNamespacedHelpers } from 'vuex';
  import LoadingContainer from '../utils/LoadingContainer';
  import SkillsTable from './SkillsTable';
  import SkillsService from './SkillsService';

  const { mapActions } = createNamespacedHelpers('subjects');

  export default {
    name: 'Skills',
    components: { SkillsTable, LoadingContainer },
    data() {
      return {
        isLoading: true,
        skills: [],
        projectId: null,
        subjectId: null,
      };
    },
    mounted() {
      this.projectId = this.$route.params.projectId;
      this.subjectId = this.$route.params.subjectId;
      this.loadSkills();
    },
    methods: {
      ...mapActions([
        'loadSubjectDetailsState',
      ]),
      loadSkills() {
        SkillsService.getSubjectSkills(this.projectId, this.subjectId)
          .then((skills) => {
            const loadedSkills = skills;
            this.skills = loadedSkills.map((loadedSkill) => {
              const copy = { ...loadedSkill };
              copy.created = window.dayjs(loadedSkill.created);
              return copy;
            });
          })
          .finally(() => {
            this.isLoading = false;
          });
      },
      skillsChanged(skillId) {
        this.loadSubjectDetailsState({ projectId: this.projectId, subjectId: this.subjectId });
        this.$emit('skills-change', skillId);
      },
    },
  };
</script>

<style scoped>
</style>

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
    <sub-page-header title="Overview">
      <b-button @click="displayEdit"
                size="sm"
                variant="outline-primary" :data-cy="`editSkillButton_${this.$route.params.skillId}`"
                :aria-label="'edit Skill '+skill.name" :ref="'edit_'+this.$route.params.skillId">
        <span class="d-none d-sm-inline">Edit </span> <i class="fas fa-edit" aria-hidden="true"/>
      </b-button>
    </sub-page-header>
    <loading-container :is-loading="isLoading">
      <div class="card">
        <div class="card-body">
          <child-row-skills-display v-if="skill.skillId" :skill="skill"></child-row-skills-display>
        </div>
      </div>
    </loading-container>
    <edit-skill v-if="showEdit" v-model="showEdit" :skillId="skill.skillId" :is-copy="false" :is-edit="true"
                :project-id="this.$route.params.projectId" :subject-id="this.$route.params.subjectId" @skill-saved="skillEdited" @hidden="handleHide"/>
  </div>
</template>

<script>
  import SubPageHeader from '../utils/pages/SubPageHeader';
  import ChildRowSkillsDisplay from './ChildRowSkillsDisplay';
  import SkillsService from './SkillsService';
  import LoadingContainer from '../utils/LoadingContainer';
  import EditSkill from './EditSkill';

  export default {
    name: 'SkillOverview',
    components: {
      LoadingContainer,
      ChildRowSkillsDisplay,
      SubPageHeader,
      EditSkill,
    },
    data() {
      return {
        isLoading: true,
        skill: {},
        showEdit: false,
      };
    },
    mounted() {
      SkillsService.getSkillDetails(this.$route.params.projectId, this.$route.params.subjectId, this.$route.params.skillId)
        .then((response) => {
          this.skill = Object.assign(response, { subjectId: this.$route.params.subjectId });
        })
        .finally(() => {
          this.isLoading = false;
        });
    },
    methods: {
      displayEdit() {
        // should only enable edit button if dirty, isn't currently
        this.showEdit = true;
      },
      skillEdited(editedSkil) {
        this.isLoading = true;
        // the page title and breadcrumb aren't updated, how to propegate an update to the page header
        // if the id changed then we'd need to update the route as well
        SkillsService.saveSkill(editedSkil).then((res) => {
          const origId = this.skill.skillId;
          this.skill = Object.assign(res, { subjectId: this.$route.params.subjectId });
          if (origId !== this.skill.skillId) {
            this.$router.replace({ name: this.$route.name, params: { ...this.$route.params, skillId: this.skill.skillId } });
          } else {
            this.$emitter.emit('skillupdated', res);
          }
        }).finally(() => {
          this.isLoading = false;
        });
      },
      handleHide() {
        this.showEdit = false;
        const ref = this.$refs[`edit_${this.$route.params.skillId}`];
        this.$nextTick(() => {
          if (ref) {
            ref.focus();
          }
        });
      },
    },
  };
</script>

<style scoped>

</style>

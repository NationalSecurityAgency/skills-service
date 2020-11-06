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
    <sub-page-header title="Subjects" action="Subject" @add-action="openNewSubjectModal"
                     :disabled="addSubjectDisabled" :disabled-msg="addSubjectsDisabledMsg"
                     :aria-label="'new subject'"/>
    <loading-container v-bind:is-loading="isLoading">
      <div v-if="subjects && subjects.length" class="row justify-content-center">
        <div v-for="(subject) of subjects" :key="subject.subjectId" :id="subject.subjectId" class="col-lg-4 mb-3"
             style="min-width: 23rem;">
          <subject :subject="subject" v-on:subject-deleted="deleteSubject" v-on:move-subject-up="moveSubjectUp"
                   v-on:move-subject-down="moveSubjectDown"/>
        </div>
      </div>

      <no-content2 v-else class="mt-4"
                   title="No Subjects Yet" message="Subjects are a way to group and organize skill definitions within a gameified training profile."></no-content2>
    </loading-container>

    <edit-subject v-if="displayNewSubjectModal" v-model="displayNewSubjectModal" :subject="emptyNewSubject" @subject-saved="subjectAdded"/>
  </div>
</template>

<script>
  import { createNamespacedHelpers } from 'vuex';
  import { SkillsReporter } from '@skilltree/skills-client-vue';
  import Subject from './Subject';
  import EditSubject from './EditSubject';
  import LoadingContainer from '../utils/LoadingContainer';
  import SubjectsService from './SubjectsService';
  import SubPageHeader from '../utils/pages/SubPageHeader';
  import NoContent2 from '../utils/NoContent2';

  const { mapActions } = createNamespacedHelpers('projects');

  export default {
    name: 'Subjects',
    components: {
      NoContent2,
      EditSubject,
      SubPageHeader,
      LoadingContainer,
      Subject,
    },
    data() {
      return {
        isLoading: true,
        subjects: [],
        displayNewSubjectModal: false,
        projectId: null,
      };
    },
    mounted() {
      this.projectId = this.$route.params.projectId;
      this.loadSubjects();
    },
    methods: {
      ...mapActions([
        'loadProjectDetailsState',
      ]),
      openNewSubjectModal() {
        this.displayNewSubjectModal = true;
      },
      loadSubjects() {
        SubjectsService.getSubjects(this.projectId)
          .then((response) => {
            this.subjects = response;
            if (this.subjects.length) {
              this.subjects[0].isFirst = true;
              this.subjects[this.subjects.length - 1].isLast = true;
            }
          })
          .finally(() => {
            this.isLoading = false;
          });
      },
      deleteSubject(subject) {
        this.isLoading = true;
        SubjectsService.deleteSubject(subject)
          .then(() => {
            this.subjects = this.subjects.filter((item) => item.subjectId !== subject.subjectId);
            this.loadProjectDetailsState({ projectId: this.projectId });
            this.$emit('subjects-changed', subject.subjectId);
          })
          .finally(() => {
            this.isLoading = false;
          });
      },
      subjectAdded(subject) {
        this.displayNewSubjectModal = false;
        this.isLoading = true;
        SubjectsService.saveSubject(subject)
          .then(() => {
            this.loadSubjects();
            this.loadProjectDetailsState({ projectId: this.projectId });
            this.$emit('subjects-changed', subject.subjectId);
            SkillsReporter.reportSkill('CreateSubject');
          });
      },
      moveSubjectDown(subject) {
        this.moveSubject(subject, 'DisplayOrderDown');
      },
      moveSubjectUp(subject) {
        this.moveSubject(subject, 'DisplayOrderUp');
      },
      moveSubject(subject, actionToSubmit) {
        this.isLoading = true;
        SubjectsService.patchSubject(subject, actionToSubmit)
          .then(() => {
            this.loadSubjects();
          });
      },
    },
    computed: {
      emptyNewSubject() {
        return {
          projectId: this.$route.params.projectId,
          name: '',
          subjectId: '',
          description: '',
          iconClass: 'fas fa-book',
        };
      },
      addSubjectDisabled() {
        return this.subjects && this.$store.getters.config && this.subjects.length >= this.$store.getters.config.maxSubjectsPerProject;
      },
      addSubjectsDisabledMsg() {
        if (this.$store.getters.config) {
          return `The maximum number of Subjects allowed is ${this.$store.getters.config.maxSubjectsPerProject}`;
        }
        return '';
      },
    },
  };
</script>

<style scoped>

  .no-subjects {
    color: #3f5971;
  }

</style>

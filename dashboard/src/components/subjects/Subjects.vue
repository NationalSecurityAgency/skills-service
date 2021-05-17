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
    <sub-page-header ref="subPageHeader" title="Subjects">
      <b-button @click="displayEditProject"
                ref="editProjectButton"
                class="btn btn-outline-primary mr-1"
                size="sm"
                variant="outline-primary"
                data-cy="btn_edit-project"
                :aria-label="'edit Project '+project.projectId">
        <span class="d-none d-sm-inline">Edit </span> <i class="fas fa-edit" aria-hidden="true"/>
      </b-button>
      <b-button ref="actionButton" type="button" size="sm" variant="outline-primary"
                :class="{'btn':true, 'btn-outline-primary':true, 'disabled':addSubjectDisabled}"
                v-on:click="openNewSubjectModal" :aria-label="'new subject'"
                :data-cy="`btn_Subjects`">
        <span class="d-none d-sm-inline">Skill </span> <i class="fas fa-plus-circle"/>
      </b-button>
      <i v-if="addSubjectDisabled" class="fas fa-exclamation-circle text-warning ml-1" style="pointer-events: all; font-size: 1.5rem;" v-b-tooltip.hover="addSubjectsDisabledMsg"/>
    </sub-page-header>
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

    <edit-subject v-if="displayNewSubjectModal" v-model="displayNewSubjectModal"
                  :subject="emptyNewSubject" @subject-saved="subjectAdded"
                  @hidden="handleHide"/>

    <edit-project v-if="editProject" v-model="editProject" :project="project" :is-edit="true"
                  @project-saved="projectSaved" @hidden="editProjectHidden"/>

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
  import EditProject from '../projects/EditProject';
  import ProjectService from '../projects/ProjectService';

  const { mapGetters, mapActions, mapMutations } = createNamespacedHelpers('projects');

  export default {
    name: 'Subjects',
    components: {
      NoContent2,
      EditSubject,
      SubPageHeader,
      LoadingContainer,
      Subject,
      EditProject,
    },
    data() {
      return {
        isLoading: true,
        subjects: [],
        displayNewSubjectModal: false,
        projectId: null,
        editProject: false,
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
      ...mapMutations(['setProject']),
      displayEditProject() {
        this.editProject = true;
      },
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
            this.handleFocus();
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
      handleHide(e) {
        if (!e || !e.update) {
          this.handleFocus();
        }
      },
      handleFocus() {
        this.$nextTick(() => {
          this.$refs.actionButton.focus();
        });
      },
      projectSaved(updatedProject) {
        ProjectService.saveProject(updatedProject).then((resp) => {
          const origProjId = this.project.projectId;
          this.setProject(resp);
          if (resp.projectId !== origProjId) {
            this.$router.replace({ name: this.$route.name, params: { ...this.$route.params, projectId: resp.projectId } });
            this.projectId = resp.projectId;
            this.loadSubjects();
          }
        });
      },
      editProjectHidden() {
        this.editProject = false;
        const ref = this.$refs.editProjectButton;
        this.$nextTick(() => {
          if (ref) {
            ref.focus();
          }
        });
      },
    },
    computed: {
      ...mapGetters(['project']),
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

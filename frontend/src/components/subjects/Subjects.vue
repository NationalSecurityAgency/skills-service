<template>
  <div>
    <sub-page-header title="Subjects" action="Subject" @add-action="openNewSubjectModal"/>
    <loading-container v-bind:is-loading="isLoading">
      <div v-if="subjects && subjects.length" class="row justify-content-center">
        <div v-for="(subject) of subjects" :key="subject.subjectId" :id="subject.subjectId" class="col-lg-4 mb-3"
             style="min-width: 23rem;">
          <subject :subject="subject" v-on:subject-deleted="deleteSubject" v-on:move-subject-up="moveSubjectUp"
                   v-on:move-subject-down="moveSubjectDown"/>
        </div>
      </div>

      <no-content3 v-if="!subjects || subjects.length==0"
                   title="No Subjects Yet" sub-title="Create a project to get started!"></no-content3>
    </loading-container>

    <edit-subject v-if="displayNewSubjectModal" v-model="displayNewSubjectModal" :subject="emptyNewSubject" @subject-saved="subjectAdded"/>
  </div>
</template>

<script>
  import { createNamespacedHelpers } from 'vuex';
  import { SkillsReporter } from '@skills/skills-client-vue';
  import Subject from './Subject';
  import EditSubject from './EditSubject';
  import LoadingContainer from '../utils/LoadingContainer';
  import SubjectsService from './SubjectsService';
  import NoContent3 from '../utils/NoContent3';
  import SubPageHeader from '../utils/pages/SubPageHeader';

  const { mapActions } = createNamespacedHelpers('projects');

  export default {
    name: 'Subjects',
    components: {
      EditSubject,
      SubPageHeader,
      NoContent3,
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
            this.isLoading = false;
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
            this.subjects = this.subjects.filter(item => item.subjectId !== subject.subjectId);
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
          })
          .finally(() => {
            this.isLoading = false;
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
          })
          .finally(() => {
            this.isLoading = false;
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
    },
  };
</script>

<style scoped>

</style>

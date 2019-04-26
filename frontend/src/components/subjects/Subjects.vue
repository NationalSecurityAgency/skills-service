<template>
  <div>
    <sub-page-header title="Subjects" action="Subject" @add-action="newSubject"/>
    <loading-container v-bind:is-loading="isLoading">
      <div>
        <div v-if="subjects && subjects.length" class="row justify-content-center">
          <div v-for="(subject) of subjects" :key="subject.id" class="col-lg-4 mb-3" style="min-width: 23rem;">
            <subject :subject="subject" v-on:subject-deleted="subjectRemoved" v-on:move-subject-up="moveSubjectUp"
                     v-on:move-subject-down="moveSubjectDown"/>
          </div>

        </div>

        <no-content3 v-if="!subjects || subjects.length==0"
          title="No Subjects Yet" sub-title="Create a project to get started!"></no-content3>
      </div>
    </loading-container>
  </div>
</template>

<script>
  import Subject from './Subject';
  import EditSubject from './EditSubject';
  import LoadingContainer from '../utils/LoadingContainer';
  import SubjectsService from './SubjectsService';
  import NoContent3 from '../utils/NoContent3';
  import SubPageHeader from '../utils/pages/SubPageHeader';

  export default {
    name: 'Subjects',
    components: {
      SubPageHeader,
      NoContent3,
      LoadingContainer,
      Subject,
    },
    props: ['project'],
    data() {
      return {
        isLoading: true,
        subjects: [],
      };
    },
    mounted() {
      this.loadSubjects();
    },
    methods: {
      loadSubjects() {
        SubjectsService.getSubjects(this.project.projectId)
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
      subjectRemoved(subject) {
        this.subjects = this.subjects.filter(item => item.id !== subject.id);
        this.$emit('subjects-changed', subject.subjectId);
      },
      subjectAdded(subject) {
        this.isLoading = true;
        SubjectsService.saveSubject(subject)
          .then(() => {
            this.loadSubjects();
            this.$emit('subjects-changed', subject.subjectId);
          })
          .finally(() => {
            this.isLoading = false;
          });
      },
      newSubject() {
        const emptySubject = {
          projectId: this.project.projectId,
          name: '',
          subjectId: '',
          description: '',
          iconClass: 'fab fa-pied-piper-alt',
        };
        this.$modal.open({
          parent: this,
          component: EditSubject,
          hasModalCard: true,
          canCancel: false,
          // width: 1300,
          props: {
            subject: emptySubject,
          },
          events: {
            'subject-created': this.subjectAdded,
          },
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
  };
</script>

<style scoped>

</style>

<template>
  <div>
    <div class="columns skills-underline-container">
      <div class="column">
        <div class="title">Subjects</div>
      </div>
      <div class="column has-text-right">
        <a v-on:click="newSubject" class="button is-outlined is-success">
          <span>Add New Subject</span>
          <span class="icon is-small">
              <i class="fas fa-plus-circle"/>
            </span>
        </a>
      </div>
    </div>

    <loading-container v-bind:is-loading="isLoading">
      <transition name="projectContainer" enter-active-class="animated fadeIn">
        <div>
          <div v-if="subjects && subjects.length" class="columns is-multiline">

            <div v-if="subjects && subjects.length" v-for="(subject) of subjects"
                 :key="subject.id" class="column is-half-tablet is-half-desktop is-one-third-widescreen">
              <subject :subject="subject" v-on:subject-deleted="subjectRemoved" v-on:move-subject-up="moveSubjectUp" v-on:move-subject-down="moveSubjectDown"/>
            </div>

          </div>

        <no-content :should-display="!subjects || subjects.length==0" :title="'No Subjects Yet'">
          <div slot="content" class="content" style="width: 100%;">
            <p class="has-text-centered">
              Create your subject today by pressing
            </p>
            <p class="has-text-centered">
              <a v-on:click="newSubject" class="button is-outlined is-success">
                <span>Add New Subject</span>
                <span class="icon is-small">
              <i class="fas fa-plus-circle"/>
              </span>
              </a>
            </p>
          </div>
        </no-content>
        </div>
      </transition>
    </loading-container>
  </div>
</template>

<script>
  import Subject from './Subject';
  import EditSubject from './EditSubject';
  import LoadingContainer from '../utils/LoadingContainer';
  import NoContent from '../utils/NoContent';
  import SubjectsService from './SubjectsService';

  export default {
    name: 'Subjects',
    components: { NoContent, LoadingContainer, Subject },
    props: ['project'],
    data() {
      return {
        isLoading: true,
        subjects: [],
        serverErrors: [],
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
            this.subjects[0].isFirst = true;
            this.subjects[this.subjects.length - 1].isLast = true;
          })
          .catch((e) => {
            this.isLoading = false;
            this.serverErrors.push(e);
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
          .catch((e) => {
            this.isLoading = false;
            throw e;
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
          .catch((e) => {
            this.isLoading = false;
            throw e;
        });
      },

    },
  };
</script>

<style scoped>

</style>

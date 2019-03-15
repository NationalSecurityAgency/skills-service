<template>
  <div class="box">
    <div class="columns has-text-centered">
      <div class="column is-full">

        <div class="columns">
          <div class="column is-narrow is-vcentered">
            <i class="has-text-info subject-tile-icon skills-icon" v-bind:class="`${subject.iconClass}`"></i>
          </div>
          <div class="column has-text-left">
            <div class="subject-title">
              <h1 class="title is-4 has-text-primary">{{ subject.name }}</h1>
              <h2 class="subtitle is-7 has-text-grey">ID: {{ subject.subjectId }}</h2>
            </div>
          </div>
          <div class="column is-narrow">
            <edit-and-delete-dropdown v-on:deleted="deleteSubject" v-on:edited="editSubject" v-on:move-up="moveUp" v-on:move-down="moveDown"
                                      :isFirst="subject.isFirst" :isLast="subject.isLast" :isLoading="isLoading"
                                      class="subject-settings"></edit-and-delete-dropdown>
          </div>
        </div>
      </div>
    </div>

    <div class="columns has-text-centered">
      <div class="column is-half">
        <div>
          <p class="heading">Number Skills</p>
          <p class="title">{{ subject.numSkills | number }}</p>
        </div>
      </div>
      <div class="column is-half">
        <div>
          <p class="heading">Number Users</p>
          <p class="title">{{ subject.numUsers | number }}</p>
        </div>
      </div>
    </div>

    <div class="columns has-text-centered">
      <div class="column is-half">
        <div>
          <p class="heading">Total Points</p>
          <p class="title">{{ subject.totalPoints | number }}</p>
        </div>
      </div>
      <div class="column is-half">
        <div>
          <p class="heading">Points %</p>
          <p class="title">{{ subject.pointsPercentage }}</p>
        </div>
      </div>
    </div>

    <div class="columns has-text-centered">
      <div class="column is-full">
        <router-link :to="{ name:'SubjectPage',
              params: { projectId: this.subject.projectId, subjectId: this.subject.subjectId}}"
                     class="button is-outlined is-info">
          <span>Manage</span>
          <span class="icon is-small">
              <i class="fas fa-arrow-circle-right"/>
          </span>
        </router-link>
      </div>
    </div>

  </div>
</template>

<script>
  import EditAndDeleteDropdown from '@/components/utils/EditAndDeleteDropdown';
  import EditSubject from './EditSubject';
  import SubjectsService from './SubjectsService';


  export default {
    name: 'Subject',
    components: { EditAndDeleteDropdown },
    props: ['subject'],
    data() {
      return {
        isLoading: false,
      };
    },
    methods: {
      deleteSubject() {
        this.$dialog.confirm({
          title: 'WARNING: Delete Subject Action',
          message: `Subject Id: <b>${this.subject.subjectId}</b> <br/><br/>Delete Action can not be undone and <b>permanently</b> removes its skill definitions and users' performed skills.`,
          confirmText: 'Delete',
          type: 'is-danger',
          hasIcon: true,
          icon: 'exclamation-triangle',
          iconPack: 'fa',
          scroll: 'keep',
          onConfirm: () => this.deleteSubjectAjax(),
        });
      },
      deleteSubjectAjax() {
        this.isLoading = true;
        SubjectsService.deleteSubject(this.subject)
          .then(() => {
            this.isLoading = false;
            this.$emit('subject-deleted', this.subject);
          })
          .finally(() => {
            this.isLoading = false;
        });
      },
      editSubject() {
        this.$modal.open({
          parent: this,
          component: EditSubject,
          hasModalCard: true,
          width: 1110,
          props: {
            subject: this.subject,
            isEdit: true,
          },
          events: {
            'subject-created': this.subjectEdited,
          },
        });
      },
      subjectEdited(subject) {
        this.isLoading = true;
        SubjectsService.saveSubject(subject)
          .then(() => {
            this.subject = subject;
            this.isLoading = false;
          })
          .finally(() => {
            this.isLoading = false;
        });
      },
      moveUp() {
        this.$emit('move-subject-up', this.subject);
      },
      moveDown() {
        this.$emit('move-subject-down', this.subject);
      },
    },
  };
</script>

<style scoped>
  .subject-settings {
    position: relative;
    display: inline-block;
    float: right;
  }

  .subject-title {
    display: inline-block;
  }

  .subject-icon {
    font-size: 2rem;
    padding: 10px;
    border: 1px solid #ddd;
    border-radius: 5px;
    box-shadow: 0 22px 35px -16px rgba(0,0,0,0.1);
    /*margin-bottom: 2rem;*/
  }

</style>

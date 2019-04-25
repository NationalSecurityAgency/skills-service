<template>
  <div class="card h-100">
    <div class="card-body">
      <div class="row mb-4">
        <div class="col-10">
          <div class="media">
            <div class="d-inline-block mt-2 mr-3 border rounded p-1 text-info">
              <i class="fa-3x" :class="`${subject.iconClass}`"></i>
            </div>
            <div class="media-body">
              <h3 class="mb-2 h3 text-truncate text-info" style="max-width: 12rem;">{{ subject.name }}</h3>
              <h5 class="h5 text-truncate text-muted" style="max-width: 12rem;">ID: {{ subject.subjectId }}</h5>
            </div>
          </div>
        </div>

        <div class="col-2 text-right">
          <edit-and-delete-dropdown v-on:deleted="deleteSubject" v-on:edited="editSubject" v-on:move-up="moveUp"
                                    v-on:move-down="moveDown"
                                    :isFirst="subject.isFirst" :isLast="subject.isLast" :isLoading="isLoading"
                                    class="subject-settings"></edit-and-delete-dropdown>
        </div>
      </div>

      <div class="row text-center mb-3">
        <div class="col">
          <div>
            <p class="h6 text-uppercase text-muted">Number Skills</p>
            <strong class="h3">{{ subject.numSkills | number }}</strong>
          </div>
        </div>
        <div class="col">
          <div>
            <p class="h6 text-uppercase text-muted">Number Users</p>
            <p class="h3">{{ subject.numUsers | number }}</p>
          </div>
        </div>
      </div>

      <div class="row text-center mb-3">
        <div class="col">
          <div>
            <p class="h6 text-uppercase text-muted">Total Points</p>
            <p class="h3">{{ subject.totalPoints | number }}</p>
          </div>
        </div>
        <div class="col">
          <div>
            <p class="h6 text-uppercase text-muted">Points %</p>
            <p class="h3">{{ subject.pointsPercentage }}</p>
          </div>
        </div>
      </div>

      <div class="text-center">
        <router-link :to="{ name:'SubjectPage', params: { projectId: this.subject.projectId, subjectId: this.subject.subjectId}}"
                     class="btn btn-outline-info">
          Manage <i class="fas fa-arrow-circle-right"/>
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

  .subject-icon {
    font-size: 2rem;
    padding: 10px;
    border: 1px dotted #ddd;
    border-radius: 5px;
    /*box-shadow: 0 22px 35px -16px rgba(0,0,0,0.1);*/
    /*margin-bottom: 2rem;*/
  }

</style>

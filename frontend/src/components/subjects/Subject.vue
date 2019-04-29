<template>
  <div>
    <page-preview-card :options="cardOptions">
      <div slot="header-top-right">
        <edit-and-delete-dropdown v-on:deleted="deleteSubject" v-on:edited="editSubject" v-on:move-up="moveUp"
                                  v-on:move-down="moveDown"
                                  :isFirst="subject.isFirst" :isLast="subject.isLast" :isLoading="isLoading"
                                  class="subject-settings"></edit-and-delete-dropdown>
      </div>
      <div slot="footer">
        <router-link
          :to="{ name:'SubjectPage', params: { projectId: this.subject.projectId, subjectId: this.subject.subjectId}}"
          class="btn btn-outline-primary btn-sm">
          Manage <i class="fas fa-arrow-circle-right"/>
        </router-link>
      </div>
    </page-preview-card>
    <b-modal id="edit-subject-modal" title="BootstrapVue">
      <p class="my-4">Hello from modal!</p>
    </b-modal>
  </div>
</template>computed

<script>
  import EditAndDeleteDropdown from '@/components/utils/EditAndDeleteDropdown';
  import EditSubject from './EditSubject';
  import SubjectsService from './SubjectsService';
  import PagePreviewCard from '../utils/pages/PagePreviewCard';


  export default {
    name: 'Subject',
    components: { PagePreviewCard, EditAndDeleteDropdown },
    props: ['subject'],
    data() {
      return {
        isLoading: false,
        cardOptions: {},
      };
    },
    mounted() {
      this.cardOptions = {
        icon: this.subject.iconClass,
        title: this.subject.name,
        subTitle: `ID: ${this.subject.subjectId}`,
        stats: [{
          label: 'Number Skills',
          count: this.subject.numSkills,
        }, {
          label: 'Number Users',
          count: this.subject.numUsers,
        }, {
          label: 'Total Points',
          count: this.subject.totalPoints,
        }, {
          label: 'Points %',
          count: this.subject.pointsPercentage,
        }],
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
      editSubjectOld() {
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
      editSubject() {
        this.$bvModal.show('edit-subject-modal');
        // this.$modal.open({
        //   parent: this,
        //   component: EditSubject,
        //   hasModalCard: true,
        //   width: 1110,
        //   props: {
        //     subject: this.subject,
        //     isEdit: true,
        //   },
        //   events: {
        //     'subject-created': this.subjectEdited,
        //   },
        // });
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

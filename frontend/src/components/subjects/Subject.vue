<template>
  <div class="h-100">
    <loading-card :loading="isLoading"/>
    <page-preview-card v-if="!isLoading" :options="cardOptions">
      <div slot="header-top-right">
        <edit-and-delete-dropdown v-on:deleted="deleteSubject" v-on:edited="showEditSubject=true"
                                  v-on:move-up="moveUp"
                                  v-on:move-down="moveDown"
                                  :isFirst="subject.isFirst" :isLast="subject.isLast" :isLoading="isLoading"
                                  class="subject-settings"></edit-and-delete-dropdown>
      </div>
      <div slot="footer">
        <router-link
          :to="{ name:'SubjectSkills', params: { projectId: this.subject.projectId, subjectId: this.subject.subjectId}}"
          class="btn btn-outline-primary btn-sm">
          Manage <i class="fas fa-arrow-circle-right"/>
        </router-link>
      </div>
    </page-preview-card>

    <edit-subject v-if="showEditSubject" v-model="showEditSubject" :id="subject.subjectId"
                  :subject="subject" :is-edit="true" @subject-saved="subjectSaved"/>
  </div>
</template>

<script>
  import EditAndDeleteDropdown from '@/components/utils/EditAndDeleteDropdown';
  import EditSubject from './EditSubject';
  import SubjectsService from './SubjectsService';
  import PagePreviewCard from '../utils/pages/PagePreviewCard';
  import LoadingCard from '../utils/LoadingCard';
  import MsgBoxMixin from '../utils/modal/MsgBoxMixin';

  export default {
    name: 'Subject',
    mixins: [MsgBoxMixin],
    components: {
      LoadingCard,
      EditSubject,
      PagePreviewCard,
      EditAndDeleteDropdown,
    },
    props: ['subject'],
    data() {
      return {
        isLoading: false,
        showEditSubject: false,
        cardOptions: {},
      };
    },
    mounted() {
      this.buildCardOptions();
    },
    methods: {
      buildCardOptions() {
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
      deleteSubject() {
        const msg = `Subject with id [${this.subject.subjectId}] will be removed. Delete Action can not be undone and permanently removes its skill definitions and users' performed skills.`;
        this.msgConfirm(msg)
          .then((res) => {
            if (res) {
              this.$emit('subject-deleted', this.subject);
            }
          });
      },
      subjectSaved(subject) {
        this.isLoading = true;
        SubjectsService.saveSubject(subject)
          .then(() => {
            this.subject = subject;
            this.buildCardOptions();
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

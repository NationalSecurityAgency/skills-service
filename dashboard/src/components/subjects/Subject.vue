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
  <div class="h-100">
    <loading-card :loading="isLoading"/>
    <page-preview-card v-if="!isLoading" :options="cardOptions">
      <div slot="header-top-right">
        <edit-and-delete-dropdown ref="subjectEditDelete" v-on:deleted="deleteSubject" v-on:edited="showEditSubject=true"
                                  v-on:move-up="moveUp"
                                  v-on:move-down="moveDown"
                                  :isFirst="subjectInternal.isFirst" :isLast="subjectInternal.isLast" :isLoading="isLoading"
                                  :is-delete-disabled="deleteSubjectDisabled" :delete-disabled-text="deleteSubjectToolTip"
                                  class="subject-settings" data-cy="cardSettingsButton"></edit-and-delete-dropdown>
      </div>
      <div slot="footer">
        <router-link
          :to="{ name:'SubjectSkills', params: { projectId: this.subjectInternal.projectId, subjectId: this.subjectInternal.subjectId, subject: this.subjectInternal}}"
          class="btn btn-outline-primary btn-sm"
          :data-cy="`subjCard_${subjectInternal.subjectId}_manageBtn`">
          Manage <i class="fas fa-arrow-circle-right" aria-hidden="true"/>
        </router-link>
      </div>
    </page-preview-card>

    <edit-subject v-if="showEditSubject" v-model="showEditSubject" :id="subjectInternal.subjectId"
                  :subject="subjectInternal" :is-edit="true" @subject-saved="subjectSaved" @hidden="hiddenEventHandler"/>
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
        subjectInternal: { ...this.subject },
        deleteSubjectDisabled: false,
        deleteSubjectToolTip: '',
      };
    },
    mounted() {
      this.buildCardOptions();
    },
    watch: {
      subject(newVal) {
        if (newVal) {
          this.subjectInternal = newVal;
          this.buildCardOptions();
        }
      },
    },
    computed: {
      minimumPoints() {
        return this.$store.getters.config.minimumSubjectPoints;
      },
    },
    methods: {
      buildCardOptions() {
        this.cardOptions = {
          icon: this.subjectInternal.iconClass,
          title: this.subjectInternal.name,
          subTitle: `ID: ${this.subjectInternal.subjectId}`,
          stats: [{
            label: 'Number Skills',
            count: this.subjectInternal.numSkills,
          }, {
            label: 'Total Points',
            count: this.subjectInternal.totalPoints,
            warn: this.subjectInternal.totalPoints < this.minimumPoints,
            warnMsg: this.subjectInternal.totalPoints < this.minimumPoints ? `Subject has insufficient points assigned. Skills cannot be achieved until subject has at least ${this.minimumPoints} points.` : null,
          }, {
            label: 'Points %',
            count: this.subjectInternal.pointsPercentage,
          }],
        };
      },
      deleteSubject() {
        SubjectsService.checkIfSubjectBelongsToGlobalBadge(this.subjectInternal.projectId, this.subjectInternal.subjectId)
          .then((belongsToGlobal) => {
            if (belongsToGlobal) {
              const msg = 'Cannot delete this subject as it belongs to one or more global badges. Please contact a Supervisor to remove this dependency.';
              this.msgOk(msg, 'Unable to delete');
            } else {
              const msg = `Subject with id [${this.subjectInternal.subjectId}] will be removed. Delete Action can not be undone and permanently removes its skill definitions and users' performed skills.`;
              this.msgConfirm(msg)
                .then((res) => {
                  if (res) {
                    this.$emit('subject-deleted', this.subjectInternal);
                  }
                });
            }
          });
      },
      subjectSaved(subject) {
        this.isLoading = true;
        SubjectsService.saveSubject(subject)
          .then(() => {
            this.subjectInternal = subject;
            this.buildCardOptions();
            this.isLoading = false;
            this.handleFocus();
          })
          .finally(() => {
            this.isLoading = false;
          });
      },
      moveUp() {
        this.$emit('move-subject-up', this.subjectInternal);
      },
      moveDown() {
        this.$emit('move-subject-down', this.subjectInternal);
      },
      hiddenEventHandler(e) {
        if (!e || !e.update) {
          this.handleFocus();
        }
      },
      handleFocus() {
        this.$nextTick(() => {
          this.$refs.subjectEditDelete.focus();
        });
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

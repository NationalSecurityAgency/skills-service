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
  <div class="h-100" data-cy="subjectCard">
    <loading-card :loading="isLoading"/>
    <nav-card-with-stats-and-controls v-if="!isLoading"
                                      :disable-sort-control="disableSortControl"
                                      :options="cardOptions" :data-cy="`subjectCard-${subjectInternal.subjectId}`">
      <div slot="underTitle">
        <card-navigate-and-edit-controls
          ref="subjectCardControls" class="mt-2"
          :options="cardOptions.controls"
          @edit="showEditSubject=true"
          @delete="deleteSubject"
          @share="shareSubject"
          @unshare="unshareSubject"
          :is-delete-disabled="deleteSubjectDisabled"
          :delete-disabled-text="deleteSubjectToolTip"/>
      </div>
      <div slot="footer" class="text-right">
        <span class="small"><b-badge style="font-size: 0.8rem;" variant="primary" data-cy="pointsPercent">{{ this.subjectInternal.pointsPercentage }}%</b-badge> of the total points</span>
      </div>
    </nav-card-with-stats-and-controls>

    <edit-subject v-if="showEditSubject" v-model="showEditSubject" :id="subjectInternal.subjectId"
                  :subject="subjectInternal" :is-edit="true" @subject-saved="subjectSaved" @hidden="hiddenEventHandler"/>
  </div>
</template>

<script>
  import EditSubject from './EditSubject';
  import SubjectsService from './SubjectsService';
  import LoadingCard from '../utils/LoadingCard';
  import MsgBoxMixin from '../utils/modal/MsgBoxMixin';
  import NavCardWithStatsAndControls from '../utils/cards/NavCardWithStatsAndControls';
  import CardNavigateAndEditControls from '../utils/cards/CardNavigateAndEditControls';

  export default {
    name: 'Subject',
    mixins: [MsgBoxMixin],
    components: {
      CardNavigateAndEditControls,
      NavCardWithStatsAndControls,
      LoadingCard,
      EditSubject,
    },
    props: ['subject', 'disableSortControl'],
    data() {
      return {
        isLoading: false,
        showEditSubject: false,
        cardOptions: { controls: {} },
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
      alreadyShared() {
        return this.subjectInternal?.exported === true;
      },
    },
    methods: {
      buildCardOptions() {
        this.cardOptions = {
          navTo: this.buildManageNavLink(),
          icon: this.subjectInternal.iconClass,
          title: this.subjectInternal.name,
          subTitle: `ID: ${this.subjectInternal.subjectId}`,
          stats: [{
            label: '# Skills',
            count: this.subjectInternal.numSkills,
            icon: 'fas fa-graduation-cap skills-color-skills',
          }, {
            label: 'Points',
            count: this.subjectInternal.totalPoints,
            warn: this.subjectInternal.totalPoints < this.minimumPoints,
            warnMsg: this.subjectInternal.totalPoints < this.minimumPoints ? `Subject has insufficient points assigned. Skills cannot be achieved until subject has at least ${this.minimumPoints} points.` : null,
            icon: 'far fa-arrow-alt-circle-up skills-color-points',
          }],
          controls: {
            navTo: this.buildManageNavLink(),
            type: 'Subject',
            name: this.subjectInternal.name,
            id: this.subjectInternal.subjectId,
            deleteDisabledText: this.deleteSubjectToolTip,
            isDeleteDisabled: this.deleteSubjectDisabled,
            showShare: false,
            shareEnabled: !this.alreadyShared,
          },
        };
      },
      buildManageNavLink() {
        return { name: 'SubjectSkills', params: { projectId: this.subjectInternal.projectId, subjectId: this.subjectInternal.subjectId, subject: this.subjectInternal } };
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
      shareSubject() {
        SubjectsService.shareSubject(this.subjectInternal.projectId, this.subjectInternal.subjectId).then(() => {
          this.subjectInternal.exported = true;
        });
      },
      unshareSubject() {
        SubjectsService.unshareSubject(this.subjectInternal.projectId, this.subjectInternal.subjectId).then(() => {
          this.subjectInternal.exported = false;
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
      hiddenEventHandler(e) {
        if (!e || !e.update) {
          this.handleFocus();
        }
      },
      handleFocus() {
        this.$nextTick(() => {
          this.$refs.subjectCardControls.focusOnEdit();
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
  }

</style>

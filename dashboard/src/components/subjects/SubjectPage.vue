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
    <page-header :loading="isLoading" :options="headerOptions">
      <div slot="subSubTitle" v-if="subject">
        <b-button @click="displayEditSubject"
                  ref="editSubjectButton"
                  class="btn btn-outline-primary mr-1"
                  size="sm"
                  variant="outline-primary"
                  data-cy="btn_edit-subject"
                  :aria-label="'edit Subject '+subject.subjectId">
          <span class="">Edit </span> <i class="fas fa-edit" aria-hidden="true"/>
        </b-button>
      </div>
    </page-header>

    <navigation v-if="!isLoading" :nav-items="[
          {name: 'Skills', iconClass: 'fa-graduation-cap skills-color-skills', page: 'SubjectSkills'},
          {name: 'Levels', iconClass: 'fa-trophy skills-color-levels', page: 'SubjectLevels'},
          {name: 'Users', iconClass: 'fa-users skills-color-users', page: 'SubjectUsers'},
          {name: 'Metrics', iconClass: 'fa-chart-bar skills-color-metrics', page: 'SubjectMetrics'},
        ]">
    </navigation>

    <edit-subject v-if="showEditSubject" v-model="showEditSubject"
                  :subject="subject" @subject-saved="subjectEdited"
                  :is-edit="true"
                  @hidden="handleHideSubjectEdit"/>
  </div>
</template>

<script>
  import { createNamespacedHelpers } from 'vuex';
  import Navigation from '../utils/Navigation';
  import PageHeader from '../utils/pages/PageHeader';
  import EditSubject from './EditSubject';
  import SubjectsService from './SubjectsService';

  const { mapActions, mapGetters, mapMutations } = createNamespacedHelpers('subjects');

  export default {
    name: 'SubjectPage',
    components: {
      PageHeader,
      Navigation,
      EditSubject,
    },
    data() {
      return {
        isLoading: true,
        projectId: '',
        subjectId: '',
        showEditSubject: false,
      };
    },
    created() {
      this.projectId = this.$route.params.projectId;
      this.subjectId = this.$route.params.subjectId;
    },
    mounted() {
      this.loadSubject();
    },
    computed: {
      ...mapGetters([
        'subject',
      ]),
      headerOptions() {
        if (!this.subject) {
          return {};
        }
        return {
          icon: 'fas fa-cubes skills-color-subjects',
          title: `SUBJECT: ${this.subject.name}`,
          subTitle: `ID: ${this.subjectId}`,
          stats: [{
            label: 'Groups',
            count: this.subject.numGroups,
            icon: 'fas fa-layer-group skills-color-groups',
          }, {
            label: 'Skills',
            count: this.subject.numSkills,
            icon: 'fas fa-graduation-cap skills-color-skills',
          }, {
            label: 'Points',
            count: this.subject.totalPoints,
            warn: this.subject.totalPoints < this.minimumPoints,
            warnMsg: this.subject.totalPoints < this.minimumPoints ? `Subject has insufficient points assigned. Skills cannot be achieved until subject has at least ${this.minimumPoints} points.` : null,
            icon: 'far fa-arrow-alt-circle-up skills-color-points',
          }],
        };
      },
      minimumPoints() {
        return this.$store.getters.config.minimumSubjectPoints;
      },
    },
    watch: {
      subject(newVal) {
        if (newVal && newVal.subjectId !== this.subjectId) {
          this.subjectId = newVal.subjectId;
        }
      },
    },
    methods: {
      ...mapActions([
        'loadSubjectDetailsState',
      ]),
      ...mapMutations([
        'setSubject',
      ]),
      loadSubject() {
        this.isLoading = true;
        if (this.$route.params.subject) {
          this.setSubject(this.$route.params.subject);
          this.isLoading = false;
        } else {
          this.loadSubjectDetailsState({ projectId: this.projectId, subjectId: this.subjectId })
            .finally(() => {
              this.isLoading = false;
            });
        }
      },
      displayEditSubject() {
        this.showEditSubject = true;
      },
      subjectEdited(subject) {
        SubjectsService.saveSubject(subject).then((resp) => {
          const origId = this.subject.subjectId;
          this.setSubject(resp);
          if (resp.subjectId !== origId) {
            this.$router.replace({ name: this.$route.name, params: { ...this.$route.params, subjectId: resp.subjectId } });
          }
        });
      },
      handleHideSubjectEdit() {
        this.showEditSubject = false;
        this.$nextTick(() => {
          const ref = this.$refs.editSubjectButton;
          if (ref) {
            ref.focus();
          }
        });
      },
    },
  };
</script>

<style scoped>
</style>

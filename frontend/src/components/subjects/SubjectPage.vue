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
    <page-header :loading="isLoading" :options="headerOptions"/>

    <navigation v-if="!isLoading" :nav-items="[
          {name: 'Skills', iconClass: 'fa-graduation-cap', page: 'SubjectSkills'},
          {name: 'Levels', iconClass: 'fa-trophy', page: 'SubjectLevels'},
          {name: 'Users', iconClass: 'fa-users', page: 'SubjectUsers'},
          {name: 'Metrics', iconClass: 'fa-chart-bar', page: 'SubjectMetrics'},
        ]">
    </navigation>
  </div>
</template>

<script>
  import { createNamespacedHelpers } from 'vuex';

  import Navigation from '../utils/Navigation';
  import PageHeader from '../utils/pages/PageHeader';

  const { mapActions, mapGetters, mapMutations } = createNamespacedHelpers('subjects');

  export default {
    name: 'SubjectPage',
    components: {
      PageHeader,
      Navigation,
    },
    data() {
      return {
        isLoading: true,
        projectId: '',
        subjectId: '',
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
          icon: 'fas fa-cubes',
          title: `SUBJECT: ${this.subject.name}`,
          subTitle: `ID: ${this.subjectId}`,
          stats: [{
            label: 'Skills',
            count: this.subject.numSkills,
          }, {
            label: 'Points',
            count: this.subject.totalPoints,
            warn: this.subject.totalPoints < this.minimumPoints,
            warnMsg: this.subject.totalPoints < this.minimumPoints ? `Subject has insufficient points assigned. Skills cannot be achieved until subject has at least ${this.minimumPoints} points.` : null,
          }],
        };
      },
      minimumPoints() {
        return this.$store.getters.config.minimumSubjectPoints;
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
    },
  };
</script>

<style scoped>
</style>

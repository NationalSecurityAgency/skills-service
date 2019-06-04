<template>
  <div>
    <page-header :loading="isLoading" :options="headerOptions"/>

    <navigation v-if="!isLoading" :nav-items="[
          {name: 'Skills', iconClass: 'fa-graduation-cap', page: 'SubjectSkills'},
          {name: 'Levels', iconClass: 'fa-trophy', page: 'SubjectLevels'},
          {name: 'Users', iconClass: 'fa-users', page: 'SubjectUsers'},
          {name: 'Stats', iconClass: 'fa-chart-bar', page: 'SubjectStats'},
        ]">
    </navigation>
  </div>
</template>

<script>
  import Navigation from '../utils/Navigation';
  import SubjectsService from './SubjectsService';
  import PageHeader from '../utils/pages/PageHeader';

  export default {
    name: 'SubjectPage',
    components: {
      PageHeader,
      Navigation,
    },
    data() {
      return {
        isLoading: true,
        subject: {},
        projectId: '',
        subjectId: '',
        headerOptions: {},
      };
    },
    created() {
      this.projectId = this.$route.params.projectId;
      this.subjectId = this.$route.params.subjectId;
    },
    mounted() {
      this.loadSubject();
    },
    methods: {
      loadSubject() {
        this.isLoading = true;
        SubjectsService.getSubjectDetails(this.projectId, this.subjectId)
          .then((response) => {
            this.subject = response;
            this.headerOptions = this.buildHeaderOptions(this.subject);
            this.isLoading = false;
          });
      },
      buildHeaderOptions(subject) {
        return {
          icon: 'fas fa-cubes',
          title: `SUBJECT: ${subject.name}`,
          subTitle: `ID: ${this.subjectId}`,
          stats: [{
            label: 'Skills',
            count: subject.numSkills,
          }, {
            label: 'Points',
            count: subject.totalPoints,
          }, {
            label: 'Users',
            count: subject.numUsers,
          }],
        };
      },
    },
  };
</script>

<style scoped>
</style>

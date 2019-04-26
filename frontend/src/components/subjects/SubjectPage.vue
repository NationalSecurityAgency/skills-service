<template>
  <div>
    <page-header :loading="isLoading" :options="headerOptions"/>

    <section class="section">
      <navigation :nav-items="[
          {name: 'Skills', iconClass: 'fa-graduation-cap'},
          {name: 'Level Definitions', iconClass: 'fa-trophy'},
          {name: 'Users', iconClass: 'fa-users'},
          {name: 'Stats', iconClass: 'fa-chart-bar'},
        ]">
        <template slot="Skills">
          <skills :project-id="projectId" :subject-id="subjectId" v-on:skills-change="loadSubject"/>
        </template>
        <template slot="Level Definitions">
          <levels :project-id="projectId" :subject-id="subjectId" :max-levels="25"/>
        </template>
        <template slot="Users">
          <section v-if="projectId" class="">
            <users :project-id="projectId" :subject-id="subjectId"/>
          </section>
        </template>
        <template slot="Stats">
          <section-stats :project-id="projectId" :section="section" :section-id-param="subjectId"></section-stats>
        </template>
      </navigation>
    </section>
  </div>
</template>

<script>
  import Navigation from '../utils/Navigation';
  import Levels from '../levels/Levels';
  import Skills from '../skills/Skills';
  import SectionStats from '../stats/SectionStats';
  import Users from '../users/Users';
  import SubjectsService from './SubjectsService';
  import { SECTION } from '../stats/SectionHelper';
  import PageHeader from '../utils/pages/PageHeader';

  export default {
    name: 'SubjectPage',
    components: {
      PageHeader,
      SectionStats,
      Skills,
      Levels,
      Users,
      Navigation,
    },
    breadcrumb() {
      return {
        label: `SUBJECT: ${this.subjectId}`,
        parentsList: [
          {
            to: {
              name: 'ProjectPage',
              params: {
                projectId: this.projectId,
              },
            },
            label: `PROJECT: ${this.projectId}`,
          },
          {
            to: {
              name: 'HomePage',
            },
            label: 'Home',
          },
        ],
      };
    },
    data() {
      return {
        isLoading: true,
        subject: {},
        projectId: '',
        subjectId: '',
        section: SECTION.SUBJECTS,
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

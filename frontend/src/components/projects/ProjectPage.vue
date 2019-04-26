<template>
  <div>
    <page-header :loading="isLoading" :options="headerOptions"/>

    <section class="section" v-if="project.name">
      <navigation :nav-items="[
          {name: 'Subjects', iconClass: 'fa-cubes'},
          {name: 'Badges', iconClass: 'fa-award'},
          {name: 'Dependencies', iconClass: 'fa-vector-square'},
          {name: 'Cross Projects', iconClass: 'fa-handshake'},
          {name: 'Levels', iconClass: 'fa-trophy'},
          {name: 'Users', iconClass: 'fa-users'},
          {name: 'Stats', iconClass: 'fa-chart-bar'},
          {name: 'Access', iconClass: 'fa-shield-alt'},
          {name: 'Settings', iconClass: 'fa-cogs'}
        ]">
        <template slot="Subjects">
          <section v-if="project.projectId" class="">
            <subjects :project="project" v-on:subjects-changed="loadProjects"/>
          </section>
        </template>
        <template slot="Levels">
          <levels :project-id="project.projectId" :max-levels="25"/>
        </template>
        <template slot="Badges">
          <badges :project="project" v-on:subjects-changed="loadProjects"/>
        </template>
        <template slot="Access">
          <section v-if="project.projectId" class="">
            <access-settings :project="project"/>
          </section>
        </template>
        <template slot="Users">
          <section v-if="project.projectId" class="">
            <users :projectId="project.projectId"/>
          </section>
        </template>
        <template slot="Stats">
          <section v-if="project.projectId" class="">
            <section-stats :project-id="project.projectId" :section="section"></section-stats>
          </section>
        </template>
        <template slot="Dependencies">
          <full-dependency-graph :project-id="project.projectId"></full-dependency-graph>
        </template>
        <template slot="Cross Projects">
          <cross-projects-skills :project-id="project.projectId"></cross-projects-skills>
        </template>
        <template slot="Settings">
          <section v-if="project.projectId" class="">
            <project-settings :project-id="project.projectId"/>
          </section>
        </template>
      </navigation>
    </section>
  </div>

</template>

<script>
  import ProjectService from './ProjectService';
  import Subjects from '../subjects/Subjects';
  import Levels from '../levels/Levels';
  import Badges from '../badges/Badges';
  import AccessSettings from '../access/AccessSettings';
  import Users from '../users/Users';
  import Navigation from '../utils/Navigation';
  import SectionStats from '../stats/SectionStats';
  import FullDependencyGraph from '../skills/dependencies/FullDependencyGraph';
  import ProjectSettings from '../settings/ProjectSettings';
  import CrossProjectsSkills from '../skills/crossProjects/CrossProjectsSkills';
  import { SECTION } from '../stats/SectionHelper';
  import PageHeader from '../utils/pages/PageHeader';

  export default {
    name: 'ProjectPage',
    components: {
      PageHeader,
      ProjectSettings,
      CrossProjectsSkills,
      FullDependencyGraph,
      SectionStats,
      Navigation,
      Levels,
      Subjects,
      Badges,
      AccessSettings,
      Users,
    },
    breadcrumb() {
      return {
        label: `PROJECT: ${this.$route.params.projectId}`,
        parent: 'HomePage',
      };
    },
    data() {
      return {
        isLoading: true,
        section: SECTION.PROJECTS,
        project: {},
        headerOptions: {},
      };
    },
    mounted() {
      this.loadProjects();
    },
    methods: {
      loadProjects() {
        this.isLoading = true;
        ProjectService.getProjectDetails(this.$route.params.projectId)
          .then((response) => {
            this.project = response;
            this.headerOptions = this.buildHeaderOptions(this.project);
            this.isLoading = false;
          });
      },
      buildHeaderOptions(project) {
        return {
          icon: 'fas fa-list-alt',
          title: `PROJECT: ${project.name}`,
          subTitle: `ID: ${project.projectId}`,
          stats: [{
            label: 'Subjects',
            count: project.numSubjects,
          }, {
            label: 'Skills',
            count: project.numSkills,
          }, {
            label: 'Points',
            count: project.totalPoints,
            warnMsg: project.totalPoints < 100 ? 'Project has insufficient points assigned. Skills cannot be achieved until project has at least 100 points.' : null,
          }, {
            label: 'Users',
            count: project.numUsers,
          }],
        };
      },
    },
  };
</script>

<style scoped>

</style>

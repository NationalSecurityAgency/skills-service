<template>
  <div>
    <section class="section">
      <loading-container v-bind:is-loading="isLoading">

        <div class="columns has-text-left">
          <div class="column">
            <div class="subject-title">
              <h1 class="title"><i class="fas fa-list-alt has-text-link"/> PROJECT: {{ project.name }}</h1>
              <h2 class="subtitle is-6 has-text-grey">ID: {{ project.projectId }}</h2>
            </div>
          </div>
          <div class="column">
            <div class="columns has-text-centered">
              <div class="column is-one-quarter">
                <div>
                  <p class="heading">Subjects</p>
                  <p class="title">{{ project.numSubjects | number}}</p>
                </div>
              </div>
              <div class="column is-one-quarter">
                <div>
                  <p class="heading">Skills</p>
                  <p class="title">{{ project.numSkills | number}}</p>
                </div>
              </div>

              <div class="column is-one-quarter">
                <div>
                  <p class="heading">Total Points</p>
                  <p class="title">{{ project.totalPoints | number }}</p>
                </div>
              </div>
              <div class="column is-one-quarter">
                <div>
                  <p class="heading">Users</p>
                  <p class="title">{{ project.numUsers | number }}</p>
                </div>
              </div>
            </div>

          </div>
        </div>

      </loading-container>
    </section>

    <hr class="skills-no-margin"/>

    <section class="section" v-if="project.name">
      <navigation :nav-items="[
          {name: 'Subjects', iconClass: 'fa-cubes'},
          {name: 'Badges', iconClass: 'fa-award'},
          {name: 'Dependencies', iconClass: 'fa-vector-square'},
          {name: 'Users', iconClass: 'fa-users'},
          {name: 'Stats', iconClass: 'fa-chart-bar'},
          {name: 'Levels', iconClass: 'fa-trophy'},
          {name: 'Access', iconClass: 'fa-shield-alt'},
        ]">
        <template slot="Subjects">
          <section v-if="project.projectId" class="">
            <subjects :project="project" v-on:subjects-changed="loadProjects"/>
          </section>
        </template>
        <template slot="Levels">
          <levels :project-id="project.projectId"/>
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
            <project-stats :project-id="project.projectId"></project-stats>
          </section>
        </template>
        <template slot="Dependencies">
          <div class="columns">
            <div class="column is-full">
              <span class="title is-3">Skill Dependencies</span>
            </div>
          </div>

          <full-dependency-graph :project-id="project.projectId"></full-dependency-graph>
        </template>
      </navigation>
    </section>
  </div>

</template>

<script>
  import axios from 'axios';
  import MyProject from './MyProject';
  import Subjects from '../subjects/Subjects';
  import Levels from '../levels/Levels';
  import Badges from '../badges/Badges';
  import AccessSettings from '../access/AccessSettings';
  import Users from '../users/Users';
  import Navigation from '../utils/Navigation';
  import LoadingContainer from '../utils/LoadingContainer';
  import ProjectStats from '../stats/ProjectStats';
  import FullDependencyGraph from '../skills/dependencies/FullDependencyGraph';

  export default {
    name: 'ProjectPage',
    components: { FullDependencyGraph, ProjectStats, LoadingContainer, Navigation, Levels, Subjects, Badges, AccessSettings, MyProject, Users },
    breadcrumb() {
      return {
        label: `PROJECT: ${this.$route.params.projectId}`,
        parent: 'HomePage',
      };
    },
    data() {
      return {
        isLoading: true,
        project: {},
        serverErrors: [],
      };
    },
    mounted() {
      this.loadProjects();
    },
    destroyed() {
      this.$store.commit('currentProjectId', '');
    },
    methods: {
      loadProjects() {
        this.isLoading = true;
        axios.get(`/admin/projects/${this.$route.params.projectId}`)
          .then((response) => {
            this.isLoading = false;
            this.project = response.data;
            this.$store.commit('currentProjectId', this.project.projectId);
          })
          .catch((e) => {
            this.serverErrors.push(e);
        });
      },
    },
  };
</script>

<style scoped>
  .section {
    padding: 2rem 1.5rem;
  }
</style>

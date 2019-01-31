<template>
  <div>
    <section class="section skills-underline-container-1">
      <loading-container v-bind:is-loading="isLoading">

        <div class="columns has-text-left">
          <div class="column">
            <div class="subject-title ">
              <h1 class="title"><i class="fas fa-cubes"/> SUBJECT: {{ subject.name }}</h1>
              <h2 class="subtitle is-6 has-text-grey">ID: {{ subject.subjectId }}</h2>
            </div>
          </div>
          <div class="column">
            <div class="columns has-text-centered">
              <div class="column is-one-quarter">
                <div>
                  <p class="heading">Skills</p>
                  <p class="title">{{ subject.numSkills | number}}</p>
                </div>
              </div>

              <div class="column is-one-quarter">
                <div>
                  <p class="heading">Num Disabled Skills</p>
                  <p class="title">0</p>
                </div>
              </div>

              <div class="column is-one-quarter">
                <div>
                  <p class="heading">Total Points</p>
                  <p class="title">{{ subject.totalPoints | number }}</p>
                </div>
              </div>

              <div class="column is-one-quarter">
                <div>
                  <p class="heading">Users</p>
                  <p class="title">{{ subject.numUsers | number}}</p>
                </div>
              </div>
            </div>
          </div>
        </div>
      </loading-container>
    </section>

    <hr class="skills-no-margin"/>

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
          <levels :project-id="projectId" :subject-id="subjectId"/>
        </template>
        <template slot="Users">
          <section v-if="projectId" class="">
            <users :project-id="projectId" :subject-id="subjectId"/>
          </section>
        </template>
        <template slot="Stats">
          <project-stats :project-id="projectId"></project-stats>
        </template>
      </navigation>
    </section>
  </div>
</template>

<script>
  import axios from 'axios';
  import Navigation from '../utils/Navigation';
  import Levels from '../levels/Levels';
  import Skills from '../skills/Skills';
  import LoadingContainer from '../utils/LoadingContainer';
  import ProjectStats from '../stats/ProjectStats';
  import Users from '../users/Users';

  export default {
    name: 'SubjectPage',
    components: { ProjectStats, LoadingContainer, Skills, Levels, Users, Navigation },
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
        serverErrors: [],
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
    methods: {
      loadSubject() {
        this.isLoading = true;
        axios.get(`/admin/projects/${this.projectId}/subjects/${this.subjectId}`)
          .then((response) => {
            this.subject = response.data;
            this.isLoading = false;
          })
          .catch((e) => {
            this.serverErrors.push(e);
            throw e;
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

<template>
  <div>
    <section class="section skills-underline-container-1">
      <loading-container v-bind:is-loading="isLoading">

        <div class="columns has-text-left">
          <div class="column">
            <div class="subject-title ">
              <h1 class="title"><i class="fas fa-graduation-cap"/> SKILL: {{ skill.name }}</h1>
              <h2 class="subtitle is-6 has-text-grey">ID: {{ skill.skillId }}</h2>
            </div>
          </div>
          <div class="column">
            <div class="columns has-text-centered">
              <div class="column is-one-quarter">
                <!--<div>-->
                  <!--<p class="heading">Skills</p>-->
                  <!--&lt;!&ndash;<p class="title">{{ badge.numSkills | number}}</p>&ndash;&gt;-->
                <!--</div>-->
              </div>

              <div class="column is-one-quarter">
                <!--<div>-->
                  <!--<p class="heading">Num Disabled Skills</p>-->
                  <!--<p class="title">0</p>-->
                <!--</div>-->
              </div>

              <div class="column is-one-quarter">
                <div>
                  <p class="heading">Total Points</p>
                  <p class="title">{{ this.skill.totalPoints | number }}</p>
                </div>
              </div>

              <div class="column is-one-quarter">
                <div>
                  <p class="heading">Users</p>
                  <p class="title">{{ this.skill.numUsers | number }}</p>
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
          {name: 'Overview', iconClass: 'fa-info-circle'},
          {name: 'Dependencies', iconClass: 'fa-vector-square'},
          {name: 'Users', iconClass: 'fa-users'},
          {name: 'Stats', iconClass: 'fa-chart-bar'},
        ]">
        <template slot="Overview">
          <div class="columns">
            <div class="column is-full">
              <span class="title is-3">Overview</span>
            </div>
          </div>

          <child-row-skills-display v-if="this.skill.skillId" :skill="this.skill"></child-row-skills-display>
        </template>
        <template slot="Dependencies">
          <skill-dependencies :skill="skill"></skill-dependencies>
        </template>
        <template slot="Users">
          <users :project-id="this.$route.params.projectId" :skill-id="this.$route.params.skillId" />
        </template>
        <template slot="Stats">
          <project-stats :project-id="this.$route.params.projectId"></project-stats>
        </template>
      </navigation>
    </section>
  </div>
</template>

<script>
  import SkillsService from './SkillsService';
  import Navigation from '../utils/Navigation';
  // import Skills from '../skills/Skills';
  import LoadingContainer from '../utils/LoadingContainer';
  import ProjectStats from '../stats/ProjectStats';
  import ChildRowSkillsDisplay from './ChildRowSkillsDisplay';
  import SkillDependencies from './dependencies/SkillDependencies';
  import Users from '../users/Users';

  export default {
    name: 'SkillPage',
    components: { SkillDependencies, ChildRowSkillsDisplay, ProjectStats, LoadingContainer, Navigation, Users },
    breadcrumb() {
      return {
        label: `SKILL: ${this.skill.name || this.$route.params.skillId}`,
        parentsList: [
          {
            to: {
              name: 'SubjectPage',
              params: {
                projectId: this.$route.params.projectId,
                subjectId: this.$route.params.subjectId,
              },
            },
            label: `SUBJECT: ${this.$route.params.subjectId}`,
          },
          {
            to: {
              name: 'ProjectPage',
              params: {
                projectId: this.$route.params.projectId,
                projectName: this.$route.params.projectId,
              },
            },
            label: `PROJECT: ${this.$route.params.projectId}`,
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
        skill: {},
        subjectId: '',
      };
    },
    mounted() {
      this.loadSkill();
    },
    watch: {
      // Vue caches components and when re-directed to the same component the path will be pushed
      // to the url but the component will NOT be re-mounted therefore we must listen for events and re-load
      // the data; alternatively could update
      //    <router-view :key="$route.fullPath"/>
      // but components will never get cached - caching maybe important for components that want to update
      // the url so the state can be re-build later (example include browsing a map or dependency graph in our case)
      '$route.params.skillId': function skillChange() {
        this.loadSkill();
      },
    },
    methods: {
      loadSkill() {
        this.isLoading = true;
        SkillsService.getSkillDetails(this.$route.params.projectId, this.$route.params.subjectId, this.$route.params.skillId)
          .then((response) => {
            this.skill = Object.assign(response, { subjectId: this.$route.params.subjectId });
            this.isLoading = false;
          })
          .finally(() => {
            this.isLoading = false;
        });
      },
    },
  };
</script>

<style scoped>

</style>


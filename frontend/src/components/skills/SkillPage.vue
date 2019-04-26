<template>
  <div>
    <page-header :loading="isLoading" :options="headerOptions"/>

    <section class="section">
      <navigation :nav-items="[
          {name: 'Overview', iconClass: 'fa-info-circle'},
          {name: 'Dependencies', iconClass: 'fa-vector-square'},
          {name: 'Users', iconClass: 'fa-users'},
          {name: 'Stats', iconClass: 'fa-chart-bar'},
        ]">
        <template slot="Overview">
          <sub-page-header title="Overview"/>
          <div class="card">
            <div class="card-body">
              <child-row-skills-display v-if="this.skill.skillId" :skill="this.skill"></child-row-skills-display>
            </div>
          </div>
        </template>
        <template slot="Dependencies">
          <skill-dependencies :skill="skill"></skill-dependencies>
        </template>
        <template slot="Users">
          <users :project-id="this.$route.params.projectId" :skill-id="this.$route.params.skillId" />
        </template>
        <template slot="Stats">
          <section-stats :project-id="this.$route.params.projectId"  :section="section" :section-id-param="this.$route.params.skillId"></section-stats>
        </template>
      </navigation>
    </section>
  </div>
</template>

<script>
  import SkillsService from './SkillsService';
  import Navigation from '../utils/Navigation';
  // import Skills from '../skills/Skills';
  import SectionStats from '../stats/SectionStats';
  import ChildRowSkillsDisplay from './ChildRowSkillsDisplay';
  import SkillDependencies from './dependencies/SkillDependencies';
  import Users from '../users/Users';
  import { SECTION } from '../stats/SectionHelper';
  import PageHeader from '../utils/pages/PageHeader';
  import SubPageHeader from '../utils/pages/SubPageHeader';

  export default {
    name: 'SkillPage',
    components: {
      SubPageHeader,
      PageHeader,
      SkillDependencies,
      ChildRowSkillsDisplay,
      SectionStats,
      Navigation,
      Users,
    },
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
        section: SECTION.SKILLS,
        headerOptions: {},
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
            this.headerOptions = this.buildHeaderOptions(this.skill);
            this.isLoading = false;
          });
      },
      buildHeaderOptions(skill) {
        return {
          icon: 'fas fa-graduation-cap',
          title: `SKILL: ${skill.name}`,
          subTitle: `ID: ${skill.skillId}`,
          stats: [{
            label: 'Points',
            count: skill.totalPoints,
          }, {
            label: 'Users',
            count: skill.numUsers,
          }],
        };
      },
    },
  };
</script>

<style scoped>

</style>

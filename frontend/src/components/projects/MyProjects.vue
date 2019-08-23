<template>
  <div>
    <sub-page-header title="My Projects" action="Project"
                     @add-action="newProject.show=true"
                     :disabled="addProjectDisabled" :disabled-msg="addProjectsDisabledMsg"/>

    <loading-container v-bind:is-loading="isLoading">
      <div v-for="project of projects" :key="project.projectId" class="mb-3">
        <my-project :project="project" v-on:project-deleted="projectRemoved" v-on:move-project-up="moveProjectUp"
                    v-on:move-project-down="moveProjectDown"/>
      </div>

      <no-content2 v-if="!projects || projects.length==0" icon="fas fa-hand-spock" class="mt-4"
                   title="No Projects Yet..." message="Welcome!! Start by creating a new project."/>
    </loading-container>

    <edit-project v-if="newProject.show" v-model="newProject.show" :project="newProject.project"
                  @project-saved="projectAdded"/>

  </div>

</template>

<script>
  import { SkillsReporter } from '@skills/skills-client-vue';
  import MyProject from './MyProject';
  import EditProject from './EditProject';
  import LoadingContainer from '../utils/LoadingContainer';
  import ProjectService from './ProjectService';
  import SubPageHeader from '../utils/pages/SubPageHeader';
  import NoContent2 from '../utils/NoContent2';

  export default {
    name: 'MyProjects',
    data() {
      return {
        isLoading: true,
        projects: [],
        newProject: {
          show: false,
          project: { name: '', projectId: '' },
        },
      };
    },
    components: {
      NoContent2,
      SubPageHeader,
      LoadingContainer,
      MyProject,
      EditProject,
    },
    mounted() {
      this.loadProjects();
    },
    computed: {
      addProjectDisabled() {
        return this.projects && this.$store.getters.config && this.projects.length >= this.$store.getters.config.maxProjectsPerAdmin;
      },
      addProjectsDisabledMsg() {
        if(this.$store.getters.config) {
          return `The maximum number of Projects allowed is ${this.$store.getters.config.maxProjectsPerAdmin}`;
        }
        return '';
      },
    },
    methods: {
      loadProjects() {
        ProjectService.getProjects()
          .then((response) => {
            this.projects = response;
          })
          .finally(() => {
            this.isLoading = false;
          });
      },
      projectRemoved(project) {
        this.isLoading = true;
        ProjectService.deleteProject(project.projectId)
          .then(() => {
            this.loadProjects();
          });
      },
      projectAdded(project) {
        this.isLoading = true;
        ProjectService.saveProject(project)
          .then(() => {
            this.loadProjects();
            SkillsReporter.reportSkill('CreateProject');
          });
      },
      moveProjectDown(project) {
        this.moveProject(project, 'DisplayOrderDown');
      },
      moveProjectUp(project) {
        this.moveProject(project, 'DisplayOrderUp');
      },
      moveProject(project, actionToSubmit) {
        this.isLoading = true;
        ProjectService.changeProjectOrder(project.projectId, actionToSubmit)
          .then(() => {
            this.loadProjects();
          });
      },

    },
  };
</script>

<style scoped>
</style>

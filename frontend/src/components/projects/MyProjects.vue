<template>
  <div class="section">

    <loading-container v-bind:is-loading="isLoading">
      <nav class="level">
        <div class="level-left">
          <div class="level-item">
            <div>
              <h1 class="title"><i class="fas fa-home has-text-link"/> My Projects</h1>
            </div>
          </div>
        </div>
        <div class="level-right">
          <div class="level-item">
            <a v-on:click="newProject" class="button is-outlined is-success">
              <span>Add New Project</span>
              <span class="icon is-small">
                <i class="fas fa-plus-circle"/>
              </span>
            </a>
          </div>
        </div>
      </nav>

      <transition name="projectContainer" enter-active-class="animated fadeIn">
        <div>
          <div v-if="projects && projects.length" v-for="project of projects" :key="project.id" class="box">
            <my-project :project="project" v-on:project-deleted="projectRemoved" v-on:move-project-up="moveProjectUp" v-on:move-project-down="moveProjectDown"/>
          </div>
        </div>
      </transition>

      <no-content :should-display="!projects || projects.length==0" :title="'No Projects Yet'">
        <div slot="content" class="content" style="width: 100%;">
          <p class="has-text-centered">
          Create your first project today by pressing
          </p>
          <p class="has-text-centered">
            <a v-on:click="newProject" class="button is-outlined is-success">
              <span>Add New Project</span>
              <span class="icon is-small">
              <i class="fas fa-plus-circle"/>
            </span>
            </a>
          </p>
        </div>
      </no-content>

    </loading-container>

  </div>

</template>

<script>
  import MyProject from './MyProject';
  import EditProject from './EditProject';
  import LoadingContainer from '../utils/LoadingContainer';
  import NoContent from '../utils/NoContent';
  import ProjectService from './ProjectService';

  // EditProject.

  export default {
    name: 'MyProjects',
    data() {
      return {
        isLoading: true,
        projects: [],
      };
    },
    components: { NoContent, LoadingContainer, MyProject },
    mounted() {
      this.loadProjects();
    },
    methods: {
      loadProjects() {
        ProjectService.getProjects()
          .then((response) => {
            this.isLoading = false;
            this.projects = response;
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
        });
      },
      newProject() {
        this.$modal.open({
          parent: this,
          component: EditProject,
          hasModalCard: true,
          // width: 400,
          props: {
            project: { name: '', projectId: '' },
          },
          events: {
            'project-created': this.projectAdded,
          },
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


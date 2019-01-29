<template>
  <div>
    <div class="columns">
      <div class="column is-full">
        <span  class="title is-3">
          {{ project.name }}
        </span>
        <span class="has-text-grey"><span class="is-uppercase">Id:</span> {{ project.projectId }}</span>
        <edit-and-delete-dropdown v-on:deleted="deleteProject" v-on:edited="editProject" v-on:move-up="moveUp" v-on:move-down="moveDown"
                                  :is-first="project.isFirst" :is-last="project.isLast" :is-loading="isLoading"
                                  class="project-settings"></edit-and-delete-dropdown>
      </div>
    </div>
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
    <div class="columns has-text-centered">
      <div class="column is-full">
        <router-link :to="{ name:'ProjectPage',
              params: { projectId: this.project.projectId}}"
                     class="button is-outlined is-info">
          <span>Manage</span>
          <span class="icon is-small">
              <i class="fas fa-arrow-circle-right"/>
            </span>
        </router-link>
      </div>
    </div>
  </div>
</template>

<script>
  import axios from 'axios';
  import EditAndDeleteDropdown from '../utils/EditAndDeleteDropdown';
  import EditProject from './EditProject';

  export default {
    name: 'MyProject',
    components: { EditAndDeleteDropdown },
    props: ['project'],
    data() {
      return {
        isLoading: false,
        serverErrors: [],
      };
    },
    methods: {
      deleteProject() {
        this.$dialog.confirm({
          title: 'WARNING: Delete Project Action',
          message: `Project ID: <b>${this.project.projectId}</b> <br/><br/>Delete Action can not be undone and <b>permanently</b> removes its skill subject definitions, skill definitions and users' performed skills.`,
          confirmText: 'Delete',
          type: 'is-danger',
          hasIcon: true,
          icon: 'exclamation-triangle',
          iconPack: 'fa',
          scroll: 'keep',
          onConfirm: () => this.sendDeleteProjectEvent(),
        });
      },
      sendDeleteProjectEvent() {
        this.isLoading = true;
        this.$emit('project-deleted', this.project);
      },
      editProject() {
        this.$modal.open({
          parent: this,
          component: EditProject,
          hasModalCard: true,
          props: {
            project: this.project,
            isEdit: true,
          },
          events: {
            'project-created': this.projectSaved,
          },
        });
      },
      projectSaved(project) {
        this.isLoading = true;
        axios.put(`/admin/projects/${project.projectId}`, project)
          .then((res) => {
            this.isLoading = false;
            this.project = res.data;
          })
          .catch((e) => {
            this.isLoading = false;
            this.serverErrors.push(e);
            throw e;
        });
      },
      moveUp() {
        this.$emit('move-project-up', this.project);
      },
      moveDown() {
        this.$emit('move-project-down', this.project);
      },
    },
  };
</script>

<style scoped>
  .project-settings {
    position: relative;
    display: inline-block;
    float: right;
  }
</style>

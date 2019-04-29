<template>
  <div>
    <page-preview-card :options="cardOptions">
      <div slot="header-top-right">
        <edit-and-delete-dropdown v-on:deleted="deleteProject" v-on:edited="editProject" v-on:move-up="moveUp"
                                  v-on:move-down="moveDown"
                                  :is-first="projectInternal.isFirst" :is-last="projectInternal.isLast"
                                  :is-loading="isLoading"
                                  class="project-settings"></edit-and-delete-dropdown>
      </div>
      <div slot="footer">
        <router-link :to="{ name:'ProjectPage', params: { projectId: this.projectInternal.projectId}}"
                     class="btn btn-outline-primary">
          Manage <i class="fas fa-arrow-circle-right"/>
        </router-link>
      </div>
    </page-preview-card>

    <!--    <div class="columns">-->
    <!--      <div class="column is-full">-->
    <!--        <span  class="title is-3">-->
    <!--          {{ projectInternal.name }}-->
    <!--        </span>-->
    <!--        <span class="has-text-grey"><span class="is-uppercase">Id:</span> {{ projectInternal.projectId }}</span>-->
    <!--        <edit-and-delete-dropdown v-on:deleted="deleteProject" v-on:edited="editProject" v-on:move-up="moveUp" v-on:move-down="moveDown"-->
    <!--                                  :is-first="projectInternal.isFirst" :is-last="projectInternal.isLast" :is-loading="isLoading"-->
    <!--                                  class="project-settings"></edit-and-delete-dropdown>-->
    <!--      </div>-->
    <!--    </div>-->
    <!--    <div class="columns has-text-centered">-->
    <!--      <div class="column is-one-quarter">-->
    <!--        <div>-->
    <!--          <p class="heading">Subjects</p>-->
    <!--          <p class="title">{{ projectInternal.numSubjects | number}}</p>-->
    <!--        </div>-->
    <!--      </div>-->
    <!--      <div class="column is-one-quarter">-->
    <!--        <div>-->
    <!--          <p class="heading">Skills</p>-->
    <!--          <p class="title">{{ projectInternal.numSkills | number}}</p>-->
    <!--        </div>-->
    <!--      </div>-->

    <!--      <div class="column is-one-quarter">-->
    <!--        <div>-->
    <!--          <p class="heading">Total Points</p>-->
    <!--          <p class="title">{{ projectInternal.totalPoints | number }}-->
    <!--            <b-tooltip v-if="projectInternal.totalPoints < 100" label="Project has insufficient points assigned. Skills cannot be achieved until project has at least 100 points."-->
    <!--                       position="is-bottom" animanted="true" type="is-light" multilined>-->
    <!--              <span><i class="fa fa-exclamation-circle"></i></span>-->
    <!--            </b-tooltip>-->
    <!--          </p>-->
    <!--        </div>-->
    <!--      </div>-->
    <!--      <div class="column is-one-quarter">-->
    <!--        <div>-->
    <!--          <p class="heading">Users</p>-->
    <!--          <p class="title">{{ projectInternal.numUsers | number }}</p>-->
    <!--        </div>-->
    <!--      </div>-->
    <!--    </div>-->
    <!--    <div class="columns has-text-centered">-->
    <!--      <div class="column is-full">-->
    <!--        <router-link :to="{ name:'ProjectPage',-->
    <!--              params: { projectId: this.projectInternal.projectId}}"-->
    <!--                     class="button is-outlined is-info">-->
    <!--          <span>Manage</span>-->
    <!--          <span class="icon is-small">-->
    <!--              <i class="fas fa-arrow-circle-right"/>-->
    <!--            </span>-->
    <!--        </router-link>-->
    <!--      </div>-->
    <!--    </div>-->
  </div>
</template>

<script>
  import EditAndDeleteDropdown from '../utils/EditAndDeleteDropdown';
  import EditProject from './EditProject';
  import ProjectService from './ProjectService';
  import PagePreviewCard from '../utils/pages/PagePreviewCard';

  export default {
    name: 'MyProject',
    components: { PagePreviewCard, EditAndDeleteDropdown },
    props: ['project'],
    data() {
      return {
        isLoading: false,
        projectInternal: {},
        cardOptions: {},
      };
    },
    mounted() {
      this.projectInternal = this.project;
      this.cardOptions = {
        // icon: this.project.iconClass,
        title: this.projectInternal.name,
        subTitle: `ID: ${this.projectInternal.projectId}`,
        stats: [{
          label: 'Subjects',
          count: this.projectInternal.numSubjects,
        }, {
          label: 'Skills',
          count: this.projectInternal.numSkills,
        }, {
          label: 'Points',
          count: this.projectInternal.totalPoints,
        }, {
          label: 'Users',
          count: this.projectInternal.numUsers,
        }],
      };
    },
    methods: {
      deleteProject() {
        this.$dialog.confirm({
          title: 'WARNING: Delete Project Action',
          message: `Project ID: <b>${this.projectInternal.projectId}</b> <br/><br/>Delete Action can not be undone and <b>permanently</b> removes its skill subject definitions, skill definitions and users' performed skills.`,
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
        this.$emit('project-deleted', this.projectInternal);
      },
      editProject() {
        this.$modal.open({
          parent: this,
          component: EditProject,
          hasModalCard: true,
          props: {
            project: this.projectInternal,
            isEdit: true,
          },
          events: {
            'project-created': this.projectSaved,
          },
        });
      },
      projectSaved(project) {
        this.isLoading = true;
        ProjectService.saveProject(project)
          .then((res) => {
            this.isLoading = false;
            this.projectInternal = res;
          })
          .finally(() => {
            this.isLoading = false;
          });
      },
      moveUp() {
        this.$emit('move-project-up', this.projectInternal);
      },
      moveDown() {
        this.$emit('move-project-down', this.projectInternal);
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

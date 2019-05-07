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

    <edit-project v-if="showEditProjectModal" v-model="showEditProjectModal" :project="projectInternal" :is-edit="true"
      @project-saved="projectSaved"/>
  </div>
</template>

<script>
  import EditAndDeleteDropdown from '../utils/EditAndDeleteDropdown';
  import EditProject from './EditProject';
  import ProjectService from './ProjectService';
  import PagePreviewCard from '../utils/pages/PagePreviewCard';
  import MsgBoxMixin from '../utils/modal/MsgBoxMixin';

  export default {
    name: 'MyProject',
    components: { EditProject, PagePreviewCard, EditAndDeleteDropdown },
    props: ['project'],
    mixins: [MsgBoxMixin],
    data() {
      return {
        isLoading: false,
        projectInternal: {},
        cardOptions: {},
        showEditProjectModal: false,
      };
    },
    mounted() {
      this.projectInternal = this.project;
      this.createCardOptions();
    },
    methods: {
      createCardOptions() {
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
            warn: this.projectInternal.totalPoints < 100,
            warnMsg: 'Project has insufficient points assigned. Skills cannot be achieved until project has at least 100 points.',
          }, {
            label: 'Users',
            count: this.projectInternal.numUsers,
          }],
        };
      },

      deleteProject() {
        const msg = `Project ID [${this.projectInternal.projectId}]. Delete Action can not be undone and permanently removes its skill subject definitions, skill definitions and users' performed skills.`;
        this.msgConfirm(msg)
          .then((res) => {
            if (res) {
              this.$emit('project-deleted', this.projectInternal);
            }
          });
      },
      editProject() {
        this.showEditProjectModal = true;
      },
      projectSaved(project) {
        this.isLoading = true;
        ProjectService.saveProject(project)
          .then((res) => {
            this.projectInternal = res;
            this.createCardOptions();
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

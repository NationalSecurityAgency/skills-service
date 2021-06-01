/*
Copyright 2020 SkillTree

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    https://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
<template>
  <div>
    <page-header :loading="isLoading" :options="headerOptions">
      <div slot="banner" v-if="project && project.expiring" data-cy="projectExpiration" class="w-100 text-center alert-danger p-2 mb-3">
          <span class="" v-b-tooltip.hover="'This Project has not been used recently, it will  be deleted unless you explicitly retain it'">
            Project has not been used in over {{this.$store.getters.config.expireUnusedProjectsOlderThan}} days and will be deleted
          </span><slim-date-cell cssClass="alert-danger"  :value="expirationDate" :fromStartOfDay="true"/>
          <b-button @click="keepIt" data-cy="keepIt" size="sm" variant="alert"
                    :aria-label="'Keep Project '+ project.name">
            <span class="d-none d-sm-inline">Keep It</span> <b-spinner v-if="cancellingExpiration" small style="font-size:1rem"/><i v-if="!cancellingExpiration" :class="'fas fa-shield-alt'" style="font-size: 1rem;" aria-hidden="true"/>
          </b-button>
      </div>
      <div slot="subSubTitle" v-if="project">
          <div data-cy="projectCreated">
            <span class="text-secondary small font-italic">Created: </span><slim-date-cell :value="project.created"/>
          </div>
          <div data-cy="projectLastReportedSkill">
            <span class="text-secondary small font-italic">Last reported Skill: </span><slim-date-cell :value="project.lastReportedSkill" :fromStartOfDay="true"/>
          </div>
        <b-button-group>
          <b-button @click="displayEditProject"
                    ref="editProjectButton"
                    class="btn btn-outline-primary mr-1"
                    size="sm"
                    variant="outline-primary"
                    data-cy="btn_edit-project"
                    :aria-label="'edit Project '+project.projectId">
            <span class="d-none d-sm-inline">Edit </span> <i class="fas fa-edit" aria-hidden="true"/>
          </b-button>
          <b-button target="_blank" v-if="project" :to="{ name:'MyProjectSkills', params: { projectId: project.projectId } }"
                    data-cy="projectPreview" size="sm"
                    variant="outline-primary" :aria-label="'preview client display for project'+project.name">
            <span class="d-sm-line">Preview</span> <i class="fas fa-eye" style="font-size:1rem;" aria-hidden="true"/>
          </b-button>
        </b-button-group>
      </div>
    </page-header>

    <navigation v-if="!isLoading" :nav-items="[
          {name: 'Subjects', iconClass: 'fa-cubes skills-color-subjects', page: 'Subjects'},
          {name: 'Badges', iconClass: 'fa-award skills-color-badges', page: 'Badges'},
          {name: 'Self Report', iconClass: 'fa-laptop skills-color-selfreport', page: 'SelfReport'},
          {name: 'Dependencies', iconClass: 'fa-project-diagram skills-color-dependencies', page: 'FullDependencyGraph'},
          {name: 'Cross Projects', iconClass: 'fa-handshake skills-color-crossProjects', page: 'CrossProjectsSkills'},
          {name: 'Levels', iconClass: 'fa-trophy skills-color-levels', page: 'ProjectLevels'},
          {name: 'Users', iconClass: 'fa-users skills-color-users', page: 'ProjectUsers'},
          {name: 'Metrics', iconClass: 'fa-chart-bar skills-color-metrics', page: 'ProjectMetrics'},
          {name: 'Issues', iconClass: 'fas fa-exclamation-triangle', page: 'ProjectErrorsPage'},
          {name: 'Access', iconClass: 'fa-shield-alt skills-color-access', page: 'ProjectAccess'},
          {name: 'Settings', iconClass: 'fa-cogs skills-color-settings', page: 'ProjectSettings'},
        ]">
    </navigation>

    <edit-project v-if="editProject" v-model="editProject" :project="project" :is-edit="true"
                  @project-saved="projectSaved" @hidden="editProjectHidden"/>
  </div>

</template>

<script>
  import { createNamespacedHelpers } from 'vuex';

  import Navigation from '../utils/Navigation';
  import PageHeader from '../utils/pages/PageHeader';
  import SlimDateCell from '../utils/table/SlimDateCell';
  import dayjs from '../../DayJsCustomizer';
  import ProjectService from './ProjectService';
  import EditProject from './EditProject';

  const { mapActions, mapGetters, mapMutations } = createNamespacedHelpers('projects');

  export default {
    name: 'ProjectPage',
    components: {
      PageHeader,
      Navigation,
      SlimDateCell,
      EditProject,
    },
    data() {
      return {
        isLoading: true,
        cancellingExpiration: false,
        editProject: false,
      };
    },
    mounted() {
      this.loadProjects();
    },
    computed: {
      ...mapGetters([
        'project',
      ]),
      headerOptions() {
        if (!this.project) {
          return {};
        }
        return {
          icon: 'fas fa-list-alt skills-color-projects',
          title: `PROJECT: ${this.project.name}`,
          subTitle: `ID: ${this.project.projectId}`,
          stats: [{
            label: 'Subjects',
            count: this.project.numSubjects,
            icon: 'fas fa-cubes skills-color-subjects',
          }, {
            label: 'Skills',
            count: this.project.numSkills,
            icon: 'fas fa-graduation-cap skills-color-skills',
          }, {
            label: 'Points',
            count: this.project.totalPoints,
            warnMsg: this.project.totalPoints < this.minimumPoints ? 'Project has insufficient points assigned. Skills cannot be achieved until project has at least 100 points.' : null,
            icon: 'far fa-arrow-alt-circle-up skills-color-points',
          }, {
            label: 'Badges',
            count: this.project.numBadges,
            icon: 'fas fa-award skills-color-badges',
          }, {
            label: 'Issues',
            count: this.project.numErrors,
            icon: 'fas fa-exclamation-triangle',
          }],
        };
      },
      minimumPoints() {
        return this.$store.getters.config.minimumProjectPoints;
      },
      expirationDate() {
        if (!this.project.expiring) {
          return '';
        }
        const gracePeriodInDays = this.$store.getters.config.expirationGracePeriod;
        const expires = dayjs(this.project.expirationTriggered).add(gracePeriodInDays, 'day').startOf('day');
        return expires.format('YYYY-MM-DD HH:mm');
      },
    },
    methods: {
      ...mapActions([
        'loadProjectDetailsState',
      ]),
      ...mapMutations([
        'setProject',
      ]),
      displayEditProject() {
        this.editProject = true;
      },
      loadProjects() {
        this.isLoading = true;
        if (this.$route.params.project) {
          this.setProject(this.$route.params.project);
          this.isLoading = false;
        } else {
          this.loadProjectDetailsState({ projectId: this.$route.params.projectId })
            .finally(() => {
              this.isLoading = false;
            });
        }
      },
      editProjectHidden() {
        this.editProject = false;
        this.$nextTick(() => {
          const ref = this.$refs.editProjectButton;
          if (ref) {
            ref.focus();
          }
        });
      },
      projectSaved(updatedProject) {
        ProjectService.saveProject(updatedProject).then((resp) => {
          const origProjId = this.project.projectId;
          this.setProject(resp);
          if (resp.projectId !== origProjId) {
            this.$router.replace({ name: this.$route.name, params: { ...this.$route.params, projectId: resp.projectId } });
            this.projectId = resp.projectId;
          }
        });
      },
      keepIt() {
        this.cancellingExpiration = true;
        ProjectService.cancelUnusedProjectDeletion(this.$route.params.projectId).then(() => {
          this.loadProjects();
        }).finally(() => {
          this.cancellingExpiration = false;
        });
      },
    },
  };
</script>

<style scoped>

</style>

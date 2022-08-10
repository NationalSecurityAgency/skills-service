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
    <sub-page-header title="Projects" action="Project">
      <b-button v-if="isRootUser" variant="outline-primary" ref="pinProjectsButton"
                @click="showSearchProjectModal=true"
                aria-label="Pin projects to your Project page"
                role="button"
                size="sm"
                class="mr-2">
        <span class="d-none d-sm-inline">Pin</span> <i class="fas fa-thumbtack" aria-hidden="true"/>
      </b-button>
      <b-button id="newProjectBtn" ref="newProjButton" @click="editNewProject()"
                variant="outline-primary" size="sm"
                :disabled="addProjectDisabled"
                data-cy="newProjectButton" aria-label="Create new Project" role="button">
            <span class="d-none d-sm-inline">Project</span> <i class="fas fa-plus-circle" aria-hidden="true"/>
          </b-button>
    </sub-page-header>

    <loading-container v-bind:is-loading="isLoading">
      <div v-if="addProjectDisabled" class="alert alert-warning" data-cy="addProjectDisabled">
        <i class="fas fa-exclamation-circle"/> Cannot create or copy projects -
        {{ addProjectsDisabledMsg }}
      </div>

      <div v-if="useTableView">
        <projects-table ref="projectsTable" :projects="projects" @project-deleted="projectRemoved"
                        @copy-project="copyProject"
                        :copy-project-disabled="addProjectDisabled"
                        @project-edited="projectEdited"></projects-table>
      </div>
      <div v-else id="projectCards">
        <div v-for="project of projects" :key="project.projectId" class="mb-3"
             :id="project.projectId">
          <b-overlay :show="sortOrder.loading" rounded="sm" opacity="0.4">
            <template #overlay>
              <div class="text-center" :data-cy="`${project.projectId}_overlayShown`">
                <div v-if="project.projectId===sortOrder.loadingProjectId"
                     data-cy="updatingSortMsg">
                  <div class="text-info text-uppercase mb-1">Updating sort order!</div>
                  <b-spinner label="Loading..." style="width: 3rem; height: 3rem;" variant="info"/>
                </div>
              </div>
            </template>
            <my-project :id="`proj${project.projectId}`" tabindex="-1"
                        :project="project" :disable-sort-control="projects.length === 1"
                        :ref="`proj${project.projectId}`"
                        @sort-changed-requested="updateSortAndReloadProjects"
                        @copy-project="copyProject"
                        v-on:project-deleted="projectRemoved" v-on:pin-removed="projectUnpinned"/>
          </b-overlay>
        </div>
      </div>

      <no-content2 v-if="!projects || projects.length==0" icon="fas fa-hand-spock" class="mt-4"
                   title="No Projects Yet..." message="A Project represents a gamified training profile that consists of skills divided into subjects. Create as many Projects as you need.">
        <div class="mt-3">
          <span v-if="!isRootUser && isProgressAndRankingEnabled()" class="text-muted" style="font-size: .90em">
            Note: This section of SkillTree is for <strong>project administrators only</strong>. If you do not plan on creating and integrating a project with SkillTree then please return to the
            <router-link to="/">
              <span class="skill-url">Progress and Ranking</span>
            </router-link> page.
          </span>
          <span v-if="isRootUser || !isProgressAndRankingEnabled()">
            <b-card class="mb-5 mt-2 px-5">
              A Project represents a gamified training profile that consists of skills divided into subjects. Create as many Projects as you need.
              <hr />
              <div class="mt-2">Please click</div>
              <div class="my-2">
                <b-button id="firstNewProjectBtn" @click="editNewProject()" variant="outline-primary" size="sm"
                          aria-label="Create new project"
                          data-cy="firstNewProjectButton" class="animate__bounceIn">
                  <span class="d-none d-sm-inline">Project</span> <i class="fas fa-plus-circle" aria-hidden="true"/>
                </b-button>
              </div>
              <div>
              on the <b>top-right</b> to create your first project.
              </div>
            </b-card>
          </span>
        </div>
      </no-content2>
    </loading-container>

    <edit-project v-if="newProject.show" v-model="newProject.show" :project="newProject.project"
                  @project-saved="projectAdded" @hidden="handleHide" :is-edit="newProject.isEdit"/>
    <pin-projects v-if="showSearchProjectModal" v-model="showSearchProjectModal"
                  @done="pinModalClosed"/>
    <lengthy-operation-progress-bar-modal v-if="copyProgressModal.show"
                                          v-model="copyProgressModal.show"
                                          :is-complete="copyProgressModal.isComplete"
                                          @operation-done="loadProjectsAfterCopy"
                                          title="Copying Project's Training Profile"
                                          success-message="Project was successfully copied, please enjoy!"/>
  </div>

</template>

<script>
  import Sortable from 'sortablejs';
  import { SkillsReporter } from '@skilltree/skills-client-vue';
  import MyProject from './MyProject';
  import EditProject from './EditProject';
  import LoadingContainer from '../utils/LoadingContainer';
  import ProjectService from './ProjectService';
  import SubPageHeader from '../utils/pages/SubPageHeader';
  import NoContent2 from '../utils/NoContent2';
  import PinProjects from './PinProjects';
  import ProjectsTable from './ProjectsTable';
  import SettingsService from '../settings/SettingsService';
  import LengthyOperationProgressBarModal
    from '@/components/utils/modal/LengthyOperationProgressBarModal';

  export default {
    name: 'MyProjects',
    data() {
      return {
        isLoading: true,
        projects: [],
        newProject: {
          show: false,
          isEdit: false,
          project: {
            name: '',
            projectId: '',
          },
        },
        showSearchProjectModal: false,
        sortOrder: {
          loading: false,
          loadingProjectId: '-1',
        },
        copyProgressModal: {
          show: false,
          isComplete: false,
          copiedProjectId: '',
        },
      };
    },
    components: {
      LengthyOperationProgressBarModal,
      PinProjects,
      NoContent2,
      SubPageHeader,
      LoadingContainer,
      MyProject,
      EditProject,
      ProjectsTable,
    },
    mounted() {
      this.loadProjects();
    },
    computed: {
      addProjectDisabled() {
        return this.projects && this.$store.getters.config && this.projects.length >= this.$store.getters.config.maxProjectsPerAdmin;
      },
      addProjectsDisabledMsg() {
        if (this.$store.getters.config) {
          return `The maximum number of Projects allowed is ${this.$store.getters.config.maxProjectsPerAdmin}`;
        }
        return '';
      },
      isRootUser() {
        return this.$store.getters['access/isRoot'];
      },
      useTableView() {
        return this.projects && this.$store.getters.config && this.projects.length >= this.$store.getters.config.numProjectsForTableView;
      },
    },
    methods: {
      handleHide() {
        this.$nextTick(() => {
          this.$refs.newProjButton.focus();
        });
      },
      pinModalClosed() {
        this.showSearchProjectModal = false;
        this.loadProjects();
        this.$nextTick(() => {
          this.$refs.pinProjectsButton.focus();
        });
      },
      projectUnpinned(project) {
        this.loadProjects().then(() => {
          this.$nextTick(() => {
            this.$announcer.polite(`Project ${project.name} has been unpinned from the root user projects view`);
          });
        });
      },
      loadProjects() {
        this.isLoading = true;
        return ProjectService.getProjects()
          .then((response) => {
            this.projects = response;
          })
          .finally(() => {
            this.isLoading = false;
            this.enableDropAndDrop();
          });
      },
      projectRemoved(project) {
        this.isLoading = true;
        ProjectService.deleteProject(project.projectId)
          .then(() => {
            this.loadProjects();
            this.$announcer.polite(`Project ${project.name} has been deleted`);
          });
      },
      copyProject(projectInfo) {
        this.copyProgressModal.isComplete = false;
        this.copyProgressModal.copiedProjectId = '';
        this.copyProgressModal.show = true;
        ProjectService.copyProject(projectInfo.originalProjectId, projectInfo.newProject)
          .then(() => {
            this.copyProgressModal.copiedProjectId = projectInfo.newProject.projectId;
            this.copyProgressModal.isComplete = true;
            this.$announcer.polite(`Project ${projectInfo.newProject.name} was copied`);
          });
      },
      loadProjectsAfterCopy() {
        this.loadProjects()
          .then(() => {
            this.focusOnProjectCard(this.copyProgressModal.copiedProjectId);
          });
      },
      projectAdded(project) {
        this.isLoading = true;
        return ProjectService.saveProject(project)
          .then(() => {
            const loadProjects = () => {
              SkillsReporter.reportSkill('CreateProject');
              this.loadProjects()
                .then(() => {
                  this.$nextTick(() => this.$announcer.polite(`Project ${project.name} has been created`));
                });
            };

            if (this.isRootUser) {
              SettingsService.pinProject(project.projectId)
                .then(() => {
                  loadProjects();
                });
            } else {
              loadProjects();
            }
          });
      },
      editNewProject() {
        this.newProject = {
          show: true,
          isEdit: false,
          project: {
            name: '',
            projectId: '',
          },
        };
      },
      projectEdited(editedProject) {
        ProjectService.saveProject(editedProject).then(() => {
          this.loadProjects().then(() => {
            this.$refs.projectsTable.focusOnEditButton(editedProject.projectId);
            this.$nextTick(() => {
              if (editedProject.isEdit) {
                this.$announcer.polite(`Project ${editedProject.name} has been edited`);
              } else {
                this.$announcer.polite(`Project ${editedProject.name} has been created`);
              }
            });
          });
        });
      },
      enableDropAndDrop() {
        if (this.projects && this.projects.length > 0
          && this.$store.getters.config && this.projects.length < this.$store.getters.config.numProjectsForTableView) {
          const self = this;
          this.$nextTick(() => {
            const cards = document.getElementById('projectCards');
            // need to check for null because this logic is within nextTick method
            // an may actually run after the user moved onto another page
            if (cards) {
              Sortable.create(cards, {
                handle: '.sort-control',
                animation: 150,
                ghostClass: 'skills-sort-order-ghost-class',
                onUpdate(event) {
                  self.sortOrderUpdate(event);
                },
              });
            }
          });
        }
      },
      sortOrderUpdate(updateEvent) {
        const { id } = updateEvent.item;
        this.sortOrder.loadingProjectId = id;
        this.sortOrder.loading = true;
        ProjectService.updateProjectDisplaySortOrder(id, updateEvent.newIndex)
          .finally(() => {
            this.sortOrder.loading = false;
          });
      },
      updateSortAndReloadProjects(updateInfo) {
        const currentIndex = this.projects.sort((a, b) => {
          if (a.displayOrder > b.displayOrder) {
            return 1;
          }
          if (b.displayOrder > a.displayOrder) {
            return -1;
          }
          return 0;
        })
          .findIndex((item) => item.projectId === updateInfo.projectId);
        const newIndex = updateInfo.direction === 'up' ? currentIndex - 1 : currentIndex + 1;
        if (newIndex >= 0 && (newIndex) < this.projects.length) {
          this.isLoading = true;
          ProjectService.updateProjectDisplaySortOrder(updateInfo.projectId, newIndex)
            .finally(() => {
              this.loadProjects()
                .then(() => {
                  const foundRef = this.$refs[`proj${updateInfo.projectId}`];
                  this.$nextTick(() => {
                    foundRef[0].focusSortControl();
                  });
                });
            });
        }
      },
      isProgressAndRankingEnabled() {
        return this.$store.getters.config.rankingAndProgressViewsEnabled === true || this.$store.getters.config.rankingAndProgressViewsEnabled === 'true';
      },
      focusOnProjectCard(projectId) {
        this.$nextTick(() => {
          const projCard = document.getElementById(`proj${projectId}`);
          if (projCard) {
            projCard.focus();
          }
        });
      },
    },
  };
</script>

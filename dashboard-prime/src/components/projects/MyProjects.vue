<script setup>
import { ref, computed, onMounted, nextTick } from 'vue';
import { useStore } from 'vuex';
import { useSkillsAnnouncer } from '@/common-components/utilities/UseSkillsAnnouncer.js'
import SubPageHeader from '@/components/utils/pages/SubPageHeader.vue';
import LoadingContainer from '@/components/utils/LoadingContainer.vue';
import SkillsSpinner from '@/components/utils/SkillsSpinner.vue';
import ProjectService from '@/components/projects/ProjectService';
import SettingsService from '@/components/settings/SettingsService';
import MyProject from '@/components/projects/MyProject.vue';
import EditProject from '@/components/projects/EditProject.vue'
import { SkillsReporter } from '@skilltree/skills-client-js'

const store = useStore();
const announcer = useSkillsAnnouncer()

onMounted(() => {
  loadProjects();
})

let isLoading = ref(false);
let projects = ref([]);
let newProject = ref({
  show: false,
  isEdit: false,
  project: {
    name: '',
    projectId: '',
  },
});
let showSearchProjectModal = false;
let sortOrder = {
  loading: false,
  loadingProjectId: '-1',
};
let copyProgressModal = {
  show: false,
  isComplete: false,
  copiedProjectId: '',
};

const addProjectDisabled = computed(() => {
  return projects && store.getters.config && projects.length >= store.getters.config.maxProjectsPerAdmin && !store.getters['access/isRoot'];
});

const addProjectsDisabledMsg = computed(() => {
  if (store.getters.config) {
    return `The maximum number of Projects allowed is ${store.getters.config.maxProjectsPerAdmin}`;
  }
  return '';
});

const isRootUser = computed(() => {
  return store.getters['access/isRoot'];
});

const useTableView = computed(() => {
  return projects && store.getters.config && projects.length >= store.getters.config.numProjectsForTableView;
});

// Functions
const handleHide = () => {
  nextTick(() => {
    this.$refs.newProjButton.focus();
  });
};
const pinModalClosed = () => {
  showSearchProjectModal = false;
  loadProjects();
  nextTick(() => {
    this.$refs.pinProjectsButton.focus();
  });
};
const projectUnpinned = (project) => {
  loadProjects().then(() => {
    nextTick(() => {
      // this.$announcer.polite(`Project ${project.name} has been unpinned from the root user projects view`);
    });
  });
};
const loadProjects = () => {
  isLoading.value = true;
  return ProjectService.getProjects()
      .then((response) => {
        projects.value = response;
      })
      .finally(() => {
        isLoading.value = false;
        // enableDropAndDrop();
      });
};
const projectRemoved = (project) => {
  isLoading.value = true;
  ProjectService.deleteProject(project.projectId)
      .then(() => {
        loadProjects();
        // this.$announcer.polite(`Project ${project.name} has been deleted`);
      });
};
const copyProject = (projectInfo) => {
  copyProgressModal.isComplete = false;
  copyProgressModal.copiedProjectId = '';
  copyProgressModal.show = true;
  ProjectService.copyProject(projectInfo.originalProjectId, projectInfo.newProject)
      .then(() => {
        copyProgressModal.copiedProjectId = projectInfo.newProject.projectId;
        copyProgressModal.isComplete = true;
        // this.$announcer.polite(`Project ${projectInfo.newProject.name} was copied`);
        SkillsReporter.reportSkill('CopyProject');
      });
};
const loadProjectsAfterCopy = () => {
  loadProjects()
      .then(() => {
        focusOnProjectCard(copyProgressModal.copiedProjectId);
      });
};
const projectAdded = (project) => {
  isLoading.value = true;
  return ProjectService.saveProject(project)
      .then(() => {
        const loadProjectsInternal = () => {
          SkillsReporter.reportSkill('CreateProject');
          loadProjects()
              .then(() => {
                announcer.polite(`Project ${project.name} has been created`);
              });
        };

        if (isRootUser) {
          SettingsService.pinProject(project.projectId)
              .then(() => {
                loadProjectsInternal();
              });
        } else {
          loadProjectsInternal();
        }
      });
};
const projectEdited = (editedProject) => {
  ProjectService.saveProject(editedProject).then(() => {
    loadProjects().then(() => {
      this.$refs.projectsTable.focusOnEditButton(editedProject.projectId);
      nextTick(() => {
        if (editedProject.isEdit) {
          // this.$announcer.polite(`Project ${editedProject.name} has been edited`);
        } else {
          // this.$announcer.polite(`Project ${editedProject.name} has been created`);
        }
      });
    });
  });
};
const enableDropAndDrop = () => {
  if (projects && projects.length > 0
      && store.getters.config && projects.length < store.getters.config.numProjectsForTableView) {
    const self = this;
    nextTick(() => {
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
};
const sortOrderUpdate = (updateEvent) => {
  const { id } = updateEvent.item;
  sortOrder.loadingProjectId = id;
  sortOrder.loading = true;
  ProjectService.updateProjectDisplaySortOrder(id, updateEvent.newIndex)
      .finally(() => {
        sortOrder.loading = false;
      });
};
const updateSortAndReloadProjects = (updateInfo) => {
  const currentIndex = projects.sort((a, b) => {
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
  if (newIndex >= 0 && (newIndex) < projects.length) {
    isLoading.value = true;
    ProjectService.updateProjectDisplaySortOrder(updateInfo.projectId, newIndex)
        .finally(() => {
          loadProjects()
              .then(() => {
                const foundRef = this.$refs[`proj${updateInfo.projectId}`];
                nextTick(() => {
                  foundRef[0].focusSortControl();
                });
              });
        });
  }
};
const isProgressAndRankingEnabled = () => {
  return store.getters.config.rankingAndProgressViewsEnabled === true || store.getters.config.rankingAndProgressViewsEnabled === 'true';
};
const focusOnProjectCard = (projectId) => {
  nextTick(() => {
    const projCard = document.getElementById(`proj${projectId}`);
    if (projCard) {
      projCard.focus();
    }
  });
};
</script>

<template>
  <div>
    <SubPageHeader title="Projects" action="Project">
      <Button v-if="isRootUser"
              outlined
              ref="pinProjectsButton"
              @click="showSearchProjectModal=true"
              aria-label="Pin projects to your Project page"
              role="button"
              size="small"
              class="mr-2 bg-primary-reverse">
        <span class="d-none d-sm-inline mr-1">Pin</span> <i class="fas fa-thumbtack" aria-hidden="true" />
      </Button>
      <Button id="newProjectBtn" ref="newProjButton" @click="newProject.show = true"
              outlined class="bg-primary-reverse" size="small"
              :disabled="addProjectDisabled"
              data-cy="newProjectButton" aria-label="Create new Project" role="button">
        <span class="d-none d-sm-inline  mr-1">Project</span> <i class="fas fa-plus-circle" aria-hidden="true" />
      </Button>
    </SubPageHeader>

    <LoadingContainer v-bind:is-loading="isLoading">
      <div v-if="addProjectDisabled" class="alert alert-warning" data-cy="addProjectDisabled">
        <i class="fas fa-exclamation-circle"/> Cannot create or copy projects -
        {{ addProjectsDisabledMsg }}
      </div>

      <div v-if="useTableView">
<!--        <projects-table ref="projectsTable" :projects="projects" @project-deleted="projectRemoved"-->
<!--                        @copy-project="copyProject"-->
<!--                        :copy-project-disabled="addProjectDisabled"-->
<!--                        @project-edited="projectEdited"-->
<!--                        @pin-removed="projectUnpinned">-->

<!--        </projects-table>-->
      </div>
      <div v-else id="projectCards">
        <div v-for="project of projects" :key="project.projectId" class="mb-3"
             :id="project.projectId">
<!--          <b-overlay :show="sortOrder.loading" rounded="sm" opacity="0.4">-->
<!--            <template #overlay>-->
              <div class="text-center" :data-cy="`${project.projectId}_overlayShown`">
                <div v-if="project.projectId===sortOrder.loadingProjectId"
                     data-cy="updatingSortMsg">
                  <div class="text-info text-uppercase mb-1">Updating sort order!</div>
                  <SkillsSpinner label="Loading..." style="width: 3rem; height: 3rem;" variant="info"/>
                </div>
              </div>
<!--            </template>-->
            <MyProject :id="`proj${project.projectId}`" tabindex="-1"
                        :project="project" :disable-sort-control="projects.length === 1"
                        :ref="`proj${project.projectId}`"
                        @sort-changed-requested="updateSortAndReloadProjects"
                        @copy-project="copyProject"
                        v-on:project-deleted="projectRemoved" v-on:pin-removed="projectUnpinned"/>
<!--          </b-overlay>-->
        </div>
      </div>
    </LoadingContainer>

    <edit-project
      v-model="newProject.show"
      :project="newProject.project"
      @project-saved="projectAdded"/>
  </div>
</template>

<style scoped></style>

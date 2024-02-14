<script setup>
import { ref, computed, onMounted, nextTick, provide } from 'vue'
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
import NoContent2 from '@/components/utils/NoContent2.vue'

const store = useStore();
const announcer = useSkillsAnnouncer()

onMounted(() => {
  loadProjects();
})

const isLoading = ref(false);
const projects = ref([]);
const newProject = ref({
  show: false,
  isEdit: false,
  project: {},
});
const showSearchProjectModal = ref(false);
const sortOrder = {
  loading: false,
  loadingProjectId: '-1',
};
const copyProgressModal = {
  show: false,
  isComplete: false,
  copiedProjectId: '',
};

const addProjectDisabled = computed(() => {
  return projects.value && store.getters.config && projects.value.length >= store.getters.config.maxProjectsPerAdmin && !store.getters['access/isRoot'];
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

// Functions
const pinModalClosed = () => {
  showSearchProjectModal.value = false;
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
        projects.value = response.map((p) => ({ ...p, description: p.description || '' }))
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
        announcer.polite(`Project ${project.name} has been deleted`);
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


const openProjectModal = (project = {}, isEdit = false) => {
  newProject.value.isEdit = isEdit
  newProject.value.project = project
  newProject.value.show = true;
};
provide('createOrUpdateProject', openProjectModal)

const projectAdded = (project) => {
  const existingIndex = projects.value.findIndex((item) => item.projectId === project.originalProjectId)
  if (existingIndex >= 0) {
    projects.value.splice(existingIndex, 1, project)
  } else {
    projects.value.push(project)
    SkillsReporter.reportSkill('CreateProject');
  }
  announcer.polite(`Project ${project.name} has been created`);
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

const hasData = computed(() => {
  return !isLoading.value && projects.value && projects.value.length > 0
})
</script>

<template>
  <div>
    <SubPageHeader title="Projects" action="Project">
      <SkillsButton
        v-if="isRootUser"
        label="Pin"
        icon="fas fa-thumbtack"
        outlined
        ref="pinProjectsButton"
        @click="showSearchProjectModal=true"
        aria-label="Pin projects to your Project page"
        role="button"
        size="small"
        class="mr-2 bg-primary-reverse" />
      <SkillsButton
        label="Project"
        icon="fas fa-plus-circle"
        id="newProjectBtn"
        ref="newProjButton"
        @click="newProject.show = true"
        outlined class="bg-primary-reverse"
        size="small"
        :disabled="addProjectDisabled"
        data-cy="newProjectButton"
        aria-label="Create new Project"
        :track-for-focus="true"
        role="button" />
    </SubPageHeader>

<!--    <div v-if="addProjectDisabled" class="alert alert-warning" data-cy="addProjectDisabled">-->
<!--      <i class="fas fa-exclamation-circle"/> Cannot create or copy projects - -->
<!--      {{ addProjectsDisabledMsg }}-->
<!--    </div>-->

    <SkillsSpinner :is-loading="isLoading" class="my-5" />

    <div v-if="hasData" id="projectCards">
      <div v-for="project of projects" :key="project.projectId" class="mb-3"
           :id="project.projectId">
        <!--          <b-overlay :show="sortOrder.loading" rounded="sm" opacity="0.4">-->
        <!--            <template #overlay>-->
        <div class="text-center" :data-cy="`${project.projectId}_overlayShown`">
          <div v-if="project.projectId===sortOrder.loadingProjectId"
               data-cy="updatingSortMsg">
            <div class="text-info text-uppercase mb-1">Updating sort order!</div>
            <SkillsSpinner label="Loading..." style="width: 3rem; height: 3rem;" variant="info" />
          </div>
        </div>
        <!--            </template>-->
        <MyProject :id="`proj${project.projectId}`" tabindex="-1"
                   :project="project" :disable-sort-control="projects.length === 1"
                   :ref="`proj${project.projectId}`"
                   @sort-changed-requested="updateSortAndReloadProjects"
                   @copy-project="copyProject"
                   v-on:project-deleted="projectRemoved" v-on:pin-removed="projectUnpinned" />
        <!--          </b-overlay>-->
      </div>
    </div>

    <NoContent2
      v-if="!hasData"
      title="No Projects Yet..."
      class="my-5"
      message="A Project represents a gamified training profile that consists of skills divided into subjects. Create as many Projects as you need."
      data-cy="noProjectsYet" />
    <edit-project
      v-if="newProject.show"
      v-model="newProject.show"
      :project="newProject.project"
      @project-saved="projectAdded"
      :enable-return-focus="true"/>
  </div>
</template>

<style scoped></style>

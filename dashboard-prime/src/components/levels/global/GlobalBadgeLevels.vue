<script setup>
import { ref, computed, onMounted, nextTick } from 'vue';
import { useRoute } from 'vue-router';
import GlobalBadgeService from "@/components/badges/global/GlobalBadgeService.js";
import SubPageHeader from "@/components/utils/pages/SubPageHeader.vue";
import LoadingContainer from "@/components/utils/LoadingContainer.vue";
import ProjectSelector from "@/components/levels/global/ProjectSelector.vue";
import LevelSelector from "@/components/levels/global/LevelSelector.vue";

const route = useRoute();
const emit = defineEmits(['global-badge-levels-changed']);

const selectedProject = ref( null);
const selectedLevel = ref( null);
const isLoading = ref( true);
const loadingAvailableProjects = ref( false);
const levelPlaceholder = ref( 'First choose a Project');
const badge = ref( null);
const badgeId = ref( null);
const badgeLevels = ref( []);
const showChangeLevel = ref( false);
const projectLevelId = ref( null);
const projectLevel = ref( null);
const projectLevelName = ref( null);
const availableProjects = ref( []);
const afterListSlotText = ref( '');
const projectSearch = ref( '');

onMounted(() => {
  badgeId.value = route.params.badgeId;
  loadBadgeLevels();
  loadProjectsForBadge();
});

const selectedProjectId = computed(() => {
  let selectedProjectId = null;
  if (selectedProject.value) {
    selectedProjectId = selectedProject.value.projectId;
  }
  return selectedProjectId;
});

const loadBadgeLevels = () => {
  if (route.params.badge) {
    badge.value = route.params.badge;
    badgeLevels.value = badge.value.requiredProjectLevels;
    isLoading.value = false;
  } else {
    GlobalBadgeService.getBadge(badgeId.value)
        .then((response) => {
          badge.value = response;
          badgeLevels.value = response.requiredProjectLevels;
          isLoading.value = false;
        });
  }
};

const addLevel = () => {
  GlobalBadgeService.assignProjectLevelToBadge(badgeId.value, selectedProject.value.projectId, selectedLevel.value)
      .then(() => {
        const newLevel = {
          badgeId: badgeId.value,
          projectId: selectedProject.value.projectId,
          projectName: selectedProject.value.name,
          level: selectedLevel.value,
        };
        badgeLevels.value.push(newLevel);
        selectedLevel.value = null;
        // const selectedProject = selectedProject.value.name;
        // const { selectedLevel } = this;
        // loadGlobalBadgeDetailsState({ badgeId: badgeId.value }).then(() => $announcer.polite(`added ${selectedProject} level ${selectedLevel} to global badge`));
        // selectedProject = null;
        loadProjectsForBadge();
        emit('global-badge-levels-changed', newLevel);
      });
};

const deleteLevel = (deletedLevel) => {
  const msg = `Removing this level will award this badge to users that fulfill all of the remaining requirements.
        Are you sure you want to remove Level "${deletedLevel.level}" for project "${deletedLevel.projectName}" from Badge "${badge.value.name}"?`;
  // msgConfirm(msg, 'WARNING: Remove Required Level').then((res) => {
  //   if (res) {
  //     levelDeleted(deletedLevel);
  //   }
  // });
};

const levelDeleted = (deletedItem) => {
  GlobalBadgeService.removeProjectLevelFromBadge(badgeId.value, deletedItem.projectId, deletedItem.level)
      .then(() => {
        badgeLevels.value = badgeLevels.value.filter((item) => `${item.projectId}${item.level}` !== `${deletedItem.projectId}${deletedItem.level}`);
        // loadGlobalBadgeDetailsState({ badgeId: badgeId.value }).then(() => $announcer.polite('project level removed from global badge'));
        loadProjectsForBadge();
        emit('global-badge-levels-changed', deletedItem);
      });
};

const projectAdded = (addedProject) => {
  selectedProject.value = addedProject;
  levelPlaceholder.value = 'Pick a Level';
  selectedLevel.value = null;
};

const projectRemoved = () => {
  selectedProject.value = null;
  selectedLevel.value = null;
  levelPlaceholder.value = 'First choose a Project';
};

const changeLevel = (level) => {
  projectLevelId.value = level.projectId;
  projectLevelName.value = level.projectName;
  projectLevel.value = level.level;
  showChangeLevel.value = true;
};

const changeLevelClosed = (e) => {
  const { projectId } = e;
  setTimeout(() => {
    handleFocus({ projectId });
  }, 0);
  showChangeLevel.value = false;
  projectLevelId.value = null;
  projectLevel.value = null;
  projectLevelName.value = null;
};

const saveLevelChange = (e) => {
  GlobalBadgeService.changeProjectLevel(badgeId.value, e.projectId, e.oldLevel, e.newLevel)
      .then(() => loadBadgeLevels());
};

const handleFocus = (e) => {
  if (e && e.projectId) {
    const refName = `edit_${e.projectId}`;
    // const ref = $refs.globalLevelsTable.$refs[refName];
    // nextTick(() => {
    //   if (ref) {
    //     ref.focus();
    //   }
    // });
  }
};

// const loadProjectsForBadge = debounce(function loadProjects() {
const loadProjectsForBadge = () => {
  loadingAvailableProjects.value = true;
  GlobalBadgeService.suggestProjectsForPage(badgeId.value, projectSearch.value)
      .then((response) => {
        if (response && response.projects) {
          availableProjects.value = response.projects;
        }
        if (response?.totalAvailable > response?.projects?.length) {
          afterListSlotText.value = `Showing ${response.projects.length} of ${response.totalAvailable} results.  Use search to narrow results.`;
        } else {
          afterListSlotText.value = '';
        }
      }).finally(() => {
    loadingAvailableProjects.value = false;
  });
};
// }, 250);

const searchChanged = (query) => {
  projectSearch.value = query;
  loadProjectsForBadge();
};
</script>

<template>
  <div>
    <sub-page-header title="Levels"/>

    <Card>
      <template #content>
        <loading-container v-model="isLoading">
          <div class="mb-4 m-3">
            <div class="row p-0">
              <div class="col-md">
                <project-selector ref="projectSelectorRef" v-model="selectedProject"
                                  :projects="availableProjects"
                                  :after-list-slot-text="afterListSlotText"
                                  @added="projectAdded"
                                  :is-loading="loadingAvailableProjects"
                                  :internal-search="false"
                                  @search-change="searchChanged"
                                  @removed="projectRemoved"></project-selector>
              </div>
              <div class="col-md my-3 m-md-0">
                <level-selector v-model="selectedLevel" :project-id="selectedProjectId" :disabled="!selectedProject" :placeholder="levelPlaceholder"></level-selector>
              </div>
              <div class="col-md-auto">
              <span>
                <SkillsButton :disabled="!(selectedProject && selectedLevel)" type="button" class="btn btn-outline-primary"
                        @click="addLevel" data-cy="addGlobalBadgeLevel" aria-label="add project level requirement to global badge" label="Add" icon="fas fa-plus-circle">
                </SkillsButton>
              </span>
              </div>
            </div>
          </div>
<!--          <simple-levels-table ref="globalLevelsTable" v-if="badgeLevels && badgeLevels.length > 0"-->
<!--                               @change-level="changeLevel"-->
<!--                               :levels="badgeLevels" @level-removed="deleteLevel"></simple-levels-table>-->
<!--          <no-content2 v-else title="No Levels Added Yet..." icon="fas fa-trophy" class="mb-5"-->
<!--                       message="Please select a project and level from drop-down menus above to start adding levels to this badge!"></no-content2>-->

        </loading-container>
      </template>
    </Card>
  </div>
</template>

<style scoped>

</style>
/*
Copyright 2024 SkillTree

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
<script setup>
import { ref, computed, onMounted } from 'vue';
import { useRoute } from 'vue-router';
import GlobalBadgeService from "@/components/badges/global/GlobalBadgeService.js";
import SubPageHeader from "@/components/utils/pages/SubPageHeader.vue";
import LoadingContainer from "@/components/utils/LoadingContainer.vue";
import ProjectSelector from "@/components/levels/global/ProjectSelector.vue";
import LevelSelector from "@/components/levels/global/LevelSelector.vue";
import SimpleLevelsTable from "@/components/levels/global/SimpleLevelsTable.vue";
import NoContent2 from "@/components/utils/NoContent2.vue";
import {useBadgeState} from "@/stores/UseBadgeState.js";
import {storeToRefs} from "pinia";
import { useSkillsAnnouncer } from '@/common-components/utilities/UseSkillsAnnouncer.js'
import ChangeProjectLevel from "@/components/levels/global/ChangeProjectLevel.vue";
import {useDialogMessages} from "@/components/utils/modal/UseDialogMessages.js";

const dialogMessages = useDialogMessages()
const announcer = useSkillsAnnouncer();
const route = useRoute();
const emit = defineEmits(['global-badge-levels-changed']);
const badgeState = useBadgeState();

const { badge } = storeToRefs(badgeState);
const selectedProject = ref( null);
const selectedLevel = ref( null);
const isLoading = ref( true);
const loadingAvailableProjects = ref( false);
const levelPlaceholder = ref( 'First choose a Project');
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
        selectedProject.value = null;
        badgeState.loadGlobalBadgeDetailsState(badgeId.value).finally(() => {
          announcer.polite(`added ${selectedProject.value} level ${selectedLevel.value} to global badge`)
          badge.value = badgeState.badge;
        });
        loadProjectsForBadge();
        emit('global-badge-levels-changed', newLevel);
      });
};

const deleteLevel = (deletedLevel) => {
  const msg = `Removing this level will award this badge to users that fulfill all of the remaining requirements.
        Are you sure you want to remove Level "${deletedLevel.level}" for project "${deletedLevel.projectName}" from Badge "${badge.value.name}"?`;
  dialogMessages.msgConfirm({
    message: msg,
    header: 'WARNING: Remove Required Level!',
    acceptLabel: 'YES, Delete It!',
    rejectLabel: 'Cancel',
    accept: () => {
      levelDeleted(deletedLevel);
    }
  })
};

const levelDeleted = (deletedItem) => {
  GlobalBadgeService.removeProjectLevelFromBadge(badgeId.value, deletedItem.projectId, deletedItem.level)
      .then(() => {
        badgeLevels.value = badgeLevels.value.filter((item) => `${item.projectId}${item.level}` !== `${deletedItem.projectId}${deletedItem.level}`);
        badgeState.loadGlobalBadgeDetailsState(badgeId.value).finally(() => {
          announcer.polite('project level removed from global badge')
          badge.value = badgeState.badge;
        });
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

const changeLevelClosed = () => {
  showChangeLevel.value = false;
  projectLevelId.value = null;
  projectLevel.value = null;
  projectLevelName.value = null;
};

const saveLevelChange = (e) => {
  GlobalBadgeService.changeProjectLevel(badgeId.value, e.projectId, e.oldLevel, e.newLevel)
      .then(() => loadBadgeLevels());
};

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

const searchChanged = (query) => {
  projectSearch.value = query;
  loadProjectsForBadge();
};

const selectLevel = (level) => {
  selectedLevel.value = level;
}
</script>

<template>
  <div>
    <sub-page-header title="Levels"/>

    <Card :pt="{ body: { class: 'p-0!' } }">
      <template #content>
        <loading-container :is-loading="isLoading">
          <div class="mb-6 m-4 px-4 py-6">
            <div class="flex gap-2">
              <div class="flex-1">
                <project-selector ref="projectSelectorRef" v-model="selectedProject"
                                  :projects="availableProjects"
                                  :after-list-slot-text="afterListSlotText"
                                  @added="projectAdded"
                                  :is-loading="loadingAvailableProjects"
                                  :internal-search="true"
                                  @search-change="searchChanged"
                                  @removed="projectRemoved"></project-selector>
              </div>
              <div class="flex-1">
                <level-selector v-model="selectedLevel" @input="selectLevel" :project-id="selectedProjectId" :disabled="!selectedProject" :placeholder="levelPlaceholder"></level-selector>
              </div>
              <div>
                <SkillsButton :disabled="!(selectedProject && selectedLevel)" type="button" class="btn btn-outline-primary"
                        @click="addLevel" data-cy="addGlobalBadgeLevel" aria-label="add project level requirement to global badge" label="Add" icon="fas fa-plus-circle">
                </SkillsButton>
              </div>
            </div>
          </div>
          <simple-levels-table ref="globalLevelsTable" v-if="badgeLevels && badgeLevels.length > 0"
                               @change-level="changeLevel" :levels="badgeLevels" @level-removed="deleteLevel"></simple-levels-table>
          <no-content2 v-else title="No Levels Added Yet..." icon="fas fa-trophy" class="pb-8"
                       message="Please select a project and level from drop-down menus above to start adding levels to this badge!"></no-content2>

        </loading-container>
      </template>
    </Card>

    <change-project-level @level-changed="saveLevelChange"
                          @hidden="changeLevelClosed"
                          v-if="showChangeLevel"
                          :title="`Change Required Level for ${projectLevelName}`"
                          :current-level="projectLevel"
                          :project-id="projectLevelId"/>
  </div>
</template>

<style scoped>

</style>
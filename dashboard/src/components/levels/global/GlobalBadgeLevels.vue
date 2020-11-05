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
    <sub-page-header title="Levels"/>

    <simple-card>
      <loading-container v-model="isLoading">
        <div class="row mb-4">
          <div class="col-12 col-sm-6">
            <project-selector ref="projectSelectorRef" v-model="selectedProject" @added="projectAdded" @removed="projectRemoved"></project-selector>
          </div>
          <div class="col-12 col-sm-4">
            <level-selector v-model="selectedLevel" :project-id="selectedProjectId" :disabled="!selectedProject" :placeholder="levelPlaceholder"></level-selector>
          </div>
          <div class="col-12 col-sm mt-2 mt-sm-0">
            <span v-b-tooltip.hover="'Add Project and Level to Global Badge.'">
              <button :disabled="!(selectedProject && selectedLevel)" type="button" class="btn btn-outline-primary" @click="addLevel">
                <span class="d-none d-sm-inline"></span>Add <i class="fas fa-plus-circle" aria-hidden="true"/>
              </button>
            </span>
          </div>
        </div>

        <simple-levels-table v-if="badgeLevels && badgeLevels.length > 0"
                             :levels="badgeLevels" @level-removed="deleteLevel"></simple-levels-table>
        <no-content2 v-else title="No Levels Added Yet..." icon="fas fa-trophy"
                     message="Please select a project and level from drop-down menus above to start adding levels to this badge!"></no-content2>

      </loading-container>
    </simple-card>

  </div>
</template>

<script>
  import { createNamespacedHelpers } from 'vuex';

  import GlobalBadgeService from '../../badges/global/GlobalBadgeService';
  import SimpleLevelsTable from './SimpleLevelsTable';
  import ProjectSelector from './ProjectSelector';
  import LevelSelector from './LevelSelector';
  import NoContent2 from '../../utils/NoContent2';
  import SubPageHeader from '../../utils/pages/SubPageHeader';
  import LoadingContainer from '../../utils/LoadingContainer';
  import SimpleCard from '../../utils/cards/SimpleCard';
  import MsgBoxMixin from '../../utils/modal/MsgBoxMixin';

  const { mapActions } = createNamespacedHelpers('badges');

  export default {
    name: 'Levels',
    components: {
      ProjectSelector,
      LevelSelector,
      SimpleLevelsTable,
      SimpleCard,
      LoadingContainer,
      SubPageHeader,
      NoContent2,
    },
    mixins: [MsgBoxMixin],
    data() {
      return {
        selectedProject: null,
        selectedLevel: null,
        isLoading: true,
        levelPlaceholder: 'First choose a Project',
        badge: null,
        badgeId: null,
        badgeLevels: [],
      };
    },
    computed: {
      selectedProjectId() {
        let selectedProjectId = null;
        if (this.selectedProject) {
          selectedProjectId = this.selectedProject.projectId;
        }
        return selectedProjectId;
      },
    },
    mounted() {
      this.badgeId = this.$route.params.badgeId;
      this.loadBadgeLevels();
    },
    methods: {
      ...mapActions([
        'loadGlobalBadgeDetailsState',
      ]),
      loadBadgeLevels() {
        if (this.$route.params.badge) {
          this.badge = this.$route.params.badge;
          this.badgeLevels = this.badge.requiredProjectLevels;
          this.isLoading = false;
        } else {
          GlobalBadgeService.getBadge(this.badgeId)
            .then((response) => {
              this.badge = response;
              this.badgeLevels = response.requiredProjectLevels;
              this.isLoading = false;
            });
        }
      },
      addLevel() {
        GlobalBadgeService.assignProjectLevelToBadge(this.badgeId, this.selectedProject.projectId, this.selectedLevel)
          .then(() => {
            const newLevel = {
              badgeId: this.badgeId,
              projectId: this.selectedProject.projectId,
              projectName: this.selectedProject.name,
              level: this.selectedLevel,
            };
            this.badgeLevels.push(newLevel);
            this.selectedLevel = null;
            this.loadGlobalBadgeDetailsState({ badgeId: this.badgeId });
            this.selectedProject = null;
            this.$refs.projectSelectorRef.loadProjectsForBadge();
            this.$emit('global-badge-levels-changed', newLevel);
          });
      },
      deleteLevel(deletedLevel) {
        const msg = `Are you sure you want to remove Level "${deletedLevel.level}" for project "${deletedLevel.projectName}" from Badge "${this.badge.name}"?`;
        this.msgConfirm(msg, 'WARNING: Remove Required Level').then((res) => {
          if (res) {
            this.levelDeleted(deletedLevel);
          }
        });
      },
      levelDeleted(deletedItem) {
        GlobalBadgeService.removeProjectLevelFromBadge(this.badgeId, deletedItem.projectId, deletedItem.level)
          .then(() => {
            this.badgeLevels = this.badgeLevels.filter((item) => `${item.projectId}${item.level}` !== `${deletedItem.projectId}${deletedItem.level}`);
            this.loadGlobalBadgeDetailsState({ badgeId: this.badgeId });
            this.$refs.projectSelectorRef.loadProjectsForBadge();
            this.$emit('global-badge-levels-changed', deletedItem);
          });
      },
      projectAdded() {
        // this.selectedProject = addedProject;
        this.levelPlaceholder = 'Pick a Level';
        this.selectedLevel = null;
      },
      projectRemoved() {
        this.selectedProject = null;
        this.selectedLevel = null;
        this.levelPlaceholder = 'First choose a Project';
      },
    },
  };
</script>

<style>
  #level-def-panel .level-icon {
    font-size: 1.5rem;
    height: 24px;
    width: 24px;
  }

  #level-def-panel .VuePagination__count {
    display: none;
  }

  .icon-warning {
    font-size: 1.5rem;
  }

</style>

<style scoped>

</style>

<template>
  <div>
    <sub-page-header title="Levels"/>

    <simple-card>
      <loading-container v-model="isLoading">
        <div class="row mb-4">
          <div class="col-12 col-sm">
            <project-selector v-model="selectedProject" @added="projectAdded" @removed="projectRemoved"></project-selector>
          </div>
          <div class="col-12 col-sm">
            <level-selector v-model="selectedLevel" :project-id="selectedProjectId" :disabled="!selectedProject" :placeholder="levelPlaceholder"></level-selector>
          </div>
          <div class="col-12 col-sm-1 mt-2 mt-sm-0">
            <span v-b-tooltip.hover="'Add Project and Level to Global Badge.'">
              <button :disabled="!(selectedProject && selectedLevel)" type="button" class="btn btn-outline-primary" @click="addLevel">
                <span class="d-none d-sm-inline"></span> <i class="fas fa-plus-circle"/>
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
            const newLevel = { name: this.selectedProject.name, level: this.selectedLevel };
            this.badgeLevels.push(newLevel);
            this.selectedProject = null;
            this.selectedLevel = null;
            this.loadGlobalBadgeDetailsState({ badgeId: this.badgeId });
            this.$emit('levels-changed', newLevel);
          });
      },
      deleteLevel(deletedLevel) {
        console.log(`Deleting Level...[${deletedLevel}] - badgeId [${this.badgeId}}]`, deletedLevel);
      },
      projectAdded() {
        // this.selectedProject = addedProject;
        this.levelPlaceholder = 'Pick a Level';
        this.selectedProject = null;
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

<template>
  <div id="shared-skills-with-others-panel" class="card">
    <div class="card-header">
      Share Skills <strong>With</strong> Other Projects
    </div>
    <div class="card-body">
      <loading-container :is-loading="loading.sharedSkillsInit || loading.allSkills">
        <div class="row text-center">
          <div class="col-sm-5">
            <skills-selector2 :options="allSkills" v-on:added="onSelectedSkill" v-on:removed="onDeselectedSkill"
                              :selected="selectedSkills"></skills-selector2>
          </div>
          <div class="col-sm-5 my-2 my-sm-0 px-sm-1">
            <project-selector :project-id="projectId" :selected="selectedProject"
                              v-on:selected="onSelectedProject"
                              v-on:unselected="onUnSelectedProject"></project-selector>
          </div>
          <div class="col-sm-2 text-center text-sm-left">
            <button class="btn btn-sm btn-outline-primary h-100" v-on:click="shareSkill"
                    :disabled="!shareButtonEnabled">
              <i class="fas fa-share-alt mr-1"></i><span class="text-truncate">Share</span>
            </button>
          </div>
        </div>

        <b-alert v-if="displayError" variant="danger" class="mt-2" show dismissible>
          <i class="fa fa-exclamation-circle"></i> Skill <strong>[{{ selectedSkills[0].name }}]</strong> is already
          shared to project <strong>[{{ selectedProject.name }}]</strong>.
        </b-alert>

        <loading-container :is-loading="loading.sharedSkills">
          <div v-if="sharedSkills && sharedSkills.length > 0" class="my-4">
            <shared-skills-table :shared-skills="sharedSkills"
                                 v-on:skill-removed="deleteSharedSkill"></shared-skills-table>
          </div>
          <div v-else>
            <no-content2 title="Share Skills With Other Projects" icon="fas fa-share-alt" class="my-5"
                         message="To start sharing skills please select a skill and then the project that you want to share this skill with."/>
          </div>
        </loading-container>

      </loading-container>
    </div>
  </div>
</template>

<script>
  import SkillsSelector2 from '../SkillsSelector2';
  import LoadingContainer from '../../utils/LoadingContainer';
  import SkillsService from '../SkillsService';
  import ProjectSelector from './ProjectSelector';
  import SharedSkillsTable from './SharedSkillsTable';
  import SkillsShareService from './SkillsShareService';
  import NoContent2 from '../../utils/NoContent2';

  export default {
    name: 'ShareSkillsWithOtherProjects',
    props: ['projectId'],
    components: {
      NoContent2,
      SharedSkillsTable,
      ProjectSelector,
      LoadingContainer,
      SkillsSelector2,
    },
    data() {
      return {
        loading: {
          allSkills: true,
          sharedSkillsInit: true,
          sharedSkills: false,
        },
        isLoading: true,
        allSkills: [],
        selectedSkills: [],
        sharedSkills: [],
        selectedProject: null,
        displayError: false,
      };
    },
    mounted() {
      this.loadAllSkills();
      this.loadSharedSkills();
    },
    computed: {
      shareButtonEnabled() {
        return this.selectedProject && this.selectedSkills && this.selectedSkills.length > 0 && !this.loading.sharedSkills;
      },
    },
    methods: {
      loadAllSkills() {
        this.loading.allSkills = true;
        SkillsService.getProjectSkills(this.projectId)
          .then((skills) => {
            this.allSkills = skills;
            this.loading.allSkills = false;
          });
      },
      loadSharedSkills() {
        this.loading.sharedSkills = true;
        SkillsShareService.getSharedSkills(this.projectId)
          .then((data) => {
            this.sharedSkills = data;
            this.loading.sharedSkillsInit = false;
            this.loading.sharedSkills = false;
          });
      },

      shareSkill() {
        if (this.doesShareAlreadyExist()) {
          this.displayError = true;
        } else {
          this.displayError = false;
          this.loading.sharedSkills = true;
          const selectedSkill = this.selectedSkills[0];
          SkillsShareService.shareSkillToAnotherProject(this.projectId, selectedSkill.skillId, this.selectedProject.projectId)
            .then(() => {
              this.loadSharedSkills();
            });
        }
      },
      doesShareAlreadyExist() {
        const selectedSkill = this.selectedSkills[0];
        const alreadyExist = this.sharedSkills.find(entry => entry.skillId === selectedSkill.skillId && entry.projectId === this.selectedProject.projectId);
        return alreadyExist;
      },
      deleteSharedSkill(itemToRemove) {
        this.loading.sharedSkills = true;
        SkillsShareService.deleteSkillShare(this.projectId, itemToRemove.skillId, itemToRemove.projectId)
          .then(() => {
            this.loadSharedSkills();
          });
      },
      onSelectedProject(item) {
        this.selectedProject = item;
      },
      onUnSelectedProject() {
        this.selectedProject = null;
      },
      onSelectedSkill(item) {
        this.selectedSkills = [item];
      },
      onDeselectedSkill() {
        this.selectedSkills = [];
      },
    },

  };
</script>

<style scoped>
  #shared-skills-with-others-panel .button {
    min-height: 40px;
  }

  #shared-skills-with-others-panel .title {
    color: #3273dc;
    font-weight: normal;
  }

  #shared-skills-with-others-panel .title strong {
    font-weight: bold;
  }
</style>

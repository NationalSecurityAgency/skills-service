<template>
  <div v-if="finalizeInfo.numSkillsToFinalize > 0" data-cy="importFinalizeAlert" class="mb-0 mt-1">
    <b-alert :show="finalizeSuccessfullyCompleted" variant="success" dismissible>
      <i class="fas fa-thumbs-up"></i> Successfully finalized <b-badge variant="info">{{finalizeInfo.numSkillsToFinalize}}</b-badge> imported skills! Please enjoy your day!
    </b-alert>
    <b-alert :show="finalizeCompletedAndFailed" variant="danger" dismissible>
      <i class="fas fa-thumbs-down"></i> Well this is sad. Looks like finalization failed, please reach out to the SkillTree team for further assistance.
    </b-alert>
    <b-alert :show="finalizeIsRunning" variant="warning">
      <i class="fas fa-running"></i> Catalog finalization is in progress. Finalizing <b-badge variant="info">{{finalizeInfo.numSkillsToFinalize}}</b-badge> imported skills! The process may take a few minutes.
    </b-alert>
    <b-alert :show="!finalizeSuccessfullyCompleted && !finalizeCompletedAndFailed && !finalizeIsRunning" variant="warning">
      <i class="fas fa-exclamation-circle"></i> There are <b-badge variant="info">{{finalizeInfo.numSkillsToFinalize}}</b-badge> imported skills in this project that are not yet finalized. Once you have finished importing the skills you are interested in,
      <b-button variant="success" @click="showFinalizeModal = true" data-cy="finalizeBtn"><i class="fas fa-check-double"></i> Finalize</b-button> the import to enable those skills.
      Click <a :href="dashboardSkillsCatalogGuide" target="_blank">here <i class="fas fa-external-link-alt"></i></a> to learn more.
    </b-alert>

    <finalize-preview-modal  v-if="showFinalizeModal" v-model="showFinalizeModal" @finalize-scheduled="finalizeScheduled"/>
  </div>
</template>

<script>
  import FinalizePreviewModal from '@/components/skills/catalog/FinalizePreviewModal';
  import SettingsService from '@/components/settings/SettingsService';
  import CatalogService from '@/components/skills/catalog/CatalogService';

  export default {
    name: 'ImportFinalizeAlert',
    components: { FinalizePreviewModal },
    computed: {
      dashboardSkillsCatalogGuide() {
        return `${this.$store.getters.config.docsHost}/dashboard/user-guide/skills-groups.html`;
      },
    },
    data() {
      return {
        showFinalizeModal: false,
        finalizeIsRunning: false,
        finalizeSuccessfullyCompleted: false,
        finalizeCompletedAndFailed: false,
        finalizeInfo: {},
      };
    },
    mounted() {
      CatalogService.getCatalogFinalizeInfo(this.$route.params.projectId)
        .then((finalizeInfoRes) => {
          this.finalizeInfo = finalizeInfoRes;
          this.getFinalizationState().then((res) => {
            if (res && res.value === 'RUNNING') {
              this.finalizeIsRunning = true;
              this.checkFinalizationState();
            }
          });
        });
    },
    methods: {
      finalizeScheduled() {
        this.finalizeIsRunning = true;
        this.checkFinalizationState();
      },
      getFinalizationState() {
        return SettingsService.getProjectSetting(this.$route.params.projectId, 'catalog.finalize.state');
      },
      checkFinalizationState() {
        setTimeout(() => {
          this.getFinalizationState().then((res) => {
            if (res) {
              if (res.value === 'RUNNING') {
                this.checkFinalizationState();
              } else if (res.value === 'COMPLETED') {
                this.finalizeIsRunning = false;
                this.finalizeSuccessfullyCompleted = true;
              } else {
                this.finalizeIsRunning = false;
                this.finalizeCompletedAndFailed = true;
              }
            }
          });
        }, 1000);
      },
    },
  };
</script>

<style scoped>

</style>

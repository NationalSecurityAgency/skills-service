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
  <div v-if="finalizeInfo.numSkillsToFinalize > 0" data-cy="importFinalizeAlert" class="mb-0 mt-1">
    <b-alert :show="finalizeInfo.finalizeSuccessfullyCompleted && !finalizeInfo.finalizeIsRunning" variant="success" dismissible>
      <i class="fas fa-thumbs-up"></i> Successfully finalized
      <b-badge variant="info">{{ finalizeInfo.numSkillsToFinalize }}</b-badge>
      imported skill{{ sOrNone(finalizeInfo.numSkillsToFinalize) }}! Please enjoy your day!
    </b-alert>
    <b-alert :show="finalizeInfo.finalizeCompletedAndFailed && !finalizeInfo.finalizeIsRunning" variant="danger" dismissible>
      <i class="fas fa-thumbs-down"></i> Well this is sad. Looks like finalization failed, please
      reach out to the SkillTree team for further assistance.
    </b-alert>
    <b-alert :show="finalizeInfo.finalizeIsRunning && !finalizeInfo.finalizeSuccessfullyCompleted" variant="warning">
      <i class="fas fa-running"></i> Catalog finalization is in progress. Finalizing
      <b-badge variant="info">{{ finalizeInfo.numSkillsToFinalize }}</b-badge>
      imported skill{{ sOrNone(finalizeInfo.numSkillsToFinalize) }}! The process may take a few
      minutes.
      <import-finalize-progress />
    </b-alert>
    <b-alert :show="!finalizeInfo.finalizeSuccessfullyCompleted && !finalizeInfo.finalizeCompletedAndFailed && !finalizeInfo.finalizeIsRunning" variant="warning">
      <i class="fas fa-exclamation-circle"></i> There
      {{ areOrIs(finalizeInfo.numSkillsToFinalize) }}
      <b-badge variant="info">{{ finalizeInfo.numSkillsToFinalize }}</b-badge>
      imported skill{{ sOrNone(finalizeInfo.numSkillsToFinalize) }} in this project that {{ this.areOrIs(finalizeInfo.numSkillsToFinalize) }} not yet finalized. Once you have finished importing
      the skills you are interested in,
      <b-button variant="success" @click="showFinalizeModal = true" data-cy="finalizeBtn"><i class="fas fa-check-double"></i> Finalize</b-button> the import to enable those skills.
      Click <a :href="dashboardSkillsCatalogGuide" target="_blank">here <i class="fas fa-external-link-alt"></i></a> to learn more.
    </b-alert>

    <finalize-preview-modal  v-if="showFinalizeModal" v-model="showFinalizeModal" @finalize-scheduled="finalizeScheduled"/>
  </div>
</template>

<script>
  import { createNamespacedHelpers } from 'vuex';
  import FinalizePreviewModal from '@/components/skills/catalog/FinalizePreviewModal';
  import SettingsService from '@/components/settings/SettingsService';
  import ImportFinalizeProgress from '@/components/skills/catalog/ImportFinalizeProgress';

  const subjectSkills = createNamespacedHelpers('subjectSkills');
  const subjects = createNamespacedHelpers('subjects');
  const projects = createNamespacedHelpers('projects');
  const finalizeInfo = createNamespacedHelpers('finalizeInfo');

  export default {
    name: 'ImportFinalizeAlert',
    components: { ImportFinalizeProgress, FinalizePreviewModal },
    computed: {
      dashboardSkillsCatalogGuide() {
        return `${this.$store.getters.config.docsHost}/dashboard/user-guide/skills-groups.html`;
      },
      ...finalizeInfo.mapGetters([
        'finalizeInfo',
      ]),
    },
    data() {
      return {
        showFinalizeModal: false,
      };
    },
    mounted() {
      this.loadFinalizeInfo({ projectId: this.$route.params.projectId })
        .then((finalizeInfoRes) => {
          if (finalizeInfoRes.finalizeIsRunning) {
            this.checkFinalizationState();
          }
        });
    },
    methods: {
      ...subjectSkills.mapActions([
        'loadSubjectSkills',
      ]),
      ...subjects.mapActions([
        'loadSubjectDetailsState',
      ]),
      ...subjects.mapActions([
        'loadSubjects',
      ]),
      ...projects.mapActions([
        'loadProjectDetailsState',
      ]),
      ...finalizeInfo.mapActions([
        'loadFinalizeInfo',
      ]),
      finalizeScheduled() {
        this.finalizeInfo.finalizeIsRunning = true;
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
                this.finalizeInfo.finalizeIsRunning = false;
                this.finalizeInfo.finalizeSuccessfullyCompleted = true;
                if (this.$route.params.subjectId) {
                  this.loadSubjectSkills({
                    projectId: this.$route.params.projectId,
                    subjectId: this.$route.params.subjectId,
                  });
                  this.loadSubjectDetailsState({
                    projectId: this.$route.params.projectId,
                    subjectId: this.$route.params.subjectId,
                  });
                } else if (this.$route.params.projectId) {
                  this.loadProjectDetailsState({ projectId: this.$route.params.projectId });
                  this.loadSubjects({ projectId: this.$route.params.projectId });
                }
              } else {
                this.finalizeInfo.finalizeIsRunning = false;
                this.finalizeInfo.finalizeCompletedAndFailed = true;
              }
            }
          });
        }, 1000);
      },
      areOrIs(numItems) {
        return (numItems > 1) ? 'are' : 'is';
      },
      sOrNone(numItems) {
        return (numItems > 1) ? 's' : '';
      },
    },
  };
</script>

<style scoped>

</style>

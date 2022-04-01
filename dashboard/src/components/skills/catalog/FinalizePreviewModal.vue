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
  <b-modal id="finalizePreview" size="lg" title="Finalize Imported Skills" v-model="show"
           :no-close-on-backdrop="true" :centered="true"
           header-bg-variant="info" header-text-variant="light" no-fade role="dialog" @hide="publishHidden"
           aria-label="'Finalize Imported Skills'">
    <skills-spinner :is-loading="loading" class="mb-5"/>
    <div v-if="!loading">
      <p>
        There <span v-if="isPlural">are</span><span v-else>is</span> <b-badge>{{ finalizeInfo.numSkillsToFinalize }}</b-badge> skill<span v-if="isPlural">s</span> to finalize.
        Please note that the finalization process may take <i>several moments</i>.
      </p>
      <p>
        The finalization process includes:
        <ul>
          <li>Imported skills will <b>now</b> contribute to the overall project and subject points.</li>
          <li>Skill points are migrated to this project for <b>all of the users</b> who made progress in the imported skills <i>(in the original project)</i>.</li>
          <li>Project and subject <b>level</b> achievements are calculated for the users that have points for the imported skills.</li>
        </ul>
      </p>
      <p v-if="!canFinalize" data-cy="no-finalize">
        <i class="fas fa-exclamation-circle mr-1 text-warning" aria-hidden="true"/> {{ this.noFinalizeMsg }}
      </p>
    </div>

    <div slot="modal-footer" class="w-100">
        <b-button variant="success" size="sm" class="float-right" @click="finalize"
                  data-cy="doPerformFinalizeButton"
                  :disabled="startedFinalize || !canFinalize">
          <i class="fas fa-check-double"></i> Let's Finalize! <b-spinner v-if="startedFinalize" label="Loading..." small></b-spinner>
        </b-button>
        <b-button variant="secondary" size="sm" class="float-right mr-2" @click="close"
                  data-cy="finalizeCancelButton"
                  :disabled="startedFinalize">
          Cancel
        </b-button>
    </div>
  </b-modal>
</template>

<script>
  import CatalogService from '@/components/skills/catalog/CatalogService';
  import SkillsSpinner from '@/components/utils/SkillsSpinner';

  export default {
    name: 'FinalizePreviewModal',
    components: { SkillsSpinner },
    props: {
      value: {
        type: Boolean,
        required: true,
      },
    },
    data() {
      return {
        show: this.value,
        loading: true,
        canFinalize: false,
        finalizeInfo: {},
        startedFinalize: false,
        noFinalizeMsg: '',
      };
    },
    watch: {
      show(newValue) {
        this.$emit('input', newValue);
      },
    },
    mounted() {
      this.loadFinalizeInfo();
    },
    computed: {
      isPlural() {
        return this.finalizeInfo.numSkillsToFinalize > 1;
      },
      minimumPoints() {
        return this.$store.getters.config.minimumProjectPoints;
      },
      minimumSubjectPoints() {
        return this.$store.getters.config.minimumSubjectPoints;
      },
    },
    methods: {
      close(e) {
        this.show = false;
        this.publishHidden(e);
      },
      publishHidden(e) {
        this.$emit('hidden', { ...e });
      },
      loadFinalizeInfo() {
        this.loading = true;
        CatalogService.getCatalogFinalizeInfo(this.$route.params.projectId)
          .then((res) => {
            this.finalizeInfo = res;
            return CatalogService.getTotalPointsIncNotFinalized(this.$route.params.projectId);
          }).then((countData) => {
            this.canFinalize = true;
            console.log('got countData', countData);
            if (countData.insufficientProjectPoints || countData.subjectsWithInsufficientPoints.length > 0) {
              this.canFinalize = false;
              if (countData.insufficientProjectPoints) {
                this.noFinalizeMsg = `Finalization cannot be performed until ${countData.projectName} has at least ${this.minimumPoints} points. Finalizing currently imported Skills would only bring ${countData.projectName} to ${countData.projectTotalPoints} points.`;
              } else {
                const insufficientSubjects = countData.subjectsWithInsufficientPoints.map((c) => c.subjectName).join(', ');
                const insufficientSubjectsWithPts = countData.subjectsWithInsufficientPoints.map((c) => `${c.subjectName}: ${c.totalPoints} points`).join(', ');
                this.noFinalizeMsg = `Finalization cannot be performed until ${insufficientSubjects} ${countData.subjectsWithInsufficientPoints.length > 1 ? 'have' : 'has'}
                at least ${this.minimumSubjectPoints} points. Finalizing the currently imported skills would only result in ${insufficientSubjectsWithPts}.`;
              }
            }
          }).finally(() => {
            this.loading = false;
          });
      },
      finalize() {
        this.startedFinalize = true;
        CatalogService.finalizeImport(this.$route.params.projectId)
          .finally(() => {
            this.startedFinalize = false;
            this.$emit('finalize-scheduled');
            this.close();
          });
      },
    },
  };
</script>

<style scoped>

</style>

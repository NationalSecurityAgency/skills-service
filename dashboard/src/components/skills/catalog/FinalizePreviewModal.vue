<template>
  <b-modal id="finalizePreview" size="lg" title="Finalize Imported Skills" v-model="show"
           :no-close-on-backdrop="true" :centered="true"
           header-bg-variant="info" header-text-variant="light" no-fade role="dialog" @hide="publishHidden"
           aria-label="'Finalize Imported Skills'">
    <skills-spinner :is-loading="loading" class="mb-5"/>
    <div v-if="!loading">
      <p>
        There are <b-badge>{{ finalizeInfo.numSkillsToFinalize }}</b-badge> skills to finalize. Please note that finalization process may take a bit.
      </p>
      <p>
        Finalization process includes:
        <ul>
          <li>Imported skills will <b>now</b> contribute to the overall project and subject points.</li>
          <li>Skill points are migrated to this project for <b>all of the users</b> who made progress in the imported skills (of the original project).</li>
          <li>Project and subject level achievements are calculated for the users that have points for the imported skills.</li>
        </ul>
      </p>
    </div>

    <div slot="modal-footer" class="w-100">
        <b-button variant="success" size="sm" class="float-right" @click="finalize"
                  data-cy="doPerformFinalizeButton"
                  :disabled="startedFinalize">
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
        finalizeInfo: {},
        startedFinalize: false,
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

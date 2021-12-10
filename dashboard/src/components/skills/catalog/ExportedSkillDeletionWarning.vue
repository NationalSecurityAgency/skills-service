<template>
<div>
  <p>
    This will <span class="text-primary font-weight-bold">PERMANENTLY</span> remove skill <span class="text-primary font-weight-bold">[{{ skillId }}]</span> from the catalog.
    This skill is currently imported by <b-spinner v-if="loading" style="width: 1rem; height: 1rem;" variant="primary" label="Spinning" type="grow"></b-spinner><b-badge v-if="!loading" variant="info">{{ importedByNumProj }}</b-badge> projects.
  </p>

  <p>
    This action <b>CANNOT</b> be undone and will permanently remove the skill from those projects including their achievements. Please proceed with care.
  </p>
</div>
</template>

<script>
  import CatalogService from '@/components/skills/catalog/CatalogService';

  export default {
    name: 'ExportedSkillDeletionWarning',
    props: {
      skillId: String,
    },
    data() {
      return {
        loading: true,
        importedByNumProj: 0,
      };
    },
    mounted() {
      this.loading = true;
      CatalogService.getExportedStats(this.$route.params.projectId, this.skillId)
        .then((res) => {
          this.importedByNumProj = res.users ? res.users.length : 0;
        }).finally(() => {
          this.loading = false;
        });
    },
  };
</script>

<style scoped>

</style>

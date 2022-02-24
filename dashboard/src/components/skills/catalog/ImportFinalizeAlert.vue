<template>
    <div class="alert alert-warning mb-0 mt-1" data-cy="importFinalizeAlert">
      <i class="fas fa-exclamation-circle"></i> There are <b-badge variant="info">{{numDisabledSkills}}</b-badge> imported skills in this {{ type }} that are not yet finalized. Once you have finished importing the skills you are interested in,
      <b-button variant="success"><i class="fas fa-check-double"></i> Finalize</b-button> the import to enable those skills.
      Click <a :href="dashboardSkillsCatalogGuide" target="_blank">here <i class="fas fa-external-link-alt"></i></a> to learn more.
    </div>
</template>

<script>
  export default {
    name: 'ImportFinalizeAlert',
    props: {
      numDisabledSkills: Number,
      type: String,
    },
    computed: {
      dashboardSkillsCatalogGuide() {
        return `${this.$store.getters.config.docsHost}/dashboard/user-guide/skills-groups.html`;
      },
    },
  };
</script>

<style scoped>

</style>

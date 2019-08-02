<template>
  <div>
    <sub-page-header title="Skills"/>

    <simple-card>
      <loading-container v-bind:is-loading="loading.availableSkills || loading.badgeSkills || loading.skillOp">
        <skills-selector2 :options="allSkills" :selected="badgeSkills" class="mb-4"
                          v-on:added="skillAdded" v-on:removed="skillDeleted"></skills-selector2>

        <simple-skills-table v-if="badgeSkills && badgeSkills.length > 0"
                             :skills="badgeSkills" v-on:skill-removed="skillDeleted"></simple-skills-table>

        <no-content2 v-else title="No Skills Selected Yet..." icon="fas fa-award"
                     message="Please use drop-down above to start adding skills to this badge!"></no-content2>
      </loading-container>
    </simple-card>
  </div>
</template>

<script>
  import { createNamespacedHelpers } from 'vuex';

  import SkillsService from '../skills/SkillsService';
  import SkillsSelector2 from '../skills/SkillsSelector2';
  import LoadingContainer from '../utils/LoadingContainer';
  import SimpleSkillsTable from '../skills/SimpleSkillsTable';
  import NoContent2 from '../utils/NoContent2';
  import SubPageHeader from '../utils/pages/SubPageHeader';
  import SimpleCard from '../utils/cards/SimpleCard';

  const { mapActions } = createNamespacedHelpers('badges');

  export default {
    name: 'BadgeSkills',
    components: {
      SimpleCard,
      SubPageHeader,
      NoContent2,
      SimpleSkillsTable,
      LoadingContainer,
      SkillsSelector2,
    },
    data() {
      return {
        loading: {
          availableSkills: true,
          badgeSkills: true,
          skillOp: false,
        },
        badgeSkills: [],
        allSkills: [],
        projectId: null,
        badgeId: null,
      };
    },
    mounted() {
      this.projectId = this.$route.params.projectId;
      this.badgeId = this.$route.params.badgeId;
      this.loadAvailableBadgeSkills();
      this.loadAssignedBadgeSkills();
    },
    methods: {
      ...mapActions([
        'loadBadgeDetailsState',
      ]),
      loadAvailableBadgeSkills() {
        SkillsService.getProjectSkills(this.projectId)
          .then((loadedSkills) => {
            this.allSkills = loadedSkills;
            this.loading.availableSkills = false;
          });
      },
      loadAssignedBadgeSkills() {
        SkillsService.getBadgeSkills(this.projectId, this.badgeId)
          .then((loadedSkills) => {
            this.badgeSkills = loadedSkills;
            this.loading.badgeSkills = false;
          });
      },
      skillDeleted(deletedItem) {
        this.loading.skillOp = true;
        SkillsService.removeSkillFromBadge(this.projectId, this.badgeId, deletedItem.skillId)
          .then(() => {
            this.badgeSkills = this.badgeSkills.filter(entry => entry.id !== deletedItem.id);
            this.loading.skillOp = false;
            this.loadBadgeDetailsState({ projectId: this.projectId, badgeId: this.badgeId });
            this.$emit('skills-changed', deletedItem);
          });
      },
      skillAdded(newItem) {
        this.loading.skillOp = true;
        SkillsService.assignSkillToBadge(this.projectId, this.badgeId, newItem.skillId)
          .then(() => {
            this.badgeSkills.push(newItem);
            this.loading.skillOp = false;
            this.loadBadgeDetailsState({ projectId: this.projectId, badgeId: this.badgeId });
            this.$emit('skills-changed', newItem);
          });
      },
    },
  };
</script>

<style scoped>

</style>

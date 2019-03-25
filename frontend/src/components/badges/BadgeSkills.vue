<template>
  <div>
    <div class="columns">
      <div class="column is-full">
        <span class="title is-3">Skills</span>
      </div>
    </div>

    <div class="skills-bordered-component">
      <loading-container  v-bind:is-loading="loading.availableSkills || loading.badgeSkills || loading.skillOp">
        <div class="columns">
          <div class="column is-full">
            <skills-selector2 :options="allSkills" :selected="badgeSkills"
                              v-on:added="skillAdded" v-on:removed="skillDeleted"></skills-selector2>
          </div>
        </div>

        <simple-skills-table v-if="badgeSkills && badgeSkills.length > 0"
                             :skills="badgeSkills" v-on:skill-removed="skillDeleted"></simple-skills-table>
        <div v-else class="columns is-centered">
          <div class="column is-half">
            <no-content2 title="No Skills Selected Yet..." icon="fas fa-award"
                         message="Please use drop-down above to start adding skills to this badge!"></no-content2>
          </div>
        </div>
      </loading-container>
    </div>
  </div>
</template>

<script>
  import SkillsService from '../skills/SkillsService';
  import SkillsSelector2 from '../skills/SkillsSelector2';
  import LoadingContainer from '../utils/LoadingContainer';
  import SimpleSkillsTable from '../skills/SimpleSkillsTable';
  import NoContent2 from '../utils/NoContent2';

  export default {
    name: 'BadgeSkills',
    components: {
      NoContent2, SimpleSkillsTable, LoadingContainer, SkillsSelector2,
    },
    props: ['projectId', 'badgeId'],
    data() {
      return {
        loading: {
          availableSkills: true,
          badgeSkills: true,
          skillOp: false,
        },
        badgeSkills: [],
        allSkills: [],
      };
    },
    mounted() {
      this.loadAvailableBadgeSkills();
      this.loadAssignedBadgeSkills();
    },
    methods: {
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
            this.$emit('skills-changed', deletedItem);
          });
      },
      skillAdded(newItem) {
        this.loading.skillOp = true;
        SkillsService.assignSkillToBadge(this.projectId, this.badgeId, newItem.skillId)
          .then(() => {
            this.badgeSkills.push(newItem);
            this.loading.skillOp = false;
            this.$emit('skills-changed', newItem);
          });
      },
    },
  };
</script>

<style scoped>

</style>

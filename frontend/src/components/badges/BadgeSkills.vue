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

        <simple-skills-table :skills="badgeSkills" v-on:skill-removed="skillDeleted"></simple-skills-table>
      </loading-container>
    </div>
  </div>
</template>

<script>
  import SkillsService from '../skills/SkillsService';
  import SkillsSelector2 from '../skills/SkillsSelector2';
  import LoadingContainer from '../utils/LoadingContainer';
  import SimpleSkillsTable from '../skills/SimpleSkillsTable';

  export default {
    name: 'BadgeSkills',
    components: { SimpleSkillsTable, LoadingContainer, SkillsSelector2 },
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
        });
      },
      skillAdded(newItem) {
        this.loading.skillOp = true;
        SkillsService.assignSkillToBadge(this.projectId, this.badgeId, newItem.skillId)
          .then(() => {
            this.badgeSkills.push(newItem);
            this.loading.skillOp = false;
        });
      },
    },
  };
</script>

<style scoped>

</style>

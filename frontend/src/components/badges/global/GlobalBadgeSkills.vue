<template>
  <div>
    <sub-page-header title="Skills"/>

    <simple-card>
      <loading-container v-bind:is-loading="loading.availableSkills || loading.badgeSkills || loading.skillOp">
        <skills-selector2 :options="availableSkills" class="mb-4"
                          v-on:added="skillAdded"
                          :onlySingleSelectedValue="true"></skills-selector2>

        <simple-skills-table v-if="badgeSkills && badgeSkills.length > 0"
                             :skills="badgeSkills" v-on:skill-removed="deleteSkill"></simple-skills-table>

        <no-content2 v-else title="No Skills Selected Yet..." icon="fas fa-award"
                     message="Please use drop-down above to start adding skills to this badge!"></no-content2>
      </loading-container>
    </simple-card>
  </div>
</template>

<script>
  import GlobalBadgeService from './GlobalBadgeService';
  import SkillsSelector2 from '../../skills/SkillsSelector2';
  import LoadingContainer from '../../utils/LoadingContainer';
  import SimpleSkillsTable from '../../skills/SimpleSkillsTable';
  import NoContent2 from '../../utils/NoContent2';
  import SubPageHeader from '../../utils/pages/SubPageHeader';
  import SimpleCard from '../../utils/cards/SimpleCard';
  import MsgBoxMixin from '../../utils/modal/MsgBoxMixin';

  export default {
    name: 'GlobalBadgeSkills',
    components: {
      SimpleCard,
      SubPageHeader,
      NoContent2,
      SimpleSkillsTable,
      LoadingContainer,
      SkillsSelector2,
    },
    mixins: [MsgBoxMixin],
    data() {
      return {
        loading: {
          availableSkills: true,
          badgeSkills: true,
          skillOp: false,
        },
        badgeSkills: [],
        availableSkills: [],
        projectId: null,
        badgeId: null,
        badge: null,
      };
    },
    mounted() {
      this.projectId = this.$route.params.projectId;
      this.badgeId = this.$route.params.badgeId;
      this.badge = this.$route.params.badge;
      this.loadAssignedBadgeSkills();
    },
    methods: {
      loadAssignedBadgeSkills() {
        GlobalBadgeService.getBadgeSkills(this.badgeId)
          .then((loadedSkills) => {
            this.badgeSkills = loadedSkills;
            this.loading.badgeSkills = false;
            this.loadAvailableBadgeSkills();
          });
      },
      loadAvailableBadgeSkills() {
        GlobalBadgeService.suggestProjectSkills(this.badgeId, '')
          .then((loadedSkills) => {
            const badgeSkillIds = this.badgeSkills.map(item => item.id);
            this.availableSkills = loadedSkills.filter(item => !badgeSkillIds.includes(item.id));
            this.loading.availableSkills = false;
          });
      },
      deleteSkill(skill) {
        const msg = `Are you sure you want to remove Skill "${skill.name}" from Badge "${this.badge.name}"?`;
        this.msgConfirm(msg, 'WARNING: Remove Required Skill').then((res) => {
          if (res) {
            this.skillDeleted(skill);
          }
        });
      },
      skillDeleted(deletedItem) {
        this.loading.skillOp = true;
        GlobalBadgeService.removeSkillFromBadge(this.badgeId, deletedItem.projectId, deletedItem.skillId)
          .then(() => {
            this.badgeSkills = this.badgeSkills.filter(entry => entry.id !== deletedItem.id);
            this.availableSkills.unshift(deletedItem);
            this.loading.skillOp = false;
            this.$emit('skills-changed', deletedItem);
          });
      },
      skillAdded(newItem) {
        this.loading.skillOp = true;
        GlobalBadgeService.assignSkillToBadge(this.badgeId, newItem.projectId, newItem.skillId)
          .then(() => {
            this.badgeSkills.push(newItem);
            this.availableSkills = this.availableSkills.filter(item => item.id !== newItem.id);
            this.loading.skillOp = false;
            this.$emit('skills-changed', newItem);
          });
      },
    },
  };
</script>

<style scoped>

</style>

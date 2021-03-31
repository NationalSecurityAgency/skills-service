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
  <div>
    <sub-page-header title="Skills"/>

    <b-card body-class="p-0">
      <loading-container v-bind:is-loading="loading.availableSkills || loading.badgeSkills || loading.skillOp">
        <skills-selector2 :options="availableSkills" class="mb-4 m-3"
                          v-on:added="skillAdded" v-on:search-change="searchChanged"
                          :onlySingleSelectedValue="true" :internal-search="false" :show-project="true"
                          :after-list-slot-text="afterListSlotText"></skills-selector2>

        <simple-skills-table v-if="badgeSkills && badgeSkills.length > 0"
                             :skills="badgeSkills" v-on:skill-removed="deleteSkill"
                             :show-project="true"
        ></simple-skills-table>

        <no-content2 v-else title="No Skills Added Yet..." icon="fas fa-award" class="mb-5"
                     message="Please use drop-down above to start adding skills to this badge!"></no-content2>
      </loading-container>
    </b-card>
  </div>
</template>

<script>
  import { createNamespacedHelpers } from 'vuex';

  import GlobalBadgeService from './GlobalBadgeService';
  import SkillsSelector2 from '../../skills/SkillsSelector2';
  import LoadingContainer from '../../utils/LoadingContainer';
  import SimpleSkillsTable from '../../skills/SimpleSkillsTable';
  import NoContent2 from '../../utils/NoContent2';
  import SubPageHeader from '../../utils/pages/SubPageHeader';
  import MsgBoxMixin from '../../utils/modal/MsgBoxMixin';

  const { mapActions } = createNamespacedHelpers('badges');

  export default {
    name: 'GlobalBadgeSkills',
    components: {
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
        badgeId: null,
        badge: null,
        afterListSlotText: '',
      };
    },
    mounted() {
      this.badgeId = this.$route.params.badgeId;
      this.loadBadge();
      this.loadAssignedBadgeSkills();
    },
    methods: {
      ...mapActions([
        'loadGlobalBadgeDetailsState',
      ]),
      loadBadge() {
        if (this.$route.params.badge) {
          this.badge = this.$route.params.badge;
          this.badgeSkills = this.badge.requiredSkills;
          this.loading.badgeSkills = false;
        } else {
          GlobalBadgeService.getBadge(this.badgeId)
            .then((response) => {
              this.badge = response;
              this.badgeSkills = response.requiredSkills;
              this.loading.badgeSkills = false;
            });
        }
      },
      loadAssignedBadgeSkills() {
        this.loadAvailableBadgeSkills('');
      },
      loadAvailableBadgeSkills(query) {
        GlobalBadgeService.suggestProjectSkills(this.badgeId, query)
          .then((res) => {
            let badgeSkillIds = [];
            if (this.badgeSkills) {
              badgeSkillIds = this.badgeSkills.map((item) => `${item.projectId}${item.skillId}`);
            }
            this.availableSkills = [];
            if (res && res.suggestedSkills) {
              this.availableSkills = res.suggestedSkills.filter((item) => !badgeSkillIds.includes(`${item.projectId}${item.skillId}`));
            }
            if (res?.totalAvailable > res?.suggestedSkills?.length) {
              this.afterListSlotText = `Showing ${res.suggestedSkills.length} of ${res.totalAvailable} results.  Use search to narrow results.`;
            } else {
              this.afterListSlotText = '';
            }
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
            this.badgeSkills = this.badgeSkills.filter((item) => `${item.projectId}${item.skillId}` !== `${deletedItem.projectId}${deletedItem.skillId}`);
            this.availableSkills.unshift(deletedItem);
            this.loadGlobalBadgeDetailsState({ badgeId: this.badgeId });
            this.loading.skillOp = false;
            this.$emit('skills-changed', deletedItem);
          });
      },
      skillAdded(newItem) {
        this.loading.skillOp = true;
        GlobalBadgeService.assignSkillToBadge(this.badgeId, newItem.projectId, newItem.skillId)
          .then(() => {
            this.badgeSkills.push(newItem);
            this.availableSkills = this.availableSkills.filter((item) => `${item.projectId}${item.skillId}` !== `${newItem.projectId}${newItem.skillId}`);
            this.loadGlobalBadgeDetailsState({ badgeId: this.badgeId });
            this.loading.skillOp = false;
            this.$emit('skills-changed', newItem);
          });
      },
      searchChanged(query) {
        this.loadAvailableBadgeSkills(query);
      },
    },
  };
</script>

<style scoped>

</style>

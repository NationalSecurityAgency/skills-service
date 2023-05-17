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
      <loading-container v-bind:is-loading="loading.availableSkills || loading.badgeSkills || loading.skillOp || loading.badgeInfo">
        <skills-selector2 v-if="!isReadOnlyProj" :options="availableSkills" class="mb-2 m-3"
                          v-on:added="skillAdded"
                          :onlySingleSelectedValue="true"></skills-selector2>
        <div v-if="learningPathViolationErr.show" class="alert alert-danger mx-3" data-cy="learningPathErrMsg">
          <i class="fas fa-exclamation-triangle" aria-hidden="true" />
          Failed to add <b>{{ learningPathViolationErr.skillName }}</b> skill to the badge.
          Adding this skill would result in a <b>circular/infinite learning path</b>.
          Please visit project's <b-link :to="{ name: 'FullDependencyGraph' }" data-cy="learningPathLink">Learning Path</b-link> page to review.
        </div>

        <simple-skills-table v-if="badgeSkills && badgeSkills.length > 0" class="mt-2"
                             :skills="badgeSkills" v-on:skill-removed="deleteSkill"></simple-skills-table>

        <no-content2 v-else title="No Skills Selected Yet..." icon="fas fa-award" class="mb-5"
                     message="Please use drop-down above to start adding skills to this badge!"></no-content2>
      </loading-container>
    </b-card>
  </div>
</template>

<script>
  import { createNamespacedHelpers } from 'vuex';
  import { SkillsReporter } from '@skilltree/skills-client-vue';

  import SkillsService from '@/components/skills/SkillsService';
  import SkillsSelector2 from '@/components/skills/SkillsSelector2';
  import LoadingContainer from '@/components/utils/LoadingContainer';
  import SimpleSkillsTable from '@/components/skills/SimpleSkillsTable';
  import NoContent2 from '@/components/utils/NoContent2';
  import SubPageHeader from '@/components/utils/pages/SubPageHeader';
  import MsgBoxMixin from '@/components/utils/modal/MsgBoxMixin';
  import BadgesService from '@/components/badges/BadgesService';
  import ProjConfigMixin from '@/components/projects/ProjConfigMixin';
  import NavigationErrorMixin from '@/components/utils/NavigationErrorMixin';

  const { mapActions } = createNamespacedHelpers('badges');

  export default {
    name: 'BadgeSkills',
    components: {
      SubPageHeader,
      NoContent2,
      SimpleSkillsTable,
      LoadingContainer,
      SkillsSelector2,
    },
    mixins: [MsgBoxMixin, ProjConfigMixin, NavigationErrorMixin],
    data() {
      return {
        loading: {
          availableSkills: true,
          badgeSkills: true,
          skillOp: false,
          badgeInfo: false,
        },
        badgeSkills: [],
        availableSkills: [],
        projectId: null,
        badgeId: null,
        badge: null,
        self: null,
        learningPathViolationErr: {
          show: false,
          skillName: '',
        },
      };
    },
    mounted() {
      this.projectId = this.$route.params.projectId;
      this.badgeId = this.$route.params.badgeId;
      this.badge = this.$route.params.badge;
      if (!this.badge) {
        this.loadBadgeInfo();
      }
      this.loadAssignedBadgeSkills();
    },
    watch: {
      '$route.params.badgeId': function updateBadge() {
        this.badgeId = this.$route.params.badgeId;
        this.loadBadgeInfo();
        this.loadAssignedBadgeSkills();
      },
    },
    methods: {
      ...mapActions([
        'loadBadgeDetailsState',
      ]),
      loadAssignedBadgeSkills() {
        SkillsService.getBadgeSkills(this.projectId, this.badgeId)
          .then((loadedSkills) => {
            // in case of 403 request is still resolved but redirected to an error page
            // this avoids JS errors in console
            const validRequest = Array.isArray(loadedSkills);
            if (validRequest) {
              this.badgeSkills = loadedSkills;
            }
            this.loading.badgeSkills = false;
            if (validRequest) {
              this.loadAvailableBadgeSkills();
            }
          });
      },
      loadAvailableBadgeSkills() {
        SkillsService.getProjectSkills(this.projectId, null, false, true)
          .then((loadedSkills) => {
            const badgeSkillIds = this.badgeSkills.map((item) => item.skillId);
            this.availableSkills = loadedSkills.filter((item) => !badgeSkillIds.includes(item.skillId));
            this.loading.availableSkills = false;
          });
      },
      loadBadgeInfo() {
        BadgesService.getBadge(this.projectId, this.badgeId)
          .then((badge) => {
            this.badge = badge;
            this.loading.badgeInfo = false;
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
        SkillsService.removeSkillFromBadge(this.projectId, this.badgeId, deletedItem.skillId)
          .then(() => {
            this.badgeSkills = this.badgeSkills.filter((entry) => entry.skillId !== deletedItem.skillId);
            this.availableSkills.unshift(deletedItem);
            this.loadBadgeDetailsState({ projectId: this.projectId, badgeId: this.badgeId });
            this.loading.skillOp = false;
            this.$emit('skills-changed', deletedItem);
          });
      },
      skillAdded(newItem) {
        this.loading.skillOp = true;
        SkillsService.assignSkillToBadge(this.projectId, this.badgeId, newItem.skillId)
          .then(() => {
            this.badgeSkills.push(newItem);
            this.availableSkills = this.availableSkills.filter((item) => item.skillId !== newItem.skillId);
            this.loadBadgeDetailsState({ projectId: this.projectId, badgeId: this.badgeId });
            this.loading.skillOp = false;
            this.$emit('skills-changed', newItem);
            SkillsReporter.reportSkill('AssignGemOrBadgeSkills');
          }).catch((e) => {
            if (e.response.data && e.response.data.errorCode && e.response.data.errorCode === 'LearningPathViolation') {
              this.loading.skillOp = false;
              this.learningPathViolationErr.show = true;
              this.learningPathViolationErr.skillName = newItem.name;
            } else {
              const errorMessage = (e.response && e.response.data && e.response.data.explanation) ? e.response.data.explanation : undefined;
              this.handlePush({
                name: 'ErrorPage',
                query: { errorMessage },
              });
            }
        });
      },
    },
  };
</script>

<style scoped>

</style>

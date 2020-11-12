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
    <sub-page-header ref="subPageHeader" title="Badges" action="Badge" @add-action="newBadge"
                     :disabled="addBadgeDisabled"
                     :disabled-msg="addBadgesDisabledMsg"/>
    <loading-container v-bind:is-loading="isLoading">
      <transition name="projectContainer" enter-active-class="animated fadeIn">
        <div>
          <div v-if="badges && badges.length" class="row justify-content-center ">
            <div v-for="(badge) of badges"
                 :key="badge.badgeId" class="col-lg-4 mb-3"  style="min-width: 23rem;">
              <badge :badge="badge"
                     :ref="'badge_'+badge.badgeId"
                     @badge-updated="saveBadge"
                     @badge-deleted="deleteBadge"
                     @move-badge-up="moveBadgeUp"
                     @move-badge-down="moveBadgeDown"/>
            </div>
          </div>

          <no-content2 v-else title="No Badges Yet"
                       message="Badges add another facet to the overall gamification profile and allows you to further reward your users by providing these prestigious symbols. Badges are a collection of skills and when all of the skills are accomplished that badge is earned."
                       class="mt-4"/>
        </div>
      </transition>
    </loading-container>

    <edit-badge v-if="displayNewBadgeModal" v-model="displayNewBadgeModal"
                :badge="emptyNewBadge"
                @badge-updated="saveBadge"
                @hidden="handleHidden"></edit-badge>
  </div>
</template>

<script>
  import { createNamespacedHelpers } from 'vuex';
  import { SkillsReporter } from '@skilltree/skills-client-vue';

  import BadgesService from './BadgesService';
  import Badge from './Badge';
  import EditBadge from './EditBadge';
  import LoadingContainer from '../utils/LoadingContainer';
  import SubPageHeader from '../utils/pages/SubPageHeader';
  import NoContent2 from '../utils/NoContent2';

  const { mapActions } = createNamespacedHelpers('projects');

  export default {
    name: 'Badges',
    components: {
      NoContent2,
      SubPageHeader,
      LoadingContainer,
      Badge,
      EditBadge,
    },
    data() {
      return {
        isLoading: true,
        badges: [],
        displayNewBadgeModal: false,
        projectId: null,
      };
    },
    mounted() {
      this.projectId = this.$route.params.projectId;
      this.loadBadges();
    },
    computed: {
      emptyNewBadge() {
        return {
          projectId: this.projectId,
          name: '',
          badgeId: '',
          description: '',
          iconClass: 'fas fa-award',
          requiredSkills: [],
        };
      },
      addBadgeDisabled() {
        return this.badges && this.$store.getters.config && this.badges.length >= this.$store.getters.config.maxBadgesPerProject;
      },
      addBadgesDisabledMsg() {
        if (this.$store.getters.config) {
          return `The maximum number of Badges allowed is ${this.$store.getters.config.maxBadgesPerProject}`;
        }
        return '';
      },
    },
    methods: {
      ...mapActions([
        'loadProjectDetailsState',
      ]),
      loadBadges(afterLoad) {
        BadgesService.getBadges(this.projectId)
          .then((badgesResponse) => {
            this.isLoading = false;
            this.badges = badgesResponse;
            if (this.badges && this.badges.length) {
              this.badges[0].isFirst = true;
              this.badges[this.badges.length - 1].isLast = true;
            }
            if (afterLoad) {
              this.$nextTick(() => {
                afterLoad();
              });
            }
          })
          .finally(() => {
            this.isLoading = false;
          });
      },
      deleteBadge(badge) {
        this.isLoading = true;
        BadgesService.deleteBadge(badge.projectId, badge.badgeId)
          .then(() => {
            this.$emit('badge-deleted', this.badge);
            this.badges = this.badges.filter((item) => item.badgeId !== badge.badgeId);
            this.loadProjectDetailsState({ projectId: this.projectId });
            this.$emit('badges-changed', badge.badgeId);
          })
          .finally(() => {
            this.isLoading = false;
          });
      },
      saveBadge(badge) {
        this.isLoading = true;
        const requiredIds = badge.requiredSkills.map((item) => item.skillId);
        const badgeReq = { requiredSkillsIds: requiredIds, ...badge };
        const { isEdit } = badge;
        BadgesService.saveBadge(badgeReq)
          .then(() => {
            let afterLoad = null;
            if (isEdit) {
              afterLoad = () => {
                const refKey = `badge_${badgeReq.badgeId}`;
                const ref = this.$refs[refKey];
                if (ref) {
                  ref[0].handleFocus();
                }
              };
            }
            this.loadBadges(afterLoad);
            this.loadProjectDetailsState({ projectId: this.projectId });
            this.$emit('badges-changed', badge.badgeId);
          });
        if (badge.startDate) {
          SkillsReporter.reportSkill('CreateGem');
        } else {
          SkillsReporter.reportSkill('CreateBadge');
        }
      },
      newBadge() {
        this.displayNewBadgeModal = true;
      },
      moveBadgeDown(badge) {
        this.moveBadge(badge, 'DisplayOrderDown');
      },
      moveBadgeUp(badge) {
        this.moveBadge(badge, 'DisplayOrderUp');
      },
      moveBadge(badge, actionToSubmit) {
        this.isLoading = true;
        BadgesService.moveBadge(badge.projectId, badge.badgeId, actionToSubmit)
          .then(() => {
            this.loadBadges();
          });
      },
      handleHidden(e) {
        if (!e || !e.update) {
          this.handleFocus();
        }
      },
      handleFocus() {
        this.$nextTick(() => {
          this.$refs.subPageHeader.$refs.actionButton.focus();
        });
      },

    },
  };
</script>

<style scoped>

</style>

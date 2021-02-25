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
    <sub-page-header ref="subPageHeader" title="Global Badges" action="Badge" @add-action="newBadge" aria-label="new global badge"/>
    <loading-container v-bind:is-loading="isLoading">
      <transition name="projectContainer" enter-active-class="animated fadeIn">
        <div>
          <div v-if="badges && badges.length" class="row justify-content-center ">
            <div v-for="(badge) of badges"
                 :key="badge.badgeId" class="col-lg-4 mb-3"  style="min-width: 23rem;">
              <badge :badge="badge" :global="true"
                     @badge-updated="saveBadge"
                     @badge-deleted="deleteBadge"
                     @move-badge-up="moveBadgeUp"
                     @move-badge-down="moveBadgeDown"
                     :ref="`badge_${badge.badgeId}`"/>
            </div>
          </div>

          <no-content2 v-else title="No Badges Yet" class="mt-4"
                       message="Global Badges are a special kind of badge that is made up of a collection of skills and/or levels that span across project boundaries."/>
        </div>
      </transition>
    </loading-container>

    <edit-badge v-if="displayNewBadgeModal" v-model="displayNewBadgeModal" :badge="emptyNewBadge"
                :global="true" @badge-updated="saveBadge"
                @hidden="handleHidden"></edit-badge>
  </div>
</template>

<script>
  import GlobalBadgeService from './GlobalBadgeService';
  import Badge from '../Badge';
  import EditBadge from '../EditBadge';
  import LoadingContainer from '../../utils/LoadingContainer';
  import SubPageHeader from '../../utils/pages/SubPageHeader';
  import NoContent2 from '../../utils/NoContent2';

  export default {
    name: 'GlobalBadges',
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
      };
    },
    mounted() {
      this.loadBadges();
    },
    computed: {
      emptyNewBadge() {
        return {
          name: '',
          badgeId: '',
          description: '',
          iconClass: 'fas fa-award',
          requiredSkills: [],
        };
      },
    },
    methods: {
      loadBadges(afterLoad) {
        GlobalBadgeService.getBadges()
          .then((badgesResponse) => {
            const badges = badgesResponse;
            if (badges && badges.length) {
              badges[0].isFirst = true;
              badges[badges.length - 1].isLast = true;
              this.badges = badges;
            } else {
              this.badges = [];
            }
            if (afterLoad) {
              afterLoad();
            }
          })
          .finally(() => {
            this.isLoading = false;
          });
      },
      deleteBadge(badge) {
        this.isLoading = true;
        GlobalBadgeService.deleteBadge(badge.badgeId)
          .then(() => {
            this.$emit('badge-deleted', this.badge);
            this.badges = this.badges.filter((item) => item.badgeId !== badge.badgeId);
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
        GlobalBadgeService.saveBadge(badgeReq)
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
            this.$emit('global-badges-changed', badge.badgeId);
          });
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
        GlobalBadgeService.moveBadge(badge.badgeId, actionToSubmit)
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

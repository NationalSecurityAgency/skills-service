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
          <div v-if="badges && badges.length" id="badgeCards"
               class="row justify-content-center">
            <div v-for="(badge) of badges" :id="badge.badgeId"
                 :key="badge.badgeId" class="col-lg-4 mb-3"  style="min-width: 23rem;">
              <b-overlay :show="sortOrder.loading" rounded="sm" opacity="0.4">
                <template #overlay>
                  <div class="text-center" :data-cy="`${badge.badgeId}_overlayShown`">
                    <div v-if="badge.badgeId===sortOrder.loadingBadgeId" data-cy="updatingSortMsg">
                      <div class="text-info text-uppercase mb-1">Updating sort order!</div>
                      <b-spinner label="Loading..." style="width: 3rem; height: 3rem;" variant="info"/>
                    </div>
                  </div>
                </template>

                <badge :badge="badge" :global="true"
                       @badge-updated="saveBadge"
                       @badge-deleted="deleteBadge"
                       :ref="`badge_${badge.badgeId}`"
                       :disable-sort-control="badges.length === 1"/>
              </b-overlay>
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
  import Sortable from 'sortablejs';
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
        sortOrder: {
          loading: false,
          loadingBadgeId: '-1',
        },
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
        return GlobalBadgeService.getBadges()
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
              setTimeout(() => {
                afterLoad();
              }, 0);
            }
          })
          .finally(() => {
            this.isLoading = false;
            this.enableDropAndDrop();
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
            this.loadBadges(afterLoad).then(() => {
              this.$nextTick(() => this.$nextTick(() => this.$announcer.polite(`a global badge has been ${isEdit ? 'saved' : 'created'}`)));
            });
            this.$emit('global-badges-changed', badge.badgeId);
          });
      },
      newBadge() {
        this.displayNewBadgeModal = true;
      },
      handleHidden(event) {
        if (!event || !event.update) {
          this.handleFocus();
        }
      },
      handleFocus() {
        this.$nextTick(() => {
          this.$refs.subPageHeader.$refs.actionButton.focus();
        });
      },
      enableDropAndDrop() {
        if (this.badges && this.badges.length > 0) {
          const self = this;
          this.$nextTick(() => {
            const cards = document.getElementById('badgeCards');
            Sortable.create(cards, {
              handle: '.sort-control',
              animation: 150,
              ghostClass: 'skills-sort-order-ghost-class',
              onUpdate(event) {
                self.sortOrderUpdate(event);
              },
            });
          });
        }
      },
      sortOrderUpdate(updateEvent) {
        const { id } = updateEvent.item;
        this.sortOrder.loadingBadgeId = id;
        this.sortOrder.loading = true;
        GlobalBadgeService.updateBadgeDisplaySortOrder(id, updateEvent.newIndex)
          .finally(() => {
            this.sortOrder.loading = false;
          });
      },
    },
  };
</script>

<style scoped>

</style>

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
                     :disabled-msg="addBadgesDisabledMsg" aria-label="new badge"/>
    <loading-container v-bind:is-loading="isLoading">
      <transition name="projectContainer" enter-active-class="animated fadeIn">
        <div>
          <div v-if="badges && badges.length" id="badgeCards" class="row justify-content-center">
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

                <badge :badge="badge"
                       :ref="'badge_'+badge.badgeId"
                       @badge-updated="saveBadge"
                       @badge-deleted="deleteBadge"
                       @sort-changed-requested="updateSortAndReloadSubjects"
                       :disable-sort-control="badges.length === 1"/>
              </b-overlay>
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
  import Sortable from 'sortablejs';
  import { createNamespacedHelpers } from 'vuex';
  import { SkillsReporter } from '@skilltree/skills-client-vue';

  import BadgesService from '@/components/badges/BadgesService';
  import Badge from '@/components/badges/Badge';
  import EditBadge from '@/components/badges/EditBadge';
  import LoadingContainer from '@/components/utils/LoadingContainer';
  import SubPageHeader from '@/components/utils/pages/SubPageHeader';
  import NoContent2 from '@/components/utils/NoContent2';
  import ProjConfigMixin from '@/components/projects/ProjConfigMixin';

  const { mapActions } = createNamespacedHelpers('projects');

  export default {
    name: 'Badges',
    mixins: [ProjConfigMixin],
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
        sortOrder: {
          loading: false,
          loadingBadgeId: '-1',
        },
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
        return BadgesService.getBadges(this.projectId)
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
            this.enableDropAndDrop();
          });
      },
      updateSortAndReloadSubjects(updateInfo) {
        const sortedBadges = this.badges.sort((a, b) => {
          if (a.displayOrder > b.displayOrder) {
            return 1;
          }
          if (b.displayOrder > a.displayOrder) {
            return -1;
          }
          return 0;
        });
        const currentIndex = sortedBadges.findIndex((item) => item.badgeId === updateInfo.id);
        const newIndex = updateInfo.direction === 'up' ? currentIndex - 1 : currentIndex + 1;
        if (newIndex >= 0 && (newIndex) < this.badges.length) {
          this.isLoading = true;
          BadgesService.updateBadgeDisplaySortOrder(this.projectId, updateInfo.id, newIndex)
            .finally(() => {
              this.loadBadges()
                .then(() => {
                  this.isLoading = false;
                  const foundRef = this.$refs[`badge_${updateInfo.id}`];
                  this.$nextTick(() => {
                    foundRef[0].focusSortControl();
                  });
                });
            });
        }
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
          .then(() => {
            setTimeout(() => this.$announcer.polite(`Badge ${badge.name} has been deleted`), 0);
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
        BadgesService.saveBadge(badgeReq).then(() => {
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
            const msg = isEdit ? 'edited' : 'created';
            this.$nextTick(() => this.$announcer.polite(`Badge ${badge.name} has been ${msg}`));
          });
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
        BadgesService.updateBadgeDisplaySortOrder(this.projectId, id, updateEvent.newIndex)
          .finally(() => {
            this.sortOrder.loading = false;
          });
      },
    },
  };
</script>

<style scoped>

</style>

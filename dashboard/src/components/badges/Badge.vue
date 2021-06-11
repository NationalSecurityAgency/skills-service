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
  <subject-card :options="cardOptions" :isLoading="isLoading" :data-cy="`subjectCard-${badgeInternal.badgeId}`">
    <div slot="header-top-right">
    </div>
    <div slot="underTitle">
      <card-navigate-and-edit-controls ref="cardNavControls" class="mt-2"
                             :options="cardOptions.controls"
                             @edit="showEditBadge=true"
                             @delete="deleteBadge"
                             @move-up="moveUp"
                             @move-down="moveDown"/>
    </div>
    <div slot="footer">
      <i v-if="badgeInternal.endDate" class="fas fa-gem position-absolute" style="font-size: 1rem; top: 1rem; left: 1rem; color: purple" aria-hidden="true"/>
      <div class="mt-1 row align-items-center" style="height: 2rem;">
        <div class="col text-right small">
        <div v-if="!this.live" data-cy="badgeStatus" style="">
          <span class="text-secondary" style="height: 3rem;">Status: </span>
          <span class="text-uppercase border-right pr-2 mr-2">Disabled <span class="far fa-stop-circle text-warning" aria-hidden="true"/></span><a href="#0" @click.stop="handlePublish" class="btn btn-outline-primary btn-sm" data-cy="goLive">Go Live</a>
        </div>
        <div v-else data-cy="badgeStatus"  style="">
          <span class="text-secondary align-middle" style="height: 4rem;">Status: </span> <span class="text-uppercase align-middle" style="height: 4rem;">Live <span class="far fa-check-circle text-success" aria-hidden="true"/></span>
        </div>
        </div>
      </div>

      <edit-badge v-if="showEditBadge" v-model="showEditBadge" :id="badge.badgeId" :badge="badge" :is-edit="true"
                  :global="global" @badge-updated="badgeEdited" @hidden="handleHidden"></edit-badge>
    </div>

  </subject-card>
</template>

<script>
  import EditBadge from './EditBadge';
  import MsgBoxMixin from '../utils/modal/MsgBoxMixin';
  import SubjectCard from '../subjects/SubjectCard';
  import CardNavigateAndEditControls from '../utils/cards/CardNavigateAndEditControls';

  export default {
    name: 'Badge',
    components: { CardNavigateAndEditControls, SubjectCard, EditBadge },
    props: {
      badge: Object,
      global: {
        type: Boolean,
        default: false,
      },
    },
    mixins: [MsgBoxMixin],
    data() {
      return {
        isLoading: false,
        badgeInternal: { ...this.badge },
        cardOptions: { controls: {} },
        showEditBadge: false,
      };
    },
    computed: {
      live() {
        return this.badgeInternal.enabled !== 'false';
      },
    },
    watch: {
      badge: function badgeWatch(newBadge, oldBadge) {
        if (oldBadge) {
          this.badgeInternal = newBadge;
          this.buildCardOptions();
        }
      },
    },
    mounted() {
      this.buildCardOptions();
    },
    methods: {
      buildCardOptions() {
        const stats = [{
          label: 'Number Skills',
          count: this.badgeInternal.numSkills,
          icon: 'fas fa-graduation-cap skills-color-skills',
        }];
        if (!this.global) {
          stats.push({
            label: 'Total Points',
            count: this.badgeInternal.totalPoints,
            icon: 'far fa-arrow-alt-circle-up skills-color-points',
          });
        } else {
          stats.push({
            label: 'Total Projects',
            count: this.badgeInternal.uniqueProjectCount,
            icon: 'fas fa-trophy skills-color-levels',
          });
        }
        this.cardOptions = {
          navTo: this.buildManageLink(),
          icon: this.badgeInternal.iconClass,
          title: this.badgeInternal.name,
          subTitle: `ID: ${this.badgeInternal.badgeId}`,
          warn: this.badgeInternal.enabled === 'false',
          warnMsg: this.badgeInternal.enabled === 'false' ? 'This badge cannot be achieved until it is live' : '',
          stats,
          controls: {
            navTo: this.buildManageLink(),
            type: this.global ? 'Global Badge' : 'Badge',
            name: this.badgeInternal.name,
            id: this.badgeInternal.badgeId,
            deleteDisabledText: this.deleteDisabledText,
            isDeleteDisabled: this.isDeleteDisabled,
            isFirst: this.badgeInternal.isFirst,
            isLast: this.badgeInternal.isLast,
          },
        };
      },
      buildManageLink() {
        const link = {
          name: this.global ? 'GlobalBadgeSkills' : 'BadgeSkills',
          params: {
            projectId: this.badgeInternal.projectId,
            badgeId: this.badgeInternal.badgeId,
            badge: this.badgeInternal,
          },
        };
        return link;
      },
      deleteBadge() {
        const msg = `Deleting Badge Id: ${this.badgeInternal.badgeId} this cannot be undone.`;
        this.msgConfirm(msg, 'WARNING: Delete Badge').then((res) => {
          if (res) {
            this.badgeDeleted();
          }
        });
      },
      badgeEdited(badge) {
        this.$emit('badge-updated', badge);
      },
      badgeDeleted() {
        this.$emit('badge-deleted', this.badgeInternal);
      },
      moveUp() {
        this.$emit('move-badge-up', this.badgeInternal);
      },
      moveDown() {
        this.$emit('move-badge-down', this.badgeInternal);
      },
      canPublish() {
        if (this.global) {
          return this.badgeInternal.numSkills > 0 || this.badgeInternal.requiredProjectLevels.length > 0;
        }

        return this.badgeInternal.numSkills > 0;
      },
      getNoPublishMsg() {
        let msg = 'This Badge has no assigned Skills. A Badge cannot be published without at least one assigned Skill.';
        if (this.global) {
          msg = 'This Global Badge has no assigned Skills or Project Levels. A Global Badge cannot be published without at least one Skill or Project Level.';
        }

        return msg;
      },
      handlePublish() {
        if (this.canPublish()) {
          const msg = `While this Badge is disabled, user's cannot see the Badge or achieve it. Once the Badge is live, it will be visible to users.
        Please note that once the badge is live, it cannot be disabled.`;
          this.msgConfirm(msg, 'Please Confirm!', 'Yes, Go Live!')
            .then((res) => {
              if (res) {
                this.badgeInternal.enabled = 'true';
                const toSave = { ...this.badgeInternal };
                if (!toSave.originalBadgeId) {
                  toSave.originalBadgeId = toSave.badgeId;
                }
                toSave.startDate = this.toDate(toSave.startDate);
                toSave.endDate = this.toDate(toSave.endDate);
                this.badgeEdited(toSave);
              }
            });
        } else {
          this.msgOk(this.getNoPublishMsg(), 'Empty Badge!');
        }
      },
      toDate(value) {
        let dateVal = value;
        if (value && !(value instanceof Date)) {
          dateVal = new Date(Date.parse(value.replace(/-/g, '/')));
        }
        return dateVal;
      },
      handleHidden(e) {
        if (!e || !e.updated) {
          this.handleFocus();
        }
      },
      handleFocus() {
        this.$nextTick(() => {
          this.$refs.cardNavControls.focusOnEdit();
        });
      },
    },
  };
</script>

<style lang="scss" scoped>
  @import "../../styles/palette";

  .badge-settings {
    position: relative;
    display: inline-block;
    float: right;
  }

  .badge-title {
    display: inline-block;
  }

  .badge-icon {
    font-size: 2rem;
    padding: 10px;
    border: 1px solid #ddd;
    border-radius: 5px;
    box-shadow: 0 22px 35px -16px rgba(0, 0, 0, 0.1);
  }

  .badge-footer-icon-green {
    color: $green-palette-color5;
  }

  .badge-footer-icon-red {
    color: $red-palette-color3;
  }
</style>

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
  <page-preview-card :options="cardOptions">
    <div slot="header-top-right">
      <edit-and-delete-dropdown v-on:deleted="deleteBadge" v-on:edited="showEditBadge=true" v-on:move-up="moveUp"
                                v-on:move-down="moveDown"
                                :isFirst="badgeInternal.isFirst" :isLast="badgeInternal.isLast" :isLoading="isLoading"
                                class="badge-settings"></edit-and-delete-dropdown>
    </div>
    <div slot="footer">
      <i v-if="badgeInternal.endDate" class="fas fa-gem position-absolute" style="font-size: 1rem; top: 1rem; left: 1rem; color: purple"></i>
      <div>
        <router-link :to="buildManageLink()"
                     class="btn btn-outline-primary btn-sm" data-cy="manageBadge">
          Manage <i class="fas fa-arrow-circle-right"/>
        </router-link>
      </div>
      <hr/>
      <div class="float-md-right" style="font-size: 0.8rem;">
        <span v-if="!this.live" data-cy="badgeStatus">
          <span class="text-secondary">Status: </span>
          <span class="text-uppercase">Disabled <span class="far fa-stop-circle text-warning"></span></span> | <a href="#0" @click.stop="handlePublish" class="btn btn-outline-primary btn-sm" data-cy="goLive">Go Live</a>
        </span>
        <span v-else data-cy="badgeStatus">
          <span class="text-secondary">Status: </span> <span class="text-uppercase">Live <span class="far fa-check-circle text-success"></span></span>
        </span>
      </div>

      <edit-badge v-if="showEditBadge" v-model="showEditBadge" :id="badge.badgeId" :badge="badge" :is-edit="true"
                  :global="global" @badge-updated="badgeEdited"></edit-badge>
    </div>
  </page-preview-card>
</template>

<script>
  import EditAndDeleteDropdown from '@/components/utils/EditAndDeleteDropdown';
  import EditBadge from './EditBadge';
  import MsgBoxMixin from '../utils/modal/MsgBoxMixin';
  import PagePreviewCard from '../utils/pages/PagePreviewCard';

  export default {
    name: 'Badge',
    components: { PagePreviewCard, EditAndDeleteDropdown, EditBadge },
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
        cardOptions: {},
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
        }];
        if (!this.global) {
          stats.push({
            label: 'Total Points',
            count: this.badgeInternal.totalPoints,
          });
        } else {
          stats.push({
            label: 'Total Projects',
            count: this.badgeInternal.uniqueProjectCount,
          });
        }
        this.cardOptions = {
          icon: this.badgeInternal.iconClass,
          title: this.badgeInternal.name,
          subTitle: `ID: ${this.badgeInternal.badgeId}`,
          warn: this.badgeInternal.enabled === 'false',
          warnMsg: this.badgeInternal.enabled === 'false' ? 'This badge cannot be achieved until it is live' : '',
          stats,
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

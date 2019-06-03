<template>
  <page-preview-card :options="cardOptions">
    <div slot="header-top-right">
      <edit-and-delete-dropdown v-on:deleted="deleteBadge" v-on:edited="showEditBadge=true" v-on:move-up="moveUp"
                                v-on:move-down="moveDown"
                                :isFirst="badgeInternal.isFirst" :isLast="badgeInternal.isLast" :isLoading="isLoading"
                                class="badge-settings"></edit-and-delete-dropdown>
    </div>
    <div slot="footer">
      <router-link :to="{ name:'BadgeSkills',
              params: { projectId: this.badgeInternal.projectId, badgeId: this.badgeInternal.badgeId}}"
                   class="btn btn-outline-primary btn-sm">
        Manage <i class="fas fa-arrow-circle-right"/>
      </router-link>

      <edit-badge v-if="showEditBadge" v-model="showEditBadge" :id="badge.badgeId" :badge="badge" @badge-updated="badgeEdited"></edit-badge>
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
    props: ['badge'],
    mixins: [MsgBoxMixin],
    data() {
      return {
        isLoading: false,
        badgeInternal: Object.assign({}, this.badge),
        cardOptions: {},
        showEditBadge: false,
      };
    },
    mounted() {
      this.cardOptions = {
        icon: this.badgeInternal.iconClass,
        title: this.badgeInternal.name,
        subTitle: `ID: ${this.badgeInternal.badgeId}`,
        stats: [{
          label: 'Number Skills',
          count: this.badgeInternal.numSkills,
        }, {
          label: 'Number Users',
          count: this.badgeInternal.numUsers,
        }, {
          label: 'Total Points',
          count: this.badgeInternal.totalPoints,
        }],
      };
    },
    methods: {
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
    },
  };
</script>

<style scoped>
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
</style>

<template>
  <div class="box">
    <div class="columns has-text-centered">
      <div class="column is-narrow is-vcentered">
        <i class="has-text-info badge-icon" v-bind:class="`${badgeInternal.iconClass}`"></i>
      </div>
      <div class="column has-text-left">
        <div class="badge-title">
          <h1 class="title is-4 has-text-primary">{{ badgeInternal.name }}</h1>
          <h2 class="subtitle is-7 has-text-grey">ID: {{ badgeInternal.badgeId }}</h2>
        </div>
      </div>
      <div class="column is-narrow">
        <edit-and-delete-dropdown v-on:deleted="deleteBadge" v-on:edited="editBadge" v-on:move-up="moveUp"
                                  v-on:move-down="moveDown"
                                  :isFirst="badgeInternal.isFirst" :isLast="badgeInternal.isLast" :isLoading="isLoading"
                                  class="badge-settings"></edit-and-delete-dropdown>
      </div>
    </div>

    <div class="columns has-text-centered">
      <div class="column is-half">
        <div>
          <p class="heading">Number Skills</p>
          <p class="title">{{ badgeInternal.numSkills | number }}</p>
        </div>
      </div>
      <div class="column is-half">
        <div>
          <p class="heading">Number Users</p>
          <p class="title">{{ badgeInternal.numUsers | number }}</p>
        </div>
      </div>
    </div>

    <div class="columns has-text-centered">
      <div class="column is-full">
        <div>
          <p class="heading">Total Points</p>
          <p class="title">{{ badgeInternal.totalPoints | number }}</p>
        </div>
      </div>
    </div>

    <div class="columns has-text-centered">
      <div class="column is-full">
        <router-link :to="{ name:'BadgePage',
              params: { projectId: this.badgeInternal.projectId, badgeId: this.badgeInternal.badgeId}}"
                     class="button is-outlined is-info">
          <span>Manage</span>
          <span class="icon is-small">
              <i class="fas fa-arrow-circle-right"/>
          </span>
        </router-link>
      </div>
    </div>

  </div>
</template>

<script>
  import EditAndDeleteDropdown from '@/components/utils/EditAndDeleteDropdown';
  import EditBadge from './EditBadge';

  export default {
    name: 'Badge',
    components: { EditAndDeleteDropdown },
    props: ['badge'],
    data() {
      return {
        isLoading: false,
        badgeInternal: Object.assign({}, this.badge),
      };
    },
    methods: {
      deleteBadge() {
        this.$dialog.confirm({
          title: 'WARNING: Delete Badge Action',
          message: `Badge Id: <b>${this.badgeInternal.badgeId}</b> <br/><br/>Delete Action cannot be undone.`,
          confirmText: 'Delete',
          type: 'is-danger',
          hasIcon: true,
          icon: 'exclamation-triangle',
          iconPack: 'fa',
          scroll: 'keep',
          onConfirm: () => this.badgeDeleted(),
        });
      },
      editBadge() {
        this.$modal.open({
          parent: this,
          component: EditBadge,
          hasModalCard: true,
          width: 1110,
          props: {
            badge: this.badgeInternal,
            isEdit: true,
          },
          events: {
            'badge-updated': this.badgeEdited,
          },
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
    box-shadow: 0 22px 35px -16px rgba(0,0,0,0.1);
  }
</style>

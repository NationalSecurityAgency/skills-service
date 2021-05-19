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
    <page-header :loading="isLoading" :options="headerOptions">
      <span slot="right-of-header">
        <i v-if="badge && badge.endDate" class="fas fa-gem ml-2" style="font-size: 1.6rem; color: purple;"></i>
      </span>
      <div slot="subSubTitle" v-if="badge">
        <b-button @click="displayEditBadge"
                  ref="editBadgeButton"
                  class="btn btn-outline-primary mr-1"
                  size="sm"
                  variant="outline-primary"
                  data-cy="btn_edit-badge"
                  :aria-label="'edit Badge '+badge.badgeId">
          <span class="d-none d-sm-inline">Edit </span> <i class="fas fa-edit" aria-hidden="true"/>
        </b-button>
      </div>
    </page-header>

    <navigation :nav-items="[
          {name: 'Skills', iconClass: 'fa-graduation-cap skills-color-skills', page: 'BadgeSkills'},
          {name: 'Users', iconClass: 'fa-users skills-color-users', page: 'BadgeUsers'},
        ]">
    </navigation>
    <edit-badge v-if="showEditBadge" v-model="showEditBadge" :id="badge.badgeId" :badge="badge" :is-edit="true"
                :global="false" @badge-updated="badgeEdited" @hidden="handleHidden"></edit-badge>
  </div>
</template>

<script>
  import { createNamespacedHelpers } from 'vuex';

  import Navigation from '../utils/Navigation';
  import PageHeader from '../utils/pages/PageHeader';
  import EditBadge from './EditBadge';
  import BadgesService from './BadgesService';

  const { mapActions, mapGetters, mapMutations } = createNamespacedHelpers('badges');

  export default {
    name: 'BadgePage',
    components: {
      PageHeader,
      Navigation,
      EditBadge,
    },
    data() {
      return {
        isLoading: true,
        projectId: '',
        badgeId: '',
        showEditBadge: false,
      };
    },
    created() {
      this.projectId = this.$route.params.projectId;
      this.badgeId = this.$route.params.badgeId;
    },
    mounted() {
      this.loadBadge();
    },
    computed: {
      ...mapGetters([
        'badge',
      ]),
      headerOptions() {
        if (!this.badge) {
          return {};
        }
        return {
          icon: 'fas fa-award skills-color-badges',
          title: `BADGE: ${this.badge.name}`,
          subTitle: `ID: ${this.badge.badgeId}`,
          stats: [{
            label: 'Skills',
            count: this.badge.numSkills,
            icon: 'fas fa-graduation-cap skills-color-skills',
          }, {
            label: 'Points',
            count: this.badge.totalPoints,
            icon: 'far fa-arrow-alt-circle-up skills-color-points',
          }],
        };
      },
    },
    methods: {
      ...mapActions([
        'loadBadgeDetailsState',
      ]),
      ...mapMutations([
        'setBadge',
      ]),
      displayEditBadge() {
        this.showEditBadge = true;
      },
      loadBadge() {
        this.isLoading = false;
        if (this.$route.params.badge) {
          this.setBadge(this.$route.params.badge);
          this.isLoading = false;
        } else {
          this.loadBadgeDetailsState({ projectId: this.projectId, badgeId: this.badgeId })
            .finally(() => {
              this.isLoading = false;
            });
        }
      },
      badgeEdited(editedBadge) {
        BadgesService.saveBadge(editedBadge).then((resp) => {
          const origId = this.badge.badgeId;
          this.setBadge(resp);
          if (origId !== resp.badgeId) {
            this.$router.replace({ name: this.$route.name, params: { ...this.$route.params, badgeId: resp.badgeId } });
            this.badgeId = resp.badgeId;
          }
        });
      },
      handleHidden(e) {
        this.showEditBadge = false;
        if (!e || !e.updated) {
          this.handleFocus();
        }
      },
      handleFocus() {
        this.$nextTick(() => {
          const ref = this.$refs.editAndDeleteBadge;
          if (ref) {
            ref.focus();
          }
        });
      },
    },
  };
</script>

<style scoped>

</style>

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
    <sub-page-header title="Badges" action="Badge" @add-action="newBadge" :disabled="addBadgeDisabled" :disabled-msg="addBadgesDisabledMsg"/>
    <loading-container v-bind:is-loading="isLoading">
      <transition name="projectContainer" enter-active-class="animated fadeIn">
        <div>
          <div v-if="badges && badges.length" class="row justify-content-center ">
            <div v-for="(badge) of badges"
                 :key="badge.badgeId" class="col-lg-4 mb-3"  style="min-width: 23rem;">
              <badge :badge="badge"
                     @badge-updated="saveBadge"
                     @badge-deleted="deleteBadge"
                     @move-badge-up="moveBadgeUp"
                     @move-badge-down="moveBadgeDown"/>
            </div>
          </div>

          <no-content3 v-else title="No Badges Yet" sub-title="Start creating badges today!"/>
        </div>
      </transition>
    </loading-container>

    <edit-badge v-if="displayNewBadgeModal" v-model="displayNewBadgeModal" :badge="emptyNewBadge" @badge-updated="saveBadge"></edit-badge>
  </div>
</template>

<script>
  import { createNamespacedHelpers } from 'vuex';
  import { SkillsReporter } from '@skilltree/skills-client-vue';

  import BadgesService from './BadgesService';
  import Badge from './Badge';
  import EditBadge from './EditBadge';
  import LoadingContainer from '../utils/LoadingContainer';
  import NoContent3 from '../utils/NoContent3';
  import SubPageHeader from '../utils/pages/SubPageHeader';

  const { mapActions } = createNamespacedHelpers('projects');

  export default {
    name: 'Badges',
    components: {
      SubPageHeader,
      NoContent3,
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
      loadBadges() {
        BadgesService.getBadges(this.projectId)
          .then((badgesResponse) => {
            this.isLoading = false;
            this.badges = badgesResponse;
            if (this.badges && this.badges.length) {
              this.badges[0].isFirst = true;
              this.badges[this.badges.length - 1].isLast = true;
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
        BadgesService.saveBadge(badgeReq)
          .then(() => {
            this.loadBadges();
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

    },
  };
</script>

<style scoped>

</style>

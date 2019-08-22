<template>
  <div>
    <sub-page-header title="Badges" action="Badge" @add-action="newBadge"/>
    <loading-container v-bind:is-loading="isLoading">
      <transition name="projectContainer" enter-active-class="animated fadeIn">
        <div>
          <div v-if="badges && badges.length" class="row justify-content-center ">
            <div v-for="(badge) of badges"
                 :key="badge.id" class="col-lg-4 mb-3"  style="min-width: 23rem;">
              <badge :badge="badge"
                     @badge-updated="saveBadge"
                     @badge-deleted="deleteBadge"
                     @move-badge-up="moveBadgeUp"
                     @move-badge-down="moveBadgeDown"/>
            </div>
          </div>

          <no-content3 v-if="!badges || badges.length === 0" title="No Badges Yet" sub-title="Start creating badges today!"/>
        </div>
      </transition>
    </loading-container>

    <edit-badge v-if="displayNewBadgeModal" v-model="displayNewBadgeModal" :badge="emptyNewBadge" @badge-updated="saveBadge"></edit-badge>
  </div>
</template>

<script>
  import { createNamespacedHelpers } from 'vuex';

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
            this.isLoading = false;
            this.$emit('badge-deleted', this.badge);
            this.badges = this.badges.filter(item => item.badgeId !== badge.badgeId);
            this.loadProjectDetailsState({ projectId: this.projectId });
            this.$emit('badges-changed', badge.badgeId);
          })
          .finally(() => {
            this.isLoading = false;
          });
      },
      saveBadge(badge) {
        this.isLoading = true;
        const requiredIds = badge.requiredSkills.map(item => item.skillId);
        const badgeReq = Object.assign({ requiredSkillsIds: requiredIds }, badge);
        BadgesService.saveBadge(badgeReq)
          .then(() => {
            this.loadBadges();
            this.loadProjectDetailsState({ projectId: this.projectId });
            this.$emit('badges-changed', badge.badgeId);
          })
          .finally(() => {
            this.isLoading = false;
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
        BadgesService.moveBadge(badge.projectId, badge.badgeId, actionToSubmit)
          .then(() => {
            this.loadBadges();
          })
          .finally(() => {
            this.isLoading = false;
          });
      },

    },
  };
</script>

<style scoped>

</style>

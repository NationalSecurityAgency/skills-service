<template>
  <div>
    <sub-page-header title="Badges" action="Badge" @add-action="newBadge"/>
    <loading-container v-bind:is-loading="isLoading">
      <transition name="projectContainer" enter-active-class="animated fadeIn">
        <div>
          <div v-if="badges && badges.length" class="row justify-content-center ">
            <div v-for="(badge) of badges"
                 :key="badge.id" class="col-lg-4 mb-3"  style="min-width: 23rem;">
              <badge :badge="badge" :global="true"
                     @badge-updated="saveBadge"
                     @  badge-deleted="deleteBadge"
                     @move-badge-up="moveBadgeUp"
                     @move-badge-down="moveBadgeDown"/>
            </div>
          </div>

          <no-content3 v-if="!badges || badges.length === 0" title="No Badges Yet" sub-title="Start creating badges today!"/>
        </div>
      </transition>
    </loading-container>

    <edit-badge v-if="displayNewBadgeModal" v-model="displayNewBadgeModal" :badge="emptyNewBadge" :global="true" @badge-updated="saveBadge"></edit-badge>
  </div>
</template>

<script>
  import GlobalBadgeService from './GlobalBadgeService';
  import Badge from '../Badge';
  import EditBadge from '../EditBadge';
  import LoadingContainer from '../../utils/LoadingContainer';
  import NoContent3 from '../../utils/NoContent3';
  import SubPageHeader from '../../utils/pages/SubPageHeader';

  export default {
    name: 'GlobalBadges',
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
      loadBadges() {
        GlobalBadgeService.getBadges()
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
        GlobalBadgeService.deleteBadge(badge.badgeId)
          .then(() => {
            this.isLoading = false;
            this.$emit('badge-deleted', this.badge);
            this.badges = this.badges.filter(item => item.id !== badge.id);
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
        GlobalBadgeService.saveBadge(badgeReq)
          .then(() => {
            this.loadBadges();
            this.$emit('global-badges-changed', badge.badgeId);
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
        GlobalBadgeService.moveBadge(badge.badgeId, actionToSubmit)
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

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

        <no-content :should-display="!badges || badges.length==0" :title="'No Badges Yet'">
          <div slot="content" class="content" style="width: 100%;">
            <p class="has-text-centered">
              Create your first badge today by pressing
            </p>
            <p class="has-text-centered">
              <a v-on:click="newBadge" class="button is-outlined is-success">
                <span>Add New Badge</span>
                <span class="icon is-small">
              <i class="fas fa-plus-circle"/>
              </span>
              </a>
            </p>
          </div>
        </no-content>
        </div>
      </transition>
    </loading-container>

    <edit-badge v-if="displayNewBadgeModal" v-model="displayNewBadgeModal" :badge="emptyNewBadge" @badge-updated="saveBadge"></edit-badge>
  </div>
</template>

<script>
  import BadgesService from './BadgesService';
  import Badge from './Badge';
  import EditBadge from './EditBadge';
  import LoadingContainer from '../utils/LoadingContainer';
  import NoContent from '../utils/NoContent';
  import SubPageHeader from '../utils/pages/SubPageHeader';

  export default {
    name: 'Badges',
    components: {
      SubPageHeader,
      NoContent,
      LoadingContainer,
      Badge,
      EditBadge,
    },
    props: ['project'],
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
          projectId: this.project.projectId,
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
        BadgesService.getBadges(this.project.projectId)
          .then((badges) => {
            this.isLoading = false;
            this.badges = badges;
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
        BadgesService.saveBadge(badgeReq)
          .then(() => {
            this.loadBadges();
            this.$emit('badges-changed', badge.badgeId);
          })
          .finally(() => {
            this.isLoading = false;
          });
      },
      newBadge() {
        console.log('newBadge called');
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

<template>
  <div>
    <div class="columns skills-underline-container">
      <div class="column">
        <div class="title">Badges</div>
      </div>
      <div class="column has-text-right">
        <a v-on:click="newBadge" class="button is-outlined is-info">
          <span>Add New Badge</span>
          <span class="icon is-small">
              <i class="fas fa-plus-circle"/>
            </span>
        </a>
      </div>
    </div>

    <loading-container v-bind:is-loading="isLoading">
      <transition name="projectContainer" enter-active-class="animated fadeIn">
        <div>
          <div v-if="badges && badges.length" class="columns is-multiline">

            <div v-if="badges && badges.length" v-for="(badge) of badges"
                 :key="badge.id" class="column is-one-third">
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
              <a v-on:click="newBadge" class="button is-outlined is-info">
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
  </div>
</template>

<script>
  import BadgesService from './BadgesService';
  import Badge from './Badge';
  import EditBadge from './EditBadge';
  import LoadingContainer from '../utils/LoadingContainer';
  import NoContent from '../utils/NoContent';

  export default {
    name: 'Badges',
    components: { NoContent, LoadingContainer, EditBadge, Badge },
    props: ['project'],
    data() {
      return {
        isLoading: true,
        badges: [],
        serverErrors: [],
      };
    },
    mounted() {
      this.loadBadges();
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
          .catch((e) => {
            this.isLoading = false;
            this.serverErrors.push(e);
            throw e;
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
          .catch((e) => {
            this.isLoading = false;
            this.serverErrors.push(e);
            throw e;
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
          .catch((e) => {
            this.isLoading = false;
            this.serverErrors.push(e);
            throw e;
        });
      },
      newBadge() {
        const emptyBadge = {
          projectId: this.project.projectId,
          name: '',
          badgeId: '',
          description: '',
          iconClass: 'fas fa-award',
          requiredSkills: [],
        };
        this.$modal.open({
          parent: this,
          component: EditBadge,
          hasModalCard: true,
          width: 1110,
          props: {
            badge: emptyBadge,
          },
          events: {
            'badge-updated': this.saveBadge,
          },
        });
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
          .catch((e) => {
            this.isLoading = false;
            throw e;
        });
      },

    },
  };
</script>

<style scoped>

</style>

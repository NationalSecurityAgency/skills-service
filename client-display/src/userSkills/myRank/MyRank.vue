<template>
  <div class="card h-100 skills-my-rank" @click.stop="openMyRankDetails()"
       :class="{ 'skills-navigable-item': !isSummaryOnly }" data-cy="myRank">
    <div class="card-header">
      <h6 class="card-title mb-0 text-uppercase"><span style="color: yellow">My Rank</span></h6>
    </div>
    <div class="card-body">
      <span class="fa-stack skills-icon user-rank-stack">
        <i class="fa fa-users fa-stack-2x watermark-icon"/>
        <strong class="fa-stack-1x text-primary user-rank-text">
          <span v-if="displayData.userSkillsRanking">
            {{ displayData.userSkillsRanking.position | number }}
          </span>
          <vue-simple-spinner v-else line-bg-color="#333" line-fg-color="#17a2b8"/>
        </strong>
      </span>
    </div>
  </div>
</template>

<script>
  import Spinner from 'vue-simple-spinner';

  export default {
    components: {
      'vue-simple-spinner': Spinner,
    },
    props: {
      displayData: Object,
    },
    methods: {
      openMyRankDetails() {
        if (!this.isSummaryOnly) {
          this.$router.push({
            name: 'myRankDetails',
            params: {
              subjectId: this.displayData.userSkills.subjectId,
            },
          });
        }
      },
    },
    computed: {
      isSummaryOnly() {
        return this.$store.state.isSummaryOnly;
      },
    },
  };
</script>

<style scoped>

  .skills-my-rank .skills-icon {
    display: inline-block;
    color: #b1b1b1;
    margin: 5px 0;
  }

  .skills-my-rank .skills-icon.user-rank-stack {
    margin: 14px 0;
    font-size: 4.1rem;
    width: 100%;
    color: #0fcc15d1;
  }
  .skills-my-rank .skills-icon.user-rank-stack i{
    opacity: 0.38;
  }

  .skills-my-rank .user-rank-text {
    font-size: 0.5em;
    line-height: 1.2em;
    margin-top: 1.8em;
    background: rgba(255, 255, 255, 0.6);
  }
</style>

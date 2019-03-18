<template>
  <div class="my-rank-wrapper">
    <my-rank-modal
      v-if="myRankModal.show"
      :subject="subject"
      @ok="closeMyRankModal"
      @cancel="closeMyRankModal"/>

    <label class="skill-tile-label">My Rank</label>
    <span class="fa-stack skills-icon user-rank-stack">
      <i class="fa fa-users fa-stack-2x"/>
      <strong
        id="user-rank-text"
        class="fa-stack-1x">
          {{ rank | number }}
      </strong>
    </span>
    <button
      class="btn btn-clear-search"
      @click.stop="openMyRankModal(subject)">
      <strong>ALL RANKINGS</strong>
    </button>
  </div>
</template>

<script>
  import MyRankModal from '@/userSkills/myRank/MyRankModal.vue';

  export default {
    components: {
      MyRankModal,
    },
    data() {
      return {
        myRankModal: {
          show: false,
        },
      };
    },
    props: {
      subject: String,
      rank: Number,
    },
    methods: {
      openMyRankModal() {
        this.myRankModal.show = true;
      },

      closeMyRankModal() {
        this.myRankModal.show = false;
      },
    },
  };
</script>

<style scoped>
  .my-rank-wrapper {
    position: relative;
  }

  .skill-tile-label {
    font-size: 19px;
    color: #333;
    width: 100%;
  }

  .skills-icon {
    display: inline-block;
    color: #b1b1b1;
    margin: 5px 0;
  }

  .skills-icon.user-rank-stack i {
    color: #aed7ac;
  }

  .skills-icon.user-rank-stack {
    margin: 14px 0;
    font-size: 60px;
    width: 100%;
    color: #aed7ac;
  }

  #user-rank-text {
    font-size: 0.5em;
    color: #333;
    line-height: 1.2em;
    margin-top: 1.8em;
    background: rgba(255, 255, 255, 0.6);
  }
</style>

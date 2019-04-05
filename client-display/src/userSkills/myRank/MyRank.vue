<template>
  <div class="card">
    <div class="card-header">
      <h5 class="card-title mb-0 float-left">My Rank</h5>
<!--      <span class="badge badge-primary float-right">Badge</span>-->
    </div>
    <div class="card-body">
      <span class="fa-stack skills-icon user-rank-stack">
        <i class="fa fa-users fa-stack-2x"/>
        <strong class="fa-stack-1x user-rank-text">
          {{ rank | number }}
        </strong>
      </span>
      <div>
        <button class="btn btn-info mr-1 text-uppercase" @click.stop="openMyRankDetails()">
          Ranking Stats
        </button>
      </div>
    </div>
  </div>
</template>

<script>
  export default {
    components: {
    },
    props: {
      subject: String,
      rank: Number,
    },
    methods: {
      openMyRankDetails() {
        this.$router.push({
          name: 'myRankDetails',
          params: {
            subject: this.subject,
          },
        });
      },
    },
  };
</script>

<style scoped src="../../assets/bootstrap4tmp/bootstrap.min.css">
</style>

<style scoped>
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

  .user-rank-text {
    font-size: 0.5em;
    color: #333;
    line-height: 1.2em;
    margin-top: 1.8em;
    background: rgba(255, 255, 255, 0.6);
  }
</style>

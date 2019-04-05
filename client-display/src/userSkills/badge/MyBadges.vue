<template>
    <div class="card skills-full-height">
        <div class="card-header">
            <h6 class="card-title mb-0 float-left">My Badges</h6>
        </div>
        <div class="card-body">
      <span class="fa-stack skills-icon user-rank-stack">
        <i class="fa fa-award fa-stack-2x"/>
        <strong class="fa-stack-1x user-rank-text">
          0 out of 3
        </strong>
      </span>
<!--                @click.stop="openMyRankDetails()"-->
         <button class="btn btn-info btn-sm mr-1 text-uppercase" >
                   Badges Catalog
         </button>
      </div>
    </div>
</template>

<script>
    export default {
        name: 'MyBadges',
    };
</script>

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

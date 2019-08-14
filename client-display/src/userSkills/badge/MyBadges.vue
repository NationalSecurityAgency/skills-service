<template>
    <div class="card h-100">
        <div class="card-header">
            <h6 class="card-title mb-0 float-left">My Badges</h6>
        </div>
        <div class="card-body">
            <span class="fa-stack skills-icon user-rank-stack">
                <i class="fa fa-award fa-stack-2x watermark-icon"/>
                <strong class="fa-stack-1x text-primary user-rank-text">
                  {{ numBadgesCompleted }} <span>Badge{{numBadgesCompleted > 1 ? 's' : ''}}</span>
                </strong>
            </span>
            <router-link to="/badges" tag="button" class="btn btn-info btn-sm mr-1 text-uppercase">
                View Badges
            </router-link>
        </div>
    </div>
</template>

<script>
    export default {
        name: 'MyBadges',
        props: {
            numBadgesCompleted: {
                type: Number,
                required: true,
            },
        },
    };
</script>

<style scoped>
    .skills-icon {
        display: inline-block;
        color: #b1b1b1;
        margin: 5px 0;
    }

    .skills-icon.user-rank-stack i {
        color: #0fcc15d1;
        opacity: 0.38;
    }

    .skills-icon.user-rank-stack {
        margin: 14px 0;
        font-size: 60px;
        width: 100%;
        color: #0fcc15d1;
    }

    .user-rank-text {
        font-size: 0.5em;
        line-height: 1.2em;
        margin-top: 1.8em;
        background: rgba(255, 255, 255, 0.6);
    }

    .user-rank-text span {
        font-weight: normal;
    }
</style>

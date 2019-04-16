<template>
    <div class="card">
        <div class="card-header">
            <h6 class="card-title mb-0 float-left text-uppercase">
                My Earned Badges
            </h6>
            <span v-if="badges && badges.length > 0" class="text-muted float-right">
                <span class="badge badge-info">{{ badges.length }}</span> Badge<span v-if="badges.length > 1">s</span> Earned
            </span>
        </div>
        <div class="card-body">
            <no-earned-badges v-if="!badges || badges.length === 0"></no-earned-badges>

            <div v-if="badges && badges.length > 0" class="row justify-content-md-center">
                <div v-for="badge in badges" v-bind:key="badge.badgeId" class="col-lg-3 col-sm-6 my-2">
                    <div class="card skills-full-height">
                        <div class="card-body">
                            <i class="fa fa-check-circle position-absolute text-success" style="right: 10px; top: 10px;"/>
                            <i v-if="badge.gem" class="fas fa-gem position-absolute" style="top: 10px; left: 10px; color: purple"></i>
                            <i :class="badge.iconClass" class="text-primary fa-5x"/>
                            <div class="card-title mb-0 text-truncate">
                                {{ badge.badge }}
                            </div>
                            <div class="text-muted mb-2"><i class="far fa-clock text-secondary" style="font-size: 0.8rem;"></i> {{ badge.dateAchieved | moment("from", "now") }}</div>
                        </div>
                    </div>

                </div>
            </div>

        </div>
    </div>
</template>

<script>
    import NoEarnedBadges from '@/userSkills/badge/NoEarnedBadges.vue';

    export default {
        name: 'MyBadgesDetails',
        components: { NoEarnedBadges },
        props: {
            badges: {
                type: Array,
                required: true,
            },
        },
    };
</script>

<style scoped>

</style>

<template>
    <badge-details-overview :badge="badge">
        <template slot="body-footer" slot-scope="{ props }">
            <router-link :to="{ name: badge.global ? 'globalBadgeDetails' : 'badgeDetails', params: { badgeId: props.badgeId }}" tag="button"
                         class="btn btn-info btn-sm mr-1 text-uppercase">
                View Details
            </router-link>
        </template>
    </badge-details-overview>
</template>

<script>
    import BadgeDetailsOverview from '@/userSkills/badge/BadgeDetailsOverview.vue';

    export default {
        name: 'BadgeCatalogItem',
        components: {
            BadgeDetailsOverview,
        },
        props: {
            badge: {
                type: Object,
                required: true,
            },
        },
    };
</script>

<style scoped>

</style>

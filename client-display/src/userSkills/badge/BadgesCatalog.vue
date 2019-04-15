<template>
    <div class="flex-fill card">
        <div class="card-body">
            <div class="" v-for="(badge, index) in badges" v-bind:key="badge.badgeId">
                <badge-catalog-item :badge="badge" class="pb-3"></badge-catalog-item>
                <div v-if="index !== badges.length - 1">
                    <hr/>
                </div>
            </div>
        </div>
    </div>
</template>

<script>
    import BadgeCatalogItem from '@/userSkills/badge/BadgeCatalogItem.vue';

    export default {
        name: 'BadgesCatalog',
        components: { BadgeCatalogItem },
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

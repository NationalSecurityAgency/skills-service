<template>
    <div class="flex-fill card mb-5">
        <div class="card-header">
            <h6 class="card-title mb-0 float-left text-uppercase">
                Badges Catalog
            </h6>
        </div>
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
        data() {
            return {
                colors: ['text-success', 'text-warning', 'text-danger', 'text-info'],
            };
        },
        methods: {
            getIconColor(index) {
                const colorIndex = index % this.colors.length;
                const color = this.colors[colorIndex];
                return color;
            },
        },
    };
</script>

<style scoped>

</style>

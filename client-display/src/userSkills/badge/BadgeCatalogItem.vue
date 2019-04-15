<template>
    <div class="media">
        <i :class="badge.iconClass" class="mr-4 text-success fa-3x" style="min-width: 3rem;, max-width: 4rem;"/>
        <div class="media-body text-left catalog-item">
            <small class="float-right text-navy">0% Complete</small>
            <h4 class="mb-1">{{ badge.badge }}</h4>
            <div class="mb-2">
                <progress-bar bar-color="lightgreen" :val="3"></progress-bar>
            </div>

            <p class="">
                {{ badge.description }}
            </p>

            <router-link to="/badges" tag="button" class="btn btn-info btn-sm mr-1 text-uppercase">
                View Details
            </router-link>
        </div>
    </div>

    <!--    <div class="card">-->
    <!--        <div class="card-header">-->
    <!--            <h3>{{ badge.badge }}</h3>-->
    <!--        </div>-->
    <!--        <div class="card-body">-->
    <!--            <div class="text-primary">-->
    <!--                <i :class="badge.iconClass" class="badge-icon fa-5x" />-->
    <!--            </div>-->
    <!--            <div class="pt-2">-->
    <!--                <router-link to="/badges" tag="button" class="btn btn-info btn-sm mr-1 text-uppercase">-->
    <!--                    View Details-->
    <!--                </router-link>-->
    <!--            </div>-->
    <!--        </div>-->
    <!--    </div>-->
</template>

<script>
    import ProgressBar from 'vue-simple-progress';

    export default {
        name: 'BadgeCatalogItem',
        components: {
            ProgressBar,
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
    .catalog-item {
        font-size: 0.8rem;
        font-weight: 400;
        line-height: 1.5;
        color: #4f565d;
    }

</style>

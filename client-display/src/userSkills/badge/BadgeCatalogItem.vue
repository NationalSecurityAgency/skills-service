<template>
    <div class="row">
        <div class="col-md-2">
            <div class="card mb-2">
                <div class="card-body">
                    <i :class="badge.iconClass" class="text-success fa-3x" style="min-width: 3rem;, max-width: 4rem;"/>
                    <i v-if="badge.gem" class="fas fa-gem position-absolute" style="top: 5px; right: 5px; color: purple"></i>
                </div>
            </div>
        </div>

        <div class="text-left catalog-item col-md-10">
            <small class="float-right text-navy" :class="{ 'text-success': percent === 100 }">
               <i v-if="percent === 100" class="fa fa-check"/> {{ percent }}% Complete
            </small>
            <h4 class="mb-1">{{ badge.badge }}</h4>
            <div class="mb-2">
                <progress-bar bar-color="lightgreen" :val="percent"></progress-bar>
            </div>

            <p class="">
                {{ badge.description }}
            </p>

            <router-link :to="{ name: 'badgeDetails', params: { badgeId: badge.badgeId }}" tag="button" class="btn btn-info btn-sm mr-1 text-uppercase">
                View Details
            </router-link>
        </div>
    </div>
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
        computed: {
            percent() {
                return Math.trunc((this.badge.numSkillsAchieved / this.badge.numTotalSkills) * 100);
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

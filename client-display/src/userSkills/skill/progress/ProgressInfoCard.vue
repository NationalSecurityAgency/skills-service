<template>
    <div class="card h-100">
        <div class="card-body container bg-light">
            <div class="media">
                <div class="d-inline-block mt-2 mr-3">
                    <i class="fa-3x" :class="icon"/>
                </div>
                <div class="media-body text-primary">
                    <h3 class="mb-0">
                        <span>{{title | number}}</span>
                    </h3>
                    <div class="mt-0 pt-0">{{subTitle}}</div>
                </div>
            </div>
            <div class="mt-2">{{label}}</div>
        </div>
    </div>
</template>

<script>
    export default {
        name: 'ProgressInfoCard',
        props: {
            title: Number,
            label: String,
            icon: String,
            subTitle: {
                type: String,
                default: 'Points',
            },
        },
    };
</script>

<style scoped>

</style>

<template>
    <div class="card h-100">
        <div class="card-body container">
            <div class="media">
                <div class="d-inline-block mt-2 mr-3">
                    <i class="fa-3x" :class="icon"/>
                </div>
                <div class="media-body">
                    <h3 class="mb-0">{{points | number}}</h3>
                    <div class="mt-0 pt-0 text-primary">Points</div>
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
            points: Number,
            label: String,
            icon: String,
        },
    };
</script>

<style scoped>
    .container {
        background-color: #eeeeee;
    }

    .media .media-body {
        color: black;
    }

</style>

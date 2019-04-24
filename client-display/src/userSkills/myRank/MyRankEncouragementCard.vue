<template>
    <div class="card">
        <div class="card-body">
            <div class="media">
                <div class="d-inline-block mt-2 mr-3 border p-3 rounded-circle">
                    <i class="fa-3x" :class="icon" style="min-width: 3.5rem;"/>
                </div>
                <div class="media-body text-left">
                    <slot/>
                </div>
            </div>
        </div>
    </div>
</template>

<script>
    export default {
        name: 'MyRankEncouragementCard',
        props: {
            icon: String,
        },
    };
</script>

<style scoped>

</style>

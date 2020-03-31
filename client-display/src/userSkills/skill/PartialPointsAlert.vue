<template>
    <div v-if="isLocked && skill.points > 0" class="alert alert-info mt-2 text-center">
        <p>
            <i class="fas fa-info-circle mr-1"></i> You were able to earn partial points before dependencies were added. Don't worry you get to keep those points!!!
        </p>
        <hr>
        <div class="text-center">Accomplish all of the dependencies to unlock the rest of this skill's points!</div>
    </div>
</template>

<script>
    export default {
        name: 'PartialPointsAlert',
        props: {
            skill: Object,
            isLocked: Boolean,
        },
    };
</script>

<style scoped>

</style>

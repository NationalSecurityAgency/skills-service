<template>
    <bootstrap-container title="Initialization is Done!">
        <p><i class="far fa-check-square"></i> The root account has been successfully created!</p>
        <p style="margin-top:2em">Click Continue to proceed the User Skills Management Dashboard.</p>
        <div class="text-center">
            <button class="btn btn-outline-primary" @click="$emit('proceed')">
                <i class="fas fa-arrow-circle-right"/> Continue
            </button>
        </div>
    </bootstrap-container>
</template>

<script>
    import BootstrapContainer from './BootstrapContainer';

    export default {
        name: 'Success',
        components: { BootstrapContainer }
    };
</script>

<style scoped>
</style>

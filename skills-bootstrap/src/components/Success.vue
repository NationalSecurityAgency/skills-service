<template>
    <bootstrap-container title="Initialization is Done!">
        <p><i class="far fa-check-square text-success"></i> The root account has been successfully created!</p>
        <p><i class="far fa-check-square text-success"></i> Inception self-training project created!</p>
        <p style="margin-top:2em">Please proceed the User Skills Dashboard.</p>
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

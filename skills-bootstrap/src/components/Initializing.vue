<template>
    <bootstrap-container title="Initialization...">
        <div class="text-center">
            <div class="spinner-border text-primary" role="status">
                <span class="sr-only">Loading...</span>
            </div>
            <p>
                This may take a second or two!
            </p>
        </div>
    </bootstrap-container>
</template>

<script>
    import BootstrapContainer from './BootstrapContainer';

    export default {
        name: 'Initializing',
        components: { BootstrapContainer },
    };
</script>

<style scoped>

</style>

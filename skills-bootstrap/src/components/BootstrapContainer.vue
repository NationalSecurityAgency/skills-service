<template>
    <div class="row justify-content-center">
        <div class="col-md-7 col-lg-6 col-xl-4 mt-3">
            <div class="text-center mt-5">
                <i class="fa fa-users fa-4x"></i>
                <h2 class="mt-4">{{ title }}</h2>
            </div>
            <div class="card">
                <div class="card-body p-4">
                    <slot/>
                </div>
            </div>
        </div>
    </div>
</template>

<script>
    export default {
        name: 'BootstrapContainer',
        props: {
            title: String,
        }
    };
</script>

<style scoped>

</style>

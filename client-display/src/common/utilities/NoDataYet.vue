<template>
    <div class="row text-secondary skills-no-data-yet">
        <div class="col">
                    <span class="fa-stack fa-3x " style="vertical-align: top;">
                      <i class="fas fa-circle fa-stack-2x"></i>
                      <i class="fas fa-battery-empty fa-stack-1x fa-inverse"></i>
                    </span>
        </div>
        <div class="w-100"></div>
        <div class="col pt-2">
            <h3>{{ title }}</h3>
        </div>
        <div class="w-100"></div>
        <div class="col">
            {{ subTitle }}
        </div>

    </div>
</template>

<script>
    export default {
        name: 'NoDataYet',
        props: {
            title: String,
            subTitle: String,
        },
    };
</script>

<style scoped>
    /*.skills-no-data-yet .fa-inverse {*/
    /*    color: yellow !important;*/
    /*}*/
</style>

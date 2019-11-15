<template>
  <b-alert
    v-model="showNewVersionAlert"
    class="position-fixed fixed-top m-0 rounded-0"
    style="z-index: 2000;"
    variant="success"
    dismissible
  >
    New Software Version is Available!! Please click <a href="" @click="window.location.reload()">Here</a>
    to reload.
  </b-alert>
</template>

<script>
    export default {
        name: 'NewSoftwareVersionComponent',
        data() {
            return {
                showNewVersionAlert: false,
                currentLibVersion: undefined,
            };
        },
        computed: {
            libVersion() {
                return this.$store.getters.libVersion;
            },
        },
        watch: {
            libVersion() {
                if (this.currentLibVersion === undefined) {
                    this.currentLibVersion = this.libVersion;
                } else if (this.currentLibVersion !== this.libVersion) {
                    this.showNewVersionAlert = true;
                }
            },
        },
    };
</script>

<style scoped>

</style>

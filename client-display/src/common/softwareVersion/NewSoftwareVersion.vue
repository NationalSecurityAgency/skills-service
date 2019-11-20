<template>
  <div class="container mt-2" v-if="showNewVersionAlert">
        <div class="alert alert-warning alert-dismissible fade show" role="alert">
    New Skills Software Version is Available!! Please click <a href="javascript:void(0)" @click="reload()">Here</a>
    to reload.
  </div>
  </div>
</template>

<script>
    export default {
        name: 'NewSoftwareVersionComponent',
        props: {
          refreshUrl: {
            type: String,
            required: false,
          },
        },
        data() {
            return {
                showNewVersionAlert: false,
                currentLibVersion: undefined,
            };
        },
        computed: {
            libVersion() {
                return this.$store.state.softwareVersion;
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
        methods: {
            reload() {
              if (this.refreshUrl) {
                location.href = this.refreshUrl;
              } else {
                location.reload();
              }
            },
        },
    };
</script>

<style scoped>
  .newVersionAlert {
    /*max-width: 70rem;*/
  }
</style>

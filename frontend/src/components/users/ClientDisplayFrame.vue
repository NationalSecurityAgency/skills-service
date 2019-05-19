<template>
  <div ref="iframeContainer" />
</template>

<script>
  import Vue from 'vue';
  import VueScrollTo from 'vue-scrollto';
  import Postmate from 'postmate';
  import axios from 'axios';

  Vue.use(VueScrollTo);

  export default {
    props: {
      serviceUrl: {
        type: String,
        default: '',
      },
      projectId: {
        type: String,
        required: true,
      },
      authenticationUrl: {
        type: String,
        required: true,
      },
    },
    data() {
      return {
        childFrame: null,
        authenticationPromise: null, // Vuex would be more appropriate
      };
    },
    mounted() {
      const handshake = new Postmate({
        container: this.$refs.iframeContainer,
        url: '/static/clientPortal/index.html',
        classListArray: ['client-display-iframe'],
        model: {
          serviceUrl: this.serviceUrl,
          projectId: this.projectId,
        },
      });

      handshake.then((child) => {
        this.childFrame = child;
        child.on('height-changed', (data) => {
          if (data > 0) {
            const adjustedHeight = Math.max(data, window.screen.height);
            this.$refs.iframeContainer.height = adjustedHeight;
            this.$refs.iframeContainer.style.height = `${adjustedHeight}px`;
          }
        });
        child.on('route-changed', () => {
          VueScrollTo.scrollTo(this.$refs.iframeContainer, 1000, {
            y: true,
            x: false,
          });
        });
        child.on('needs-authentication', () => {
          if (!this.authenticationPromise) {
            this.authenticationPromise = axios.get(this.authenticationUrl)
              .then((result) => {
                child.call('updateAuthenticationToken', result.data.access_token);
              })
              .finally(() => {
                this.authenticationPromise = null;
              });
          }
        });
      });
    },
    beforeDestroy() {
      this.childFrame.destroy();
    },
  };
</script>

<style>
  .client-display-iframe {
    width: 100%;
    height: 100%;
    border: 0;
  }
</style>

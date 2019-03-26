<template>
  <div id="client-portal-frame">
    <iframe
      v-if="authToken"
      ref="theIframe"
      class="the-iframe"
      src="/static/clientPortal/index.html"/>
  </div>
</template>

<script>
  import Vue from 'vue';
  import VueScrollTo from 'vue-scrollto';
  import ClientDisplayFrameMessage from '../utils/ClientDisplayFrameMessage';

  Vue.use(VueScrollTo);

  export default {
    props: {
      authToken: {
        type: String,
        required: true,
      },
      serviceUrl: {
        type: String,
        default: '',
      },
      projectId: {
        type: String,
        required: true,
      },
    },
    created() {
      this.messageHandler = (event) => {
        const messageParser = new ClientDisplayFrameMessage(event.data);
        console.log(event.data);
        if (messageParser.isSkillsMessage()) {
          const parsedMessage = messageParser.getParsedMessage();
          if (parsedMessage.name === 'frame-loaded') {
            this.$refs.theIframe.height = parsedMessage.payload.contentHeight;
            const bindings = {
              projectId: this.projectId,
              serviceUrl: this.serviceUrl,
              authToken: this.authToken,
            };
            this.$refs.theIframe.contentWindow.postMessage(`skills::data-init::${JSON.stringify(bindings)}`, '*');
          } else if (parsedMessage.name === 'route-changed') {
            VueScrollTo.scrollTo(this.$refs.theIframe, 0, {
              y: true,
              x: false,
              offset: -60,
            });
          }
        }
      };
      window.addEventListener('message', this.messageHandler);
    },
    beforeDestroy() {
      window.removeEventListener('message', this.messageHandler);
    },
  };
</script>

<style scoped>
  #client-portal-frame {
    max-width: 1000px;
  }

  .the-iframe {
    width: 100%;
  }
</style>

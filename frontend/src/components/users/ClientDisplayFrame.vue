<template>
  <div id="client-portal-frame">
    <iframe
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
    created() {
      this.messageHandler = (event) => {
        const messageParser = new ClientDisplayFrameMessage(event.data);
        if (messageParser.isSkillsMessage()) {
          const parsedMessage = messageParser.getParsedMessage();
          if (parsedMessage.name === 'height-change') {
            if (parsedMessage.payload.contentHeight > 0) {
              const adjustedHeight = Math.max(parsedMessage.payload.contentHeight, window.screen.height);
              this.$refs.theIframe.height = adjustedHeight;
              this.$refs.theIframe.style.height = `${adjustedHeight}px`;
            }
          } else if (parsedMessage.name === 'frame-initialized') {
            const bindings = {
              projectId: this.projectId,
              authenticationUrl: this.authenticationUrl,
              serviceUrl: this.serviceUrl,
            };
            this.$refs.theIframe.contentWindow.postMessage(`skills::data-init::${JSON.stringify(bindings)}`, '*');
          } else if (parsedMessage.name === 'route-changed') {
            VueScrollTo.scrollTo(this.$refs.theIframe, 1000, {
              y: true,
              x: false,
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
  .the-iframe {
    width: 100%;
    height: 1000px;
    border: 0;
  }
</style>

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
  import ClientDisplayFrameMessage from '../utils/ClientDisplayFrameMessage';

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
      window.addEventListener('message', (event) => {
        const messageParser = new ClientDisplayFrameMessage(event.data);
        if (messageParser.isSkillsMessage()) {
          const parsedMessage = messageParser.getParsedMessage();
          if (parsedMessage.name === 'frame-loaded') {
            console.log('received contentheight', parsedMessage.payload.contentHeight);
            this.$refs.theIframe.height = parsedMessage.payload.contentHeight;
            const bindings = {
              projectId: this.projectId,
              serviceUrl: this.serviceUrl,
              authToken: this.authToken,
            };
            this.$refs.theIframe.contentWindow.postMessage(`skills::data-init::${JSON.stringify(bindings)}`, '*');
          }
        }
      });
    },
  };
</script>

<style scoped>
  .the-iframe {
    width: 100%;
  }
</style>

<template>
  <div id="client-portal-frame">
    <iframe
      v-if="authToken"
      ref="theIframe"
      width="1500"
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
  #client-portal-frame {
    width: 1000px;
  }
</style>

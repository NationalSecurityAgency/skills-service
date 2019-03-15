<template>
  <div id="client-portal-frame">
    <iframe
      ref="theIframe"
      width="1500"
      src="/static/clientPortal/index.html"/>
  </div>
</template>

<script>
  class SkillsFrameMessageParser {
    constructor(message) {
      this.parsedMessage = this.parseMessage(message);
    }

    parseMessage(message) {
      let parsedMessage = null;
      const split = message && message.split ? message.split('::') : [];
      if (split.length >= 2 && split[0] === 'skills') {
        const name = split[1];
        const payload = split[2] ? JSON.parse(split[2]) : null;
        parsedMessage = {
          name,
          payload,
        };
      }
      return parsedMessage;
    }

    getParsedMessage() {
      return this.parsedMessage;
    }

    isSkillsMessage() {
      let isSkillsMessage = false;
      if (this.parsedMessage) {
        isSkillsMessage = true;
      }
      return isSkillsMessage;
    }
  }

  export default {
    created() {
      window.addEventListener('message', (event) => {
        const messageParser = new SkillsFrameMessageParser(event.data);
        if (messageParser.isSkillsMessage()) {
          const parsedMessage = messageParser.getParsedMessage();
          if (parsedMessage.name === 'frame-loaded'){
            this.$refs.theIframe.height = parsedMessage.payload.contentHeight;
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

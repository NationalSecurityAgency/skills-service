export default class ClientDisplayFrameMessage {
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

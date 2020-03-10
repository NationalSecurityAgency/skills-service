/*
 * Copyright 2020 SkillTree
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
export default class ClientDisplayFrameMessage {
  constructor(message) {
    this.parsedMessage = ClientDisplayFrameMessage.parseMessage(message);
  }

  static parseMessage(message) {
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

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
import {
  SkillsReporter,
  SUCCESS_EVENT,
  FAILURE_EVENT,
} from '@skilltree/skills-client-js';

import debounce from 'lodash/debounce';

const eventCache = new WeakMap();

const eventListener = (el, skillId) => debounce(() => {
  SkillsReporter.reportSkill(skillId)
    .then((result) => {
      const event = new CustomEvent(SUCCESS_EVENT, { detail: result });
      el.dispatchEvent(event);
    })
    .catch((error) => {
      const event = new CustomEvent(FAILURE_EVENT, { detail: error });
      el.dispatchEvent(event);
    });
}, 500);

export default {
  name: 'skills',
  inserted(el, binding) {
    const eventContext = {
      name: binding.arg ? binding.arg : 'click',
      handler: eventListener(el, binding.value),
    };
    el.addEventListener(eventContext.name, eventContext.handler);
    eventCache.set(el, eventContext);
  },
  unbind(el) {
    const eventContext = eventCache.get(el);
    setTimeout(() => {
      el.removeEventListener(eventContext.name, eventContext.handler);
      eventCache.delete(el);
    });
  },
};

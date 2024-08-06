/*
 * Copyright 2024 SkillTree
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
import { useDebounceFn } from '@vueuse/core'
import { SkillsReporter, SUCCESS_EVENT, FAILURE_EVENT } from '@skilltree/skills-client-js';
import { useAppConfig } from '@/common-components/stores/UseAppConfig.js'

export const useSkillsReporterDirective = () => {
  const eventCache = new WeakMap();
  const appConfig = useAppConfig()
  const eventListener = (el, skillId) => useDebounceFn(() => {
    SkillsReporter.reportSkill(skillId)
      .then((result) => {
        const event = new CustomEvent(SUCCESS_EVENT, { detail: result });
        el.dispatchEvent(event);
      })
      .catch((error) => {
        const event = new CustomEvent(FAILURE_EVENT, { detail: error });
        el.dispatchEvent(event);
      });
  }, appConfig.formFieldDebounceInMs);

  const vSkills = {
    mounted: (el, binding) => {
      const eventContext = {
        name: binding.arg ? binding.arg : 'click',
        handler: eventListener(el, binding.value),
      };
      el.addEventListener(eventContext.name, eventContext.handler);
      eventCache.set(el, eventContext);
    },
    unmounted: (el) => {
      const eventContext = eventCache.get(el);
      setTimeout(() => {
        el.removeEventListener(eventContext.name, eventContext.handler);
        eventCache.delete(el);
      });
    }
  }

  const vSkillsOnMounted = {
    mounted: (el, binding) => {
      const {projectId, subjectId, skillId} = binding.value;
      const skillsReporter = new SkillsReporter(projectId, subjectId, skillId);
      skillsReporter.render(el);
    },
  }

  return {
    vSkills,
    vSkillsOnMounted,
  }
}
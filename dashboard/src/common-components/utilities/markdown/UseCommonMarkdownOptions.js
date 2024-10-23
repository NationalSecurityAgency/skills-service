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
import * as emoji from 'node-emoji';
export const useCommonMarkdownOptions = () => {
  const markdownOptions = {
    linkAttributes: {
      target: '_blank',
        rel: 'noopener noreferrer',
    },
    customHTMLRenderer: {
      link(node, context) {
        const { origin, entering } = context;
        const result = origin();
        if (!entering) {
          const faIcon = node?.destination?.startsWith('/api/download/') ? 'fas fa-download' : 'fas fa-external-link-alt';
          return {
            type: 'html',
            content: ` <span class="${faIcon}" style="font-size: 0.8rem"></span></a>`,
          };
        }
        return result;
      },
      text(node, context) {
        const { origin, entering } = context;
        const result = origin();
        const onMissing = (name) => name;
        const emojified = emoji.emojify(result.content, onMissing);
        if (entering) {
          return {
            type: 'text',
            content: emojified,
          };
        }
        return result;
      },
    },
  }

  const getMenuItem = () => document.querySelector('.toastui-editor-popup-body [aria-role="menu"]')
  const getMenuPopup = (id) => document.querySelector(`#${id} .toastui-editor-popup-body`)
  const getMarkdownEditor = (id) => document.querySelector(`[data-cy="markdownEditorInput"] #${id}`)
  const getHeaderButton = (id) => getMarkdownEditor(id)?.querySelector(`#${id} [aria-label="Headings"]`)
  const getMouseEvent = () => new MouseEvent('click', { view: window, bubbles: true, cancelable: true })

  return {
    markdownOptions,
    getMenuItem,
    getMenuPopup,
    getMarkdownEditor,
    getHeaderButton,
    getMouseEvent
  }
}
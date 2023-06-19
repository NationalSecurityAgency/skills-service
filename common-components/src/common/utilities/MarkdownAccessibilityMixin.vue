/*
Copyright 2020 SkillTree

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    https://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
<script>
  const getMenuItem = () => document.querySelector('.toastui-editor-popup-body [aria-role="menu"]');
  const getHeaderButton = () => {
    const markdownEditor = document.getElementById('toastuiEditor');
    const headerButton = markdownEditor.querySelector('.heading');
    return headerButton;
  };

  export default {
    name: 'MarkdownAccessibilityMixin',
    methods: {
      fixToolbarButtonSelectorAccessibilityIssues() {
        this.$nextTick(() => {
          this.$nextTick(() => {
            const headerButton = getHeaderButton();
            if (headerButton) {
              headerButton.setAttribute('aria-haspopup', 'menu');
              headerButton.setAttribute('aria-expanded', false);
              headerButton.setAttribute('aria-controls', 'headerChoicesId');
              headerButton.setAttribute('id', 'headerButtonId');
              headerButton.addEventListener('click', this.handleHeaderButtonClick);
              headerButton.addEventListener('keydown', this.handleHeaderButtonKeydown);
            }
          });
        });
      },
      handleHeaderButtonKeydown(event) {
        if (event.key === 'ArrowDown') {
          const menu = getMenuItem();
          const firstItem = menu.querySelector('[data-level="1"]');
          firstItem.setAttribute('tabindex', 0);
          firstItem.focus();
        }
      },
      handleHeaderButtonClick(btnClickEvent) {
        const menu = getMenuItem();
        menu.setAttribute('id', 'headerChoicesId');
        menu.setAttribute('role', 'menu');
        menu.setAttribute('tabindex', -1);
        menu.setAttribute('aria-labelledby', 'headerButtonId');
        const menuitems = menu.querySelectorAll('[aria-role="menuitem"]');
        menuitems.forEach((item) => {
          item.setAttribute('tabindex', -1);
          item.setAttribute('role', 'menuitem');
          item.addEventListener('keydown', this.handleNavigationAndSelection);
        });
        btnClickEvent.srcElement.setAttribute('aria-expanded', true);
      },
      handleNavigationAndSelection(event) {
        if (event.key === 'Enter') {
          event.preventDefault();
          const evt = new MouseEvent('click', {
            view: window,
            bubbles: true,
            cancelable: true,
          });
          event.srcElement.dispatchEvent(evt);
        } else if (event.key === 'ArrowDown') {
          this.handleUpAndDownNavigation(event.srcElement, 1);
        } else if (event.key === 'ArrowUp') {
          this.handleUpAndDownNavigation(event.srcElement, -1);
        }
      },
      handleUpAndDownNavigation(currentItem, numToAdd) {
        const currentCount = currentItem.getAttribute('data-level');
        let nextCount = Number(currentCount) + numToAdd;
        if (nextCount < 0) {
          nextCount = 6;
        }
        let selector = `[data-level="${nextCount}"]`;
        if (nextCount > 6 || nextCount < 1) {
          selector = '[data-type="Paragraph"]';
        }
        const nextItem = getMenuItem().querySelector(selector);
        if (nextItem) {
          nextItem.setAttribute('tabindex', 0);
          nextItem.focus();
          currentItem.setAttribute('tabindex', -1);
        }
      },
    },
  };
</script>

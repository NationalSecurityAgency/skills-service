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
  const getMenuPopup = () => document.querySelector('.toastui-editor-popup-body');
  const getMarkdownEditor = () => document.getElementById('toastuiEditor');
  const getHeaderButton = () => getMarkdownEditor()?.querySelector('.heading');

  const getMouseEvent = () => new MouseEvent('click', { view: window, bubbles: true, cancelable: true });

  export default {
    name: 'MarkdownAccessibilityMixin',
    methods: {
      clickOnHeaderToolbarButton() {
        const btn = getHeaderButton();
        if (btn) {
          btn.dispatchEvent(getMouseEvent());
          btn.focus();
        }
      },
      clickOnFontSizeToolbarButton() {
        this.doClickOnToolbarButton('[skilltree-id="fontSizeBtn"]');
      },
      clickOnImageToolbarButton() {
        this.doClickOnToolbarButton('.image');
      },
      clickOnLinkToolbarButton() {
        this.doClickOnToolbarButton('.link');
      },
      clickOnAttachmentToolbarButton() {
        this.doClickOnToolbarButton('.attachment-button');
      },
      doClickOnToolbarButton(selector) {
        const markdownEditor = getMarkdownEditor();
        if (markdownEditor) {
          const btn = markdownEditor.querySelector(selector);
          btn.dispatchEvent(getMouseEvent());
          btn.focus();
        }
      },
      fixAccessibilityIssues() {
        this.fixHeaderButtonIssues();
        this.fixMoreButtonAriaLabel(1);
        this.fixFontSizeButtonIssues();
        this.fixInsertImageButtonIssues();
        this.fixInsertUrlButtonIssues();
      },
      fixInsertUrlButtonIssues() {
        this.$nextTick(() => {
          const markdownEditor = getMarkdownEditor();
          if (markdownEditor) {
            const imageButton = markdownEditor.querySelector('.link');
            imageButton.addEventListener('click', this.handleUrlButtonClick);
          }
        });
      },
      handleUrlButtonClick() {
        this.$nextTick(() => {
          this.$nextTick(() => this.$announcer.polite('Insert hyperlink into text'));
          const urlInput = getMenuPopup().querySelector('#toastuiLinkUrlInput');
          urlInput.focus();
        });
      },
      fixInsertImageButtonIssues() {
        this.$nextTick(() => {
          const markdownEditor = getMarkdownEditor();
          if (markdownEditor) {
            const imageButton = markdownEditor.querySelector('.image');
            imageButton.addEventListener('click', this.handleImageButtonClick);
          }
        });
      },
      handleImageButtonClick() {
        this.$nextTick(() => {
          this.$nextTick(() => this.$announcer.polite('Insert image by uploading a file or via an external URL. File Tab is currently active but please use left and right keys to switch between the tabs.'));
          const menuPopup = getMenuPopup();
          const activeTab = menuPopup.querySelector('.tab-item.active');
          activeTab.focus();
          const hiddenFileUploadInput = menuPopup.querySelector('#toastuiImageFileInput');
          hiddenFileUploadInput.setAttribute('tabindex', -1);

          const tabs = menuPopup.querySelectorAll('.tab-item');
          tabs.forEach((tab) => {
            tab.addEventListener('keydown', (event) => {
              if (event.key === 'ArrowLeft' || event.key === 'ArrowRight') {
                const otherTab = menuPopup.querySelector('.tab-item:not(.active)');
                otherTab.dispatchEvent(getMouseEvent());
                otherTab.focus();
              }
            });
          });
          const okButton = menuPopup.querySelector('.toastui-editor-ok-button');
          okButton.setAttribute('tabindex', 0);
          const cancelButton = menuPopup.querySelector('.toastui-editor-close-button');
          cancelButton.setAttribute('tabindex', 0);
          const descriptonInput = menuPopup.querySelector('#toastuiAltTextInput');
          descriptonInput.setAttribute('tabindex', 0);
          const selectFileButton = menuPopup.querySelector('.toastui-editor-file-select-button');
          selectFileButton.setAttribute('tabindex', 0);
        });
      },
      fixFontSizeButtonIssues() {
        this.$nextTick(() => {
          const markdownEditor = getMarkdownEditor();
          if (markdownEditor) {
            const fontButton = markdownEditor.querySelector('[aria-label="F"]');
            fontButton.setAttribute('aria-label', 'Font Size');
            fontButton.setAttribute('skilltree-id', 'fontSizeBtn');
            fontButton.addEventListener('click', this.handleFontSizeButtonClick);
          }
        });
      },
      handleFontSizeButtonClick() {
        this.$nextTick(() => {
          const sizeInput = getMenuPopup().querySelector('.size-input');
          sizeInput.setAttribute('aria-label', 'Set Font Size in pixels');
          sizeInput.classList.add('w-100');
          const menuitems = getMenuPopup().querySelectorAll('.drop-down .drop-down-item');
          menuitems.forEach((item) => {
            item.setAttribute('aria-hidden', true);
          });
          sizeInput.focus();
        });
      },
      fixMoreButtonAriaLabel(attemptNum) {
        if (attemptNum <= 10) {
          setTimeout(() => {
            this.$nextTick(() => {
              const markdownEditor = getMarkdownEditor();
              if (markdownEditor) {
                const moreButton = markdownEditor.querySelector('.more');
                if (moreButton) {
                  moreButton.setAttribute('aria-label', 'More Toolbar Controls');
                } else {
                  this.fixMoreButtonAriaLabel(attemptNum + 1);
                }
              }
            });
          }, 300);
        }
      },
      fixHeaderButtonIssues() {
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
        this.$nextTick(() => this.$announcer.polite('Select header type or paragraph text by using up and down keys.'));
        const menu = getMenuItem();
        menu.setAttribute('id', 'headerChoicesId');
        menu.setAttribute('role', 'menu');
        menu.setAttribute('tabindex', -1);
        menu.setAttribute('aria-labelledby', 'headerButtonId');
        const menuitems = menu.querySelectorAll('[aria-role="menuitem"]');
        menuitems.forEach((item) => {
          item.setAttribute('tabindex', -1);
          item.setAttribute('role', 'menuitem');
          item.addEventListener('keydown', this.handleHeaderNavigationAndSelection);
        });
        btnClickEvent.srcElement.setAttribute('aria-expanded', true);
      },
      handleHeaderNavigationAndSelection(event) {
        if (event.key === 'Enter') {
          event.preventDefault();
          event.srcElement.dispatchEvent(getMouseEvent());
        } else if (event.key === 'ArrowDown') {
          this.handleHeaderUpAndDownNavigation(event.srcElement, 1);
        } else if (event.key === 'ArrowUp') {
          this.handleHeaderUpAndDownNavigation(event.srcElement, -1);
        }
      },
      handleHeaderUpAndDownNavigation(currentItem, numToAdd) {
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

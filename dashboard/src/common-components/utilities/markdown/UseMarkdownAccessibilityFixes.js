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
import { nextTick, watch } from 'vue'
import { useCommonMarkdownOptions } from './UseCommonMarkdownOptions'
import { useSkillsAnnouncer } from '@/common-components/utilities/UseSkillsAnnouncer.js'
import { useDebounceFn, useWindowSize } from '@vueuse/core'

export const useMarkdownAccessibilityFixes = () => {

  const commonMarkdownOptions = useCommonMarkdownOptions()
  const announcer = useSkillsAnnouncer()
  const windowSize = useWindowSize()

  function doClickOnToolbarButton(selector) {
    const markdownEditor = commonMarkdownOptions.getMarkdownEditor()
    if (markdownEditor) {
      const btn = markdownEditor.querySelector(selector)
      btn.dispatchEvent(commonMarkdownOptions.getMouseEvent())
      btn.focus()
    }
  }

  function clickOnHeaderToolbarButton(id) {
    const btn = commonMarkdownOptions.getHeaderButton(id)
    if (btn) {
      btn.dispatchEvent(commonMarkdownOptions.getMouseEvent())
      btn.focus()
    }
  }

  function clickOnFontSizeToolbarButton() {
    doClickOnToolbarButton('[skilltree-id="fontSizeBtn"]')
  }

  function clickOnImageToolbarButton() {
    doClickOnToolbarButton('.image')
  }

  function clickOnLinkToolbarButton() {
    doClickOnToolbarButton('.link')
  }

  function clickOnAttachmentToolbarButton() {
    doClickOnToolbarButton('.attachment-button')
  }

  function fixAccessibilityIssues(id, allowInsertImages=true) {
    fixHeaderButtonIssues(id)
    fixMoreButtonAriaLabel(1)
    fixFontSizeButtonIssues(id)
    if (allowInsertImages) {
      fixInsertImageButtonIssues(id)
    }
    fixInsertUrlButtonIssues(id)
  }

  function fixInsertUrlButtonIssues(id) {
    nextTick(() => {
      const markdownEditor = commonMarkdownOptions.getMarkdownEditor(id)
      if (markdownEditor) {
        const imageButton = markdownEditor.querySelector('.link')
        imageButton.addEventListener('click', handleUrlButtonClick)
      }
    })
  }

  function handleUrlButtonClick() {
    nextTick(() => {
      nextTick(() => announcer.polite('Insert hyperlink into text'))
      const urlInput = commonMarkdownOptions.getMenuPopup().querySelector('#toastuiLinkUrlInput')
      urlInput.focus()
    })
  }

  function fixInsertImageButtonIssues(id) {
    nextTick(() => {
      const markdownEditor = commonMarkdownOptions.getMarkdownEditor(id)
      if (markdownEditor) {
        const imageButton = markdownEditor.querySelector('.image')

        const handleImageButtonClick = () => {
          nextTick(() => {
            nextTick(() => announcer.polite('Insert image by uploading a file or via an external URL. File Tab is currently active but please use left and right keys to switch between the tabs.'))
            const menuPopup = commonMarkdownOptions.getMenuPopup(id)
            const activeTab = menuPopup.querySelector('.tab-item.active')
            activeTab.focus()
            const hiddenFileUploadInput = menuPopup.querySelector('#toastuiImageFileInput')
            hiddenFileUploadInput.setAttribute('tabindex', -1)

            const tabs = menuPopup.querySelectorAll('.tab-item')
            tabs.forEach((tab) => {
              tab.addEventListener('keydown', (event) => {
                if (event.key === 'ArrowLeft' || event.key === 'ArrowRight') {
                  const otherTab = menuPopup.querySelector('.tab-item:not(.active)')
                  otherTab.dispatchEvent(commonMarkdownOptions.getMouseEvent())
                  otherTab.focus()
                }
              })
            })
            const okButton = menuPopup.querySelector('.toastui-editor-ok-button')
            okButton.setAttribute('tabindex', 0)
            const cancelButton = menuPopup.querySelector('.toastui-editor-close-button')
            cancelButton.setAttribute('tabindex', 0)
            const descriptonInput = menuPopup.querySelector('#toastuiAltTextInput')
            descriptonInput.setAttribute('tabindex', 0)
            const selectFileButton = menuPopup.querySelector('.toastui-editor-file-select-button')
            selectFileButton.setAttribute('tabindex', 0)
          })
        }

        imageButton.addEventListener('click', handleImageButtonClick)
      }
    })
  }


  function fixFontSizeButtonIssues(id) {
    return nextTick(() => {
      const markdownEditor = commonMarkdownOptions.getMarkdownEditor(id)
      if (markdownEditor) {
        const fontButton = markdownEditor.querySelector(`#${id} [aria-label="F"]`)
        fontButton.setAttribute('aria-label', 'Font Size')
        fontButton.setAttribute('skilltree-id', 'fontSizeBtn')
        fontButton.addEventListener('click', handleFontSizeButtonClick)
      }
    })
  }

  function handleFontSizeButtonClick() {
    nextTick(() => {
      const sizeInput = commonMarkdownOptions.getMenuPopup().querySelector('.size-input')
      sizeInput.setAttribute('aria-label', 'Set Font Size in pixels')
      sizeInput.classList.add('w-100')
      const menuitems = commonMarkdownOptions.getMenuPopup().querySelectorAll('.drop-down .drop-down-item')
      menuitems.forEach((item) => {
        item.setAttribute('aria-hidden', true)
      })
      sizeInput.focus()
    })
  }

  const debouncedFixMoreButtonAriaLabel = useDebounceFn(() => {
    fixMoreButtonAriaLabel(1)
  }, 150)

  watch(() => windowSize?.width?.value,
    (newWidth) => {
      if (newWidth <= 950) {
        debouncedFixMoreButtonAriaLabel()
      }
    }
  )

  function fixMoreButtonAriaLabel(attemptNum) {
    if (attemptNum <= 10) {
      setTimeout(() => {
        nextTick(() => {
          const markdownEditor = commonMarkdownOptions.getMarkdownEditor()
          if (markdownEditor) {
            const moreButton = markdownEditor.querySelector('.more')
            if (moreButton) {
              moreButton.setAttribute('aria-label', 'More Toolbar Controls')
            } else {
              fixMoreButtonAriaLabel(attemptNum + 1)
            }
          }
        })
      }, 300)
    }
  }

  function fixHeaderButtonIssues(id) {
    nextTick(() => {
      nextTick(() => {
        const headerButton = commonMarkdownOptions.getHeaderButton(id)
        if (headerButton) {
          headerButton.setAttribute('aria-haspopup', 'menu')
          headerButton.setAttribute('aria-expanded', false)
          headerButton.setAttribute('aria-controls', 'headerChoicesId')
          headerButton.setAttribute('id', 'headerButtonId')
          headerButton.addEventListener('click', handleHeaderButtonClick)
          headerButton.addEventListener('keydown', handleHeaderButtonKeydown)
        }
      })
    })
  }

  function handleHeaderButtonKeydown(event) {
    if (event.key === 'ArrowDown') {
      const menu = commonMarkdownOptions.getMenuItem()
      const firstItem = menu.querySelector('[data-level="1"]')
      firstItem.setAttribute('tabindex', 0)
      firstItem.focus()
    }
  }

  function handleHeaderButtonClick(btnClickEvent) {
    nextTick(() => announcer.polite('Select header type or paragraph text by using up and down keys.'))
    const menu = commonMarkdownOptions.getMenuItem()
    menu.setAttribute('id', 'headerChoicesId')
    menu.setAttribute('role', 'menu')
    menu.setAttribute('tabindex', -1)
    menu.setAttribute('aria-labelledby', 'headerButtonId')
    const menuitems = menu.querySelectorAll('[aria-role="menuitem"]')
    menuitems.forEach((item) => {
      item.setAttribute('tabindex', -1)
      item.setAttribute('role', 'menuitem')
      item.addEventListener('keydown', handleHeaderNavigationAndSelection)
    })
    btnClickEvent.srcElement.setAttribute('aria-expanded', true)
  }

  function handleHeaderNavigationAndSelection(event) {
    if (event.key === 'Enter') {
      event.preventDefault()
      event.srcElement.dispatchEvent(commonMarkdownOptions.getMouseEvent())
    } else if (event.key === 'ArrowDown') {
      handleHeaderUpAndDownNavigation(event.srcElement, 1)
    } else if (event.key === 'ArrowUp') {
      handleHeaderUpAndDownNavigation(event.srcElement, -1)
    }
  }

  function handleHeaderUpAndDownNavigation(currentItem, numToAdd) {
    const currentCount = currentItem.getAttribute('data-level')
    let nextCount = Number(currentCount) + numToAdd
    if (nextCount < 0) {
      nextCount = 6
    }
    let selector = `[data-level="${nextCount}"]`
    if (nextCount > 6 || nextCount < 1) {
      selector = '[data-type="Paragraph"]'
    }
    const nextItem = commonMarkdownOptions.getMenuItem().querySelector(selector)
    if (nextItem) {
      nextItem.setAttribute('tabindex', 0)
      nextItem.focus()
      currentItem.setAttribute('tabindex', -1)
    }
  }

  return {
    fixAccessibilityIssues,
    clickOnHeaderToolbarButton,
    clickOnFontSizeToolbarButton,
    clickOnImageToolbarButton,
    clickOnLinkToolbarButton,
    clickOnAttachmentToolbarButton
  }
}